package org.hackathon.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hackathon.data.dto.AssignJudgeDTO;
import org.hackathon.data.dto.AutoAssignDTO;
import org.hackathon.data.dto.RecuseDTO;
import org.hackathon.data.dto.TransferAssignmentDTO;
import org.hackathon.data.dto.UrgeJudgeDTO;
import org.hackathon.data.enums.AssignSource;
import org.hackathon.data.enums.NoticeType;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.enums.ReviewStatus;
import org.hackathon.data.po.Phase;
import org.hackathon.data.po.ReviewAssignment;
import org.hackathon.data.po.Submission;
import org.hackathon.data.po.Team;
import org.hackathon.data.po.User;
import org.hackathon.data.vo.AssignResultVO;
import org.hackathon.data.vo.JudgeProgressVO;
import org.hackathon.data.vo.PhaseProgressVO;
import org.hackathon.data.vo.RecuseResultVO;
import org.hackathon.data.vo.UrgeResultVO;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.AuthorityMapper;
import org.hackathon.mapper.RegistrationMapper;
import org.hackathon.mapper.ReviewAssignmentMapper;
import org.hackathon.mapper.SubmissionMapper;
import org.hackathon.mapper.TeamMapper;
import org.hackathon.mapper.UserMapper;
import org.hackathon.security.Context;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评委指派、利益回避与重新分发、逾期移交与打分进度监管。
 *
 * <p>重新分发的挑选规则：候选集为本赛事全部评委，逐条排除
 * ①在该作品上已有任何一条任务记录的评委（含已回避、已移交，防止把作品退回给回避者）、
 * ②属于该作品所属队伍的成员或队长的评委；
 * 剩余候选按「本轮在手任务数升序、用户id升序」取第一名，实现负载均衡且结果确定。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewAssignService {

    private final ReviewAssignmentMapper assignmentMapper;
    private final SubmissionMapper submissionMapper;
    private final AuthorityMapper authorityMapper;
    private final RegistrationMapper registrationMapper;
    private final TeamMapper teamMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    // ==================== 指派 ====================

    /**
     * 管理员把若干评委指派到若干作品，按笛卡尔积建任务
     * @param dto 评委id与作品id列表
     * @param ctx 已由 @Require(EVENT_ADMIN) 校验，轮次由路径确定
     * @return 新建与跳过的数量及跳过原因
     */
    @Transactional
    public AssignResultVO assign(AssignJudgeDTO dto, Context ctx) {
        Phase phase = ctx.phase();
        Integer eventId = ctx.event().getEventId();
        Set<Integer> judges = new HashSet<>(authorityMapper.selectEventJudgeIds(eventId));

        int created = 0;
        List<String> reasons = new ArrayList<>();
        for (Integer submissionId : dto.getSubmissionIds().stream().distinct().toList()) {
            Submission submission = mustFindSubmission(submissionId, phase);
            Set<Integer> conflicts = teamSideUsers(submission.getTeamId());
            for (Integer judgeId : dto.getJudgeIds().stream().distinct().toList()) {
                if (!judges.contains(judgeId)) {
                    throw new BusinessException(ResultCode.NOT_JUDGE,
                            "用户 " + judgeId + " 不是本赛事评委");
                }
                if (conflicts.contains(judgeId)) {
                    throw new BusinessException(ResultCode.JUDGE_CONFLICT_INTEREST,
                            "评委 " + judgeId + " 是作品 " + submissionId + " 所属队伍的成员");
                }
                if (insert(phase, submissionId, judgeId, AssignSource.MANUAL,
                        ctx.userId(), null) == null) {
                    reasons.add("评委 %d 已被指派作品 %d".formatted(judgeId, submissionId));
                    continue;
                }
                created++;
            }
        }
        return new AssignResultVO(created, reasons.size(), reasons);
    }

    /**
     * 为本轮每份作品自动补齐评委，逐份按负载最轻优先挑选
     * @param dto 每份作品期望的评委数
     * @return 新建与跳过的数量
     */
    @Transactional
    public AssignResultVO autoAssign(AutoAssignDTO dto, Context ctx) {
        Phase phase = ctx.phase();
        List<Submission> submissions = submissionMapper.selectByPhase(phase.getPhaseId());
        List<Integer> judges = authorityMapper.selectEventJudgeIds(ctx.event().getEventId());

        int created = 0;
        List<String> reasons = new ArrayList<>();
        for (Submission submission : submissions) {
            List<ReviewAssignment> existing = assignmentMapper.selectBySubmission(submission.getSubmissionId());
            long held = existing.stream().filter(a -> a.getStatus().isHeld()).count();
            for (long i = held; i < dto.getJudgeCount(); i++) {
                Integer judgeId = pickJudge(phase, submission, judges);
                if (judgeId == null) {
                    reasons.add("作品 %d 无可分发的无利益冲突评委".formatted(submission.getSubmissionId()));
                    break;
                }
                ReviewAssignment po = insert(phase, submission.getSubmissionId(), judgeId,
                        AssignSource.MANUAL, ctx.userId(), null);
                if (po == null) break;
                created++;
            }
        }
        return new AssignResultVO(created, reasons.size(), reasons);
    }

    // ==================== 回避与重新分发 ====================

    /**
     * 评委申请利益回避：原任务置为已回避并移出其待评列表，随后由系统重新分发
     * @param dto 回避理由
     * @param ctx 已由 @Require(ASSIGNED_JUDGE) 校验
     * @return 回避的任务与接手评委
     */
    @Transactional
    public RecuseResultVO recuse(RecuseDTO dto, Context ctx) {
        Submission submission = ctx.submission();
        Phase phase = ctx.phase();
        ReviewAssignment assignment = mustFindHeld(submission.getSubmissionId(), ctx.userId());

        LocalDateTime now = LocalDateTime.now();
        assignment.setStatus(ReviewStatus.RECUSED);
        assignment.setRecuseReason(dto.getReason().trim());
        assignment.setRecuseTime(now);
        assignment.setUpdateTime(now);
        if (assignmentMapper.updateById(assignment) == 0) {
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        }

        List<Integer> judges = authorityMapper.selectEventJudgeIds(ctx.event().getEventId());
        Integer newJudgeId = pickJudge(phase, submission, judges);
        if (newJudgeId == null) {
            //无人可接手时不静默丢弃，交由赛管人工处置
            notificationService.pushAll(authorityMapper.selectEventAdminIds(ctx.event().getEventId()),
                    NoticeType.REVIEW_TRANSFER, "作品重新分发失败",
                    "作品 %d 的评委已申请回避，但当前没有无利益冲突的评委可以接手，请人工指派。"
                            .formatted(submission.getSubmissionId()),
                    ctx.event().getEventId(), phase.getPhaseId(), submission.getSubmissionId(),
                    assignment.getAssignmentId());
            return new RecuseResultVO(assignment.getAssignmentId(), null, null, null,
                    "回避成功，但暂无可接手的评委，已通知赛事管理员人工指派");
        }

        ReviewAssignment created = insert(phase, submission.getSubmissionId(), newJudgeId,
                AssignSource.REDISTRIBUTE, null, assignment.getAssignmentId());
        if (created == null) {
            throw new BusinessException(ResultCode.NO_AVAILABLE_JUDGE);
        }
        notifyAssigned(created, phase, ctx.event().getEventId(), "评审任务重新分发",
                "原评委已回避，作品 %d 的评审任务已分发给你。".formatted(submission.getSubmissionId()));
        return new RecuseResultVO(assignment.getAssignmentId(), created.getAssignmentId(),
                newJudgeId, nameOf(newJudgeId), "回避成功，已重新分发");
    }

    // ==================== 逾期移交 ====================

    /**
     * 管理员把逾期评委的待评任务移交给其他评委
     * @param assignmentId 待移交的任务
     * @param dto 接手评委，不传时由系统按负载最轻原则挑选
     * @return 移交后的新任务与接手评委
     */
    @Transactional
    public RecuseResultVO transfer(Integer assignmentId, TransferAssignmentDTO dto, Context ctx) {
        Phase phase = ctx.phase();
        ReviewAssignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null || !assignment.getPhaseId().equals(phase.getPhaseId())) {
            throw new BusinessException(ResultCode.ASSIGNMENT_NOT_FOUND);
        }
        if (!assignment.getStatus().isHeld()) {
            throw new BusinessException(ResultCode.ASSIGNMENT_CLOSED);
        }
        Submission submission = submissionMapper.selectById(assignment.getSubmissionId());
        if (submission == null) {
            throw new BusinessException(ResultCode.SUBMISSION_NOT_FOUND);
        }

        List<Integer> judges = authorityMapper.selectEventJudgeIds(ctx.event().getEventId());
        final Integer toJudgeId = dto.getToJudgeId() != null ? dto.getToJudgeId()
                : pickJudge(phase, submission, judges);
        if (toJudgeId == null) {
            throw new BusinessException(ResultCode.NO_AVAILABLE_JUDGE);
        }
        if (dto.getToJudgeId() != null) {
            if (!judges.contains(toJudgeId)) {
                throw new BusinessException(ResultCode.NOT_JUDGE, "接手人不是本赛事评委");
            }
            if (teamSideUsers(submission.getTeamId()).contains(toJudgeId)) {
                throw new BusinessException(ResultCode.JUDGE_CONFLICT_INTEREST);
            }
            if (assignmentMapper.selectBySubmission(submission.getSubmissionId()).stream()
                    .anyMatch(a -> a.getJudgeId().equals(toJudgeId))) {
                throw new BusinessException(ResultCode.ASSIGNMENT_REPEAT);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        assignment.setStatus(ReviewStatus.TRANSFERRED);
        assignment.setUpdateTime(now);
        if (assignmentMapper.updateById(assignment) == 0) {
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        }
        ReviewAssignment created = insert(phase, submission.getSubmissionId(), toJudgeId,
                AssignSource.TRANSFER, ctx.userId(), assignment.getAssignmentId());
        if (created == null) {
            throw new BusinessException(ResultCode.ASSIGNMENT_REPEAT);
        }

        String reason = StringUtils.hasText(dto.getReason()) ? dto.getReason().trim() : "打分逾期";
        notifyAssigned(created, phase, ctx.event().getEventId(), "评审任务移交",
                "管理员将作品 %d 的评审任务移交给你，原因：%s。".formatted(submission.getSubmissionId(), reason));
        notificationService.push(assignment.getJudgeId(), NoticeType.REVIEW_TRANSFER, "评审任务已移交",
                "你名下作品 %d 的评审任务已被管理员移交，原因：%s。"
                        .formatted(submission.getSubmissionId(), reason),
                ctx.event().getEventId(), phase.getPhaseId(), submission.getSubmissionId(),
                assignment.getAssignmentId());
        return new RecuseResultVO(assignment.getAssignmentId(), created.getAssignmentId(),
                toJudgeId, nameOf(toJudgeId), "移交成功");
    }

    // ==================== 进度监管与催办 ====================

    /**
     * 本轮各评委的打分进度，形如「评委A 已完成 8/10」
     */
    public PhaseProgressVO getProgress(Context ctx) {
        Phase phase = ctx.phase();
        List<ReviewAssignment> all = assignmentMapper.selectByPhase(phase.getPhaseId());
        Map<Integer, List<ReviewAssignment>> byJudge = all.stream()
                .collect(Collectors.groupingBy(ReviewAssignment::getJudgeId));
        //赛事下的评委即使一份任务都没有也要出现在看板里
        List<Integer> judgeIds = new ArrayList<>(authorityMapper.selectEventJudgeIds(ctx.event().getEventId()));
        byJudge.keySet().stream().filter(id -> !judgeIds.contains(id)).forEach(judgeIds::add);
        Map<Integer, String> names = nameOf(judgeIds);

        LocalDateTime now = LocalDateTime.now();
        boolean closed = phase.getReviewEnd() != null && now.isAfter(phase.getReviewEnd());
        List<JudgeProgressVO> judges = judgeIds.stream().map(judgeId -> {
            List<ReviewAssignment> list = byJudge.getOrDefault(judgeId, List.of());
            List<ReviewAssignment> pending = list.stream()
                    .filter(a -> a.getStatus() == ReviewStatus.PENDING).toList();
            int done = (int) list.stream().filter(a -> a.getStatus() == ReviewStatus.DONE).count();
            int held = done + pending.size();
            JudgeProgressVO vo = new JudgeProgressVO();
            vo.setJudgeId(judgeId);
            vo.setJudgeName(names.get(judgeId));
            vo.setTotal(held);
            vo.setDone(done);
            vo.setPending(pending.size());
            vo.setRecused((int) list.stream().filter(a -> a.getStatus() == ReviewStatus.RECUSED).count());
            vo.setTransferred((int) list.stream()
                    .filter(a -> a.getStatus() == ReviewStatus.TRANSFERRED).count());
            vo.setProgress("%d/%d".formatted(done, held));
            vo.setOverdue(closed && !pending.isEmpty());
            vo.setUrgeCount(list.stream().mapToInt(a ->
                    a.getUrgeCount() == null ? 0 : a.getUrgeCount()).max().orElse(0));
            vo.setLastUrgeTime(list.stream().map(ReviewAssignment::getLastUrgeTime)
                    .filter(java.util.Objects::nonNull).max(Comparator.naturalOrder()).orElse(null));
            vo.setPendingAssignmentIds(pending.stream().map(ReviewAssignment::getAssignmentId).toList());
            return vo;
        }).toList();

        PhaseProgressVO vo = new PhaseProgressVO();
        vo.setPhaseId(phase.getPhaseId());
        vo.setPhaseName(phase.getName());
        vo.setReviewBeg(phase.getReviewBeg());
        vo.setReviewEnd(phase.getReviewEnd());
        vo.setReviewClosed(closed);
        vo.setJudgeCount(judges.size());
        vo.setTotalTask(judges.stream().mapToInt(JudgeProgressVO::getTotal).sum());
        vo.setDoneTask(judges.stream().mapToInt(JudgeProgressVO::getDone).sum());
        vo.setPendingTask(judges.stream().mapToInt(JudgeProgressVO::getPending).sum());
        vo.setJudges(judges);
        return vo;
    }

    /**
     * 一键催办，评委列表为空时催办本轮全部仍有待评任务的评委
     * @return 触达的评委数与其待评任务数
     */
    @Transactional
    public UrgeResultVO urge(UrgeJudgeDTO dto, Context ctx) {
        Phase phase = ctx.phase();
        List<ReviewAssignment> pending = assignmentMapper.selectByPhase(phase.getPhaseId()).stream()
                .filter(a -> a.getStatus() == ReviewStatus.PENDING).toList();
        if (dto.getJudgeIds() != null && !dto.getJudgeIds().isEmpty()) {
            Set<Integer> targets = new HashSet<>(dto.getJudgeIds());
            pending = pending.stream().filter(a -> targets.contains(a.getJudgeId())).toList();
        }
        if (pending.isEmpty()) return new UrgeResultVO(0, 0);

        LocalDateTime now = LocalDateTime.now();
        assignmentMapper.update(null, new LambdaUpdateWrapper<ReviewAssignment>()
                .in(ReviewAssignment::getAssignmentId,
                        pending.stream().map(ReviewAssignment::getAssignmentId).toList())
                .setSql("urge_count = urge_count + 1")
                .setSql("version = version + 1")
                .set(ReviewAssignment::getLastUrgeTime, now)
                .set(ReviewAssignment::getUpdateTime, now));

        Map<Integer, Long> byJudge = pending.stream().collect(
                Collectors.groupingBy(ReviewAssignment::getJudgeId, Collectors.counting()));
        String tail = StringUtils.hasText(dto.getMessage()) ? "附言：" + dto.getMessage().trim() : "";
        String deadline = phase.getReviewEnd() == null ? "" :
                "，评审截止时间 " + phase.getReviewEnd().format(NotificationService.TIME);
        byJudge.forEach((judgeId, count) -> notificationService.push(judgeId, NoticeType.REVIEW_URGE,
                "打分催办", "轮次「%s」你还有 %d 份作品未完成打分%s。%s"
                        .formatted(phase.getName(), count, deadline, tail),
                ctx.event().getEventId(), phase.getPhaseId(), null, null));
        return new UrgeResultVO(byJudge.size(), pending.size());
    }

    // ==================== 内部工具 ====================

    /**
     * 建任务，(作品, 评委) 已存在时返回 null 交由调用方决定跳过还是报错
     */
    private ReviewAssignment insert(Phase phase, Integer submissionId, Integer judgeId,
                                    AssignSource source, Integer assignerId, Integer sourceAssignmentId) {
        LocalDateTime now = LocalDateTime.now();
        ReviewAssignment po = new ReviewAssignment(null, phase.getPhaseId(), submissionId, judgeId,
                ReviewStatus.PENDING, source, assignerId, sourceAssignmentId, null, null, null,
                null, null, 0, null, 1, now, now);
        try {
            assignmentMapper.insert(po);
        } catch (DuplicateKeyException e) {
            return null;
        }
        return po;
    }

    private void notifyAssigned(ReviewAssignment assignment, Phase phase, Integer eventId,
                                String title, String content) {
        notificationService.push(assignment.getJudgeId(), NoticeType.REVIEW_ASSIGN, title,
                content, eventId, phase.getPhaseId(), assignment.getSubmissionId(),
                assignment.getAssignmentId());
    }

    /**
     * 按「本轮在手任务数升序、用户id升序」挑选无利益冲突且尚未接触过该作品的评委
     * @return 无可用评委时返回 null
     */
    private Integer pickJudge(Phase phase, Submission submission, List<Integer> judges) {
        Set<Integer> touched = assignmentMapper.selectBySubmission(submission.getSubmissionId())
                .stream().map(ReviewAssignment::getJudgeId).collect(Collectors.toSet());
        Set<Integer> conflicts = teamSideUsers(submission.getTeamId());
        return judges.stream()
                .filter(id -> !touched.contains(id) && !conflicts.contains(id))
                .min(Comparator.comparingLong(
                                (Integer id) -> assignmentMapper.countHeldByJudge(phase.getPhaseId(), id))
                        .thenComparingInt(id -> id))
                .orElse(null);
    }

    /**
     * 与作品存在天然利益关系的用户：队长与全部队员
     */
    private Set<Integer> teamSideUsers(Integer teamId) {
        Set<Integer> users = new HashSet<>(registrationMapper.selectMemberIds(teamId));
        Team team = teamMapper.selectById(teamId);
        if (team != null) users.add(team.getLeaderId());
        return users;
    }

    private Submission mustFindSubmission(Integer submissionId, Phase phase) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null || !submission.getPhaseId().equals(phase.getPhaseId())) {
            throw new BusinessException(ResultCode.SUBMISSION_NOT_FOUND);
        }
        return submission;
    }

    ReviewAssignment mustFindHeld(Integer submissionId, Integer judgeId) {
        ReviewAssignment assignment = assignmentMapper.selectHeld(submissionId, judgeId);
        if (assignment == null) {
            throw new BusinessException(ResultCode.NOT_ASSIGNED_JUDGE);
        }
        return assignment;
    }

    private String nameOf(Integer userId) {
        User user = userMapper.selectById(userId);
        return user == null ? null : user.getName();
    }

    Map<Integer, String> nameOf(List<Integer> userIds) {
        if (userIds.isEmpty()) return new HashMap<>();
        return userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, User::getName));
    }
}
