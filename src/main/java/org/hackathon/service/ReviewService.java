package org.hackathon.service;

import lombok.RequiredArgsConstructor;
import org.hackathon.data.dto.ScoreItemDTO;
import org.hackathon.data.dto.ScoreWorkDTO;
import org.hackathon.data.enums.FileScope;
import org.hackathon.data.enums.FileStatus;
import org.hackathon.data.enums.FlagStatus;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.enums.ReviewStatus;
import org.hackathon.data.po.FileObject;
import org.hackathon.data.po.Phase;
import org.hackathon.data.po.ReviewAssignment;
import org.hackathon.data.po.ReviewDimension;
import org.hackathon.data.po.ReviewFlag;
import org.hackathon.data.po.ReviewScore;
import org.hackathon.data.po.Student;
import org.hackathon.data.po.Submission;
import org.hackathon.data.po.Team;
import org.hackathon.data.po.User;
import org.hackathon.data.vo.DimensionVO;
import org.hackathon.data.vo.JudgeScoreVO;
import org.hackathon.data.vo.PreviewItemVO;
import org.hackathon.data.vo.ReviewFlagVO;
import org.hackathon.data.vo.ReviewPreviewVO;
import org.hackathon.data.vo.ReviewScoreVO;
import org.hackathon.data.vo.ReviewTaskVO;
import org.hackathon.data.vo.ReviewWorkVO;
import org.hackathon.data.vo.SubmissionResultVO;
import org.hackathon.data.vo.TeamMemberVO;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.FileObjectMapper;
import org.hackathon.mapper.RegistrationMapper;
import org.hackathon.mapper.ReviewAssignmentMapper;
import org.hackathon.mapper.ReviewFlagMapper;
import org.hackathon.mapper.ReviewScoreMapper;
import org.hackathon.mapper.StudentMapper;
import org.hackathon.mapper.SubmissionMapper;
import org.hackathon.mapper.TeamMapper;
import org.hackathon.mapper.UserMapper;
import org.hackathon.security.Context;
import org.hackathon.security.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评委工作台：待评列表、评审视图、在线预览、逐维度打分，以及去极值平均计分。
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewAssignmentMapper assignmentMapper;
    private final ReviewScoreMapper scoreMapper;
    private final ReviewFlagMapper flagMapper;
    private final SubmissionMapper submissionMapper;
    private final TeamMapper teamMapper;
    private final UserMapper userMapper;
    private final StudentMapper studentMapper;
    private final RegistrationMapper registrationMapper;
    private final FileObjectMapper fileObjectMapper;
    private final ReviewDimensionService dimensionService;
    private final ReviewAssignService assignService;
    private final ReviewFlagService flagService;
    private final SubmissionService submissionService;
    private final FileService fileService;

    // ==================== 待评列表 ====================

    /**
     * 评委在本轮的待评列表
     * @param ctx 已由 @Require(EVENT_JUDGE) 校验，评委即为当前登录人
     */
    public List<ReviewTaskVO> listTasks(Context ctx) {
        Phase phase = ctx.phase();
        boolean blind = ctx.view(phase) == Context.View.BLIND;
        List<ReviewAssignment> assignments =
                assignmentMapper.selectByPhaseJudge(phase.getPhaseId(), ctx.userId());
        if (assignments.isEmpty()) return List.of();

        List<Integer> submissionIds = assignments.stream()
                .map(ReviewAssignment::getSubmissionId).distinct().toList();
        Map<Integer, Submission> submissions = submissionMapper.selectByIds(submissionIds).stream()
                .collect(Collectors.toMap(Submission::getSubmissionId, s -> s));
        Map<Integer, Team> teams = teamMapper.selectByIds(submissions.values().stream()
                        .map(Submission::getTeamId).distinct().toList()).stream()
                .collect(Collectors.toMap(Team::getTeamId, t -> t));
        Map<Integer, Long> openFlags = flagMapper.selectBySubmissions(submissionIds).stream()
                .filter(f -> f.getStatus() == FlagStatus.OPEN)
                .collect(Collectors.groupingBy(ReviewFlag::getSubmissionId, Collectors.counting()));

        return assignments.stream().map(po -> {
            Submission submission = submissions.get(po.getSubmissionId());
            Team team = submission == null ? null : teams.get(submission.getTeamId());
            ReviewTaskVO vo = new ReviewTaskVO();
            vo.setAssignmentId(po.getAssignmentId());
            vo.setSubmissionId(po.getSubmissionId());
            vo.setPhaseId(po.getPhaseId());
            vo.setStatus(po.getStatus());
            vo.setSource(po.getSource());
            vo.setTotalScore(po.getTotalScore());
            vo.setScoreTime(po.getSubmitTime());
            vo.setUrgeCount(po.getUrgeCount());
            vo.setLastUrgeTime(po.getLastUrgeTime());
            if (submission != null) {
                vo.setTeamId(submission.getTeamId());
                vo.setTeamName(teamLabel(team, submission.getTeamId(), blind));
                vo.setSubmissionStatus(submission.derivedStatus(phase));
                vo.setVersionNo(submission.getVersionNo());
                vo.setSubmitTime(submission.getSubmitTime());
                vo.setOpenFlagCount(openFlags.getOrDefault(po.getSubmissionId(), 0L).intValue());
            }
            return vo;
        }).toList();
    }

    // ==================== 评审视图 ====================

    /**
     * 评委查看作品的评审视图：作品内容 + 评分维度 + 本评委已有打分 + 异常标记
     * @param ctx 已由 @Require({ASSIGNED_JUDGE, EVENT_ADMIN}) 校验
     */
    public ReviewWorkVO getWork(Context ctx) {
        Submission submission = ctx.submission();
        Phase phase = ctx.phase();
        boolean blind = ctx.view(phase) == Context.View.BLIND;

        ReviewWorkVO vo = new ReviewWorkVO();
        vo.setBlind(blind);
        vo.setSubmission(submissionService.getSubmissionById(ctx));
        vo.setMembers(blind ? null : members(ctx.team()));

        List<ReviewDimension> dimensions = dimensionService.listDimensions(phase.getPhaseId());
        vo.setDimensions(dimensions.stream().map(DimensionVO::of).toList());

        ReviewAssignment assignment = assignmentMapper.selectHeld(submission.getSubmissionId(), ctx.userId());
        if (assignment != null) {
            vo.setAssignmentId(assignment.getAssignmentId());
            vo.setStatus(assignment.getStatus());
            vo.setTotalScore(assignment.getTotalScore());
            vo.setComment(assignment.getComment());
            vo.setScoreTime(assignment.getSubmitTime());
            vo.setVersion(assignment.getVersion());
            Map<Integer, ReviewScore> scores = scoreMapper.selectByAssignment(assignment.getAssignmentId())
                    .stream().collect(Collectors.toMap(ReviewScore::getDimensionId, s -> s));
            vo.setScores(dimensions.stream().map(d -> {
                ReviewScore score = scores.get(d.getDimensionId());
                return new ReviewScoreVO(d.getDimensionId(), d.getName(), d.getMaxScore(), d.getWeight(),
                        score == null ? null : score.getScore(), score == null ? null : score.getComment());
            }).toList());
        } else {
            vo.setScores(List.of());
        }

        LocalDateTime now = LocalDateTime.now();
        vo.setScorable(phase.getReviewBeg() != null && phase.getReviewEnd() != null
                && !now.isBefore(phase.getReviewBeg()) && !now.isAfter(phase.getReviewEnd()));
        vo.setFlags(flagService.listFlags(submission.getSubmissionId(), blind));
        return vo;
    }

    // ==================== 在线预览 ====================

    /**
     * 在线预览入口：文档与录屏返回带时效的预签名直读地址，不经服务端转发；
     * 在线 Demo 网址原样下发供评委端 iframe 嵌入
     * @param ctx 已由 @Require({ASSIGNED_JUDGE, EVENT_ADMIN, SUBMISSION_OWNER}) 校验
     */
    public ReviewPreviewVO preview(Context ctx) {
        Submission submission = ctx.submission();
        boolean blind = ctx.view(ctx.phase()) == Context.View.BLIND;

        ReviewPreviewVO vo = new ReviewPreviewVO();
        vo.setSubmissionId(submission.getSubmissionId());
        vo.setDoc(previewItem(submission.getDocFileId(), blind, ctx));
        vo.setVideo(previewItem(submission.getVideoFileId(), blind, ctx));
        vo.setVideoUrl(submission.getVideoUrl());
        vo.setArchive(previewItem(submission.getArchiveFileId(), blind, ctx));
        vo.setDemoUrl(submission.getDemoUrl());
        vo.setRepoUrl(submission.getRepoUrl());
        return vo;
    }

    /**
     * 预签名直读地址，有效期取 FileScope 上配置的时长（录屏 120 分钟、文档 60 分钟）
     */
    private PreviewItemVO previewItem(Long fileId, boolean blind, Context ctx) {
        if (fileId == null) return null;
        FileObject file = fileObjectMapper.selectById(fileId);
        if (file == null || file.getStatus() != FileStatus.READY) return null;
        //download=false，签名内联展示，浏览器内置阅读器/播放器直接加载
        var url = fileService.url(fileId, false, ctx);
        return new PreviewItemVO(fileId, blind ? blindName(file) : file.getOriginName(),
                file.getContentType(), file.getSizeBytes(), url.url(), url.expiresAt());
    }

    /**
     * 盲审下文件名也可能泄露身份，统一替换为与队伍编号一致的匿名名
     */
    private String blindName(FileObject file) {
        return "Team-%03d-%s.%s".formatted(file.getTeamId(),
                file.getScope().getDirectory().replace('/', '-'), file.extension());
    }

    // ==================== 打分 ====================

    /**
     * 评委按本轮维度逐项打分并填写评语，评审时间窗内可重复提交覆盖
     * @param dto 各维度得分与总评语
     * @param ctx 已由 @Require(value = ASSIGNED_JUDGE, window = REVIEW) 校验
     * @return 本次的加权总分
     */
    @Transactional
    public BigDecimal score(ScoreWorkDTO dto, Context ctx) {
        Submission submission = ctx.submission();
        ReviewAssignment assignment = assignService.mustFindHeld(submission.getSubmissionId(), ctx.userId());
        List<ReviewDimension> dimensions = dimensionService.requireDimensions(ctx.phase().getPhaseId());

        Map<Integer, BigDecimal> scores = new HashMap<>();
        Map<Integer, String> comments = new HashMap<>();
        for (ScoreItemDTO item : dto.getScores()) {
            if (scores.put(item.getDimensionId(), item.getScore()) != null) {
                throw new BusinessException(ResultCode.DIMENSION_MISMATCH,
                        "维度 " + item.getDimensionId() + " 重复打分");
            }
            comments.put(item.getDimensionId(), item.getComment());
        }
        for (ReviewDimension dimension : dimensions) {
            BigDecimal score = scores.get(dimension.getDimensionId());
            if (score == null) {
                throw new BusinessException(ResultCode.DIMENSION_MISMATCH,
                        "缺少维度「" + dimension.getName() + "」的评分");
            }
            if (score.compareTo(dimension.getMaxScore()) > 0) {
                throw new BusinessException(ResultCode.SCORE_OUT_OF_RANGE,
                        "维度「%s」满分为 %s".formatted(dimension.getName(), dimension.getMaxScore()));
            }
        }
        if (scores.size() != dimensions.size()) {
            throw new BusinessException(ResultCode.DIMENSION_MISMATCH, "存在不属于本轮的评分维度");
        }

        BigDecimal total = dimensionService.weightedTotal(dimensions, scores);
        LocalDateTime now = LocalDateTime.now();
        //重复打分按覆盖处理，先清空旧维度分再落新分
        scoreMapper.deleteByAssignment(assignment.getAssignmentId());
        for (ReviewDimension dimension : dimensions) {
            scoreMapper.insert(new ReviewScore(null, assignment.getAssignmentId(),
                    dimension.getDimensionId(), scores.get(dimension.getDimensionId()),
                    comments.get(dimension.getDimensionId()), now, now));
        }

        if (assignment.getStatus() == ReviewStatus.DONE) {
            if (dto.getVersion() == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "版本号不能为空");
            }
            assignment.setVersion(dto.getVersion());
        }
        assignment.setStatus(ReviewStatus.DONE);
        assignment.setTotalScore(total);
        assignment.setComment(dto.getComment());
        assignment.setSubmitTime(now);
        assignment.setUpdateTime(now);
        if (assignmentMapper.updateById(assignment) == 0) {
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        }
        return total;
    }

    // ==================== 去极值平均计分 ====================

    /**
     * 单份作品的最终得分与计分明细
     */
    public SubmissionResultVO getResult(Context ctx) {
        Submission submission = ctx.submission();
        List<ReviewAssignment> assignments =
                assignmentMapper.selectBySubmission(submission.getSubmissionId());
        SubmissionResultVO result = buildResult(submission, ctx.team(), assignments, judgeNames(assignments));
        //作品所有者可查询汇总成绩，但评委身份和逐评委打分只供赛事管理员监管。
        if (!ctx.is(Role.EVENT_ADMIN)) {
            result.setJudgeScores(List.of());
        }
        return result;
    }

    /**
     * 本轮全部作品的最终得分，按得分降序
     */
    public List<SubmissionResultVO> listResults(Context ctx) {
        Phase phase = ctx.phase();
        List<Submission> submissions = submissionMapper.selectByPhase(phase.getPhaseId());
        if (submissions.isEmpty()) return List.of();
        List<ReviewAssignment> all = assignmentMapper.selectBySubmissions(
                submissions.stream().map(Submission::getSubmissionId).toList());
        Map<Integer, List<ReviewAssignment>> bySubmission = all.stream()
                .collect(Collectors.groupingBy(ReviewAssignment::getSubmissionId));
        Map<Integer, Team> teams = teamMapper.selectByIds(submissions.stream()
                        .map(Submission::getTeamId).distinct().toList()).stream()
                .collect(Collectors.toMap(Team::getTeamId, t -> t));
        Map<Integer, String> names = judgeNames(all);
        return submissions.stream()
                .map(s -> buildResult(s, teams.get(s.getTeamId()),
                        bySubmission.getOrDefault(s.getSubmissionId(), List.of()), names))
                .sorted(Comparator.comparing(SubmissionResultVO::getFinalScore,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /**
     * 去极值平均：已完成打分的评委数 ≥5 时剔除一个最高分与一个最低分再取平均，
     * 不足 5 人时直接取全部评委的平均值；一个评委都没打分则最终得分为空。
     */
    private SubmissionResultVO buildResult(Submission submission, Team team,
                                           List<ReviewAssignment> assignments,
                                           Map<Integer, String> names) {
        List<ReviewAssignment> done = assignments.stream()
                .filter(a -> a.getStatus() == ReviewStatus.DONE && a.getTotalScore() != null)
                //分数升序，同分按任务id升序，保证剔除结果确定
                .sorted(Comparator.comparing(ReviewAssignment::getTotalScore)
                        .thenComparing(ReviewAssignment::getAssignmentId))
                .toList();

        int count = done.size();
        boolean trimmed = count >= 5;
        List<JudgeScoreVO> details = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        int counted = 0;
        for (int i = 0; i < count; i++) {
            ReviewAssignment po = done.get(i);
            boolean excluded = trimmed && (i == 0 || i == count - 1);
            JudgeScoreVO detail = new JudgeScoreVO();
            detail.setAssignmentId(po.getAssignmentId());
            detail.setJudgeId(po.getJudgeId());
            detail.setJudgeName(names.get(po.getJudgeId()));
            detail.setTotalScore(po.getTotalScore());
            detail.setSubmitTime(po.getSubmitTime());
            detail.setCounted(!excluded);
            detail.setExcludeReason(excluded ? (i == 0 ? "去极值：最低分" : "去极值：最高分") : null);
            details.add(detail);
            if (!excluded) {
                sum = sum.add(po.getTotalScore());
                counted++;
            }
        }

        SubmissionResultVO vo = new SubmissionResultVO();
        vo.setSubmissionId(submission.getSubmissionId());
        vo.setTeamId(submission.getTeamId());
        vo.setTeamName(team == null ? null : team.getName());
        vo.setAssignedCount((int) assignments.stream().filter(a -> a.getStatus().isHeld()).count());
        vo.setScoredCount(count);
        vo.setTrimmed(trimmed);
        vo.setRule(trimmed ? "评委数 %d ≥ 5，剔除一个最高分与一个最低分后取平均".formatted(count)
                : "评委数 %d < 5，取全部评委平均值".formatted(count));
        vo.setFinalScore(counted == 0 ? null
                : sum.divide(BigDecimal.valueOf(counted), 2, RoundingMode.HALF_UP));
        //明细按分数降序展示，便于人工核对
        details.sort(Comparator.comparing(JudgeScoreVO::getTotalScore).reversed());
        vo.setJudgeScores(details);
        return vo;
    }

    // ==================== 内部工具 ====================

    private String teamLabel(Team team, Integer teamId, boolean blind) {
        if (blind) return "Team-%03d".formatted(teamId);
        return team == null ? null : team.getName();
    }

    /**
     * 队伍成员，仅非盲审视角下发，供评委判断是否需要申请回避
     */
    private List<TeamMemberVO> members(Team team) {
        List<Integer> ids = registrationMapper.selectMemberIds(team.getTeamId());
        if (ids.isEmpty()) return List.of();
        Map<Integer, String> names = userMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(User::getUserId, User::getName));
        return studentMapper.selectByIds(ids).stream().map(po -> new TeamMemberVO(
                po.getUserId(), names.get(po.getUserId()), po.getCasId(), po.getCampus(),
                po.getMajor(), po.getTagsAsList(), team.getLeaderId().equals(po.getUserId())
        )).toList();
    }

    private Map<Integer, String> judgeNames(List<ReviewAssignment> assignments) {
        return assignService.nameOf(assignments.stream()
                .map(ReviewAssignment::getJudgeId).distinct().toList());
    }
}
