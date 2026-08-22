package org.hackathon.service;

import lombok.RequiredArgsConstructor;
import org.hackathon.data.dto.CloseFlagDTO;
import org.hackathon.data.dto.FlagWorkDTO;
import org.hackathon.data.enums.FlagStatus;
import org.hackathon.data.enums.FlagTarget;
import org.hackathon.data.enums.NoticeType;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.po.Phase;
import org.hackathon.data.po.ReviewFlag;
import org.hackathon.data.po.Submission;
import org.hackathon.data.po.Team;
import org.hackathon.data.po.User;
import org.hackathon.data.vo.FlagIdVO;
import org.hackathon.data.vo.ReviewFlagVO;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.ReviewFlagMapper;
import org.hackathon.mapper.UserMapper;
import org.hackathon.security.Context;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Demo 异常标记与补交窗口。
 *
 * <p>补交是一条独立的豁免通道：评委标记异常时才开窗，窗口写在 review_flag 上并带有截止时间、
 * 开窗人与补交后的版本号，队长只能通过 {@code Require.Window.SUPPLEMENT} 校验的专用接口补交。
 * 既有的提交时间窗 SUBMIT 与作品锁定状态不受任何影响。
 */
@Service
@RequiredArgsConstructor
public class ReviewFlagService {

    //未指定时长时的默认补交窗口
    private static final int DEFAULT_WINDOW_HOURS = 24;

    private final ReviewFlagMapper flagMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    /**
     * 评委对损坏的 Demo 链接或异常文件打异常标记，同时给队长开出补交窗口
     * @param dto 异常对象、异常信息与期望的窗口时长
     * @param ctx 已由 @Require(value = ASSIGNED_JUDGE, window = REVIEW) 校验
     * @return 标记id与窗口截止时间
     */
    @Transactional
    public FlagIdVO flag(FlagWorkDTO dto, Context ctx) {
        Submission submission = ctx.submission();
        Phase phase = ctx.phase();
        Team team = ctx.team();
        FlagTarget target = dto.getTarget();
        if (!target.enabledIn(phase.getSubmissionConfig())) {
            throw new BusinessException(ResultCode.FLAG_ITEM_DISABLED);
        }
        if (flagMapper.selectOpenByTarget(submission.getSubmissionId(), target) != null) {
            throw new BusinessException(ResultCode.FLAG_REPEAT);
        }

        LocalDateTime now = LocalDateTime.now();
        int hours = dto.getWindowHours() == null ? DEFAULT_WINDOW_HOURS : dto.getWindowHours();
        //窗口不得越过本轮评审截止时间，避免补交产生无人评审的新版本
        LocalDateTime end = now.plusHours(hours);
        if (phase.getReviewEnd() != null && end.isAfter(phase.getReviewEnd())) {
            end = phase.getReviewEnd();
        }

        ReviewFlag flag = new ReviewFlag(null, submission.getSubmissionId(), phase.getPhaseId(),
                ctx.userId(), target, dto.getDescription().trim(), FlagStatus.OPEN,
                end, ctx.userId(), now, null, null, null, 1, now, now);
        flagMapper.insert(flag);

        notificationService.push(team.getLeaderId(), NoticeType.DEMO_FLAG,
                "作品异常，请尽快补交",
                "评委反馈你的作品「%s」存在异常：%s。请在 %s 前通过补交通道重新提交，逾期窗口自动关闭。"
                        .formatted(target.getDesc(), flag.getDescription(),
                                end.format(NotificationService.TIME)),
                team.getEventId(), phase.getPhaseId(), submission.getSubmissionId(), flag.getFlagId());
        return new FlagIdVO(flag.getFlagId(), end);
    }

    /**
     * 作品上的全部异常标记
     * @param blind 盲审视角下不下发标记人姓名
     */
    public List<ReviewFlagVO> listFlags(Integer submissionId, boolean blind) {
        List<ReviewFlag> flags = flagMapper.selectBySubmission(submissionId);
        if (flags.isEmpty()) return List.of();
        Map<Integer, String> names = blind ? Map.of() : userMapper.selectByIds(
                        flags.stream().map(ReviewFlag::getJudgeId).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getUserId, User::getName));
        LocalDateTime now = LocalDateTime.now();
        return flags.stream().map(po -> toVO(po, names.get(po.getJudgeId()), now)).toList();
    }

    /**
     * 管理员提前关闭补交窗口
     */
    @Transactional
    public void close(Integer flagId, CloseFlagDTO dto, Context ctx) {
        ReviewFlag flag = flagMapper.selectById(flagId);
        if (flag == null || !flag.getSubmissionId().equals(ctx.submission().getSubmissionId())) {
            throw new BusinessException(ResultCode.FLAG_NOT_FOUND);
        }
        if (flag.getStatus() != FlagStatus.OPEN) {
            throw new BusinessException(ResultCode.FLAG_CLOSED);
        }
        LocalDateTime now = LocalDateTime.now();
        flag.setStatus(FlagStatus.CLOSED);
        flag.setCloseReason(dto.getReason().trim());
        flag.setUpdateTime(now);
        if (flagMapper.updateById(flag) == 0) {
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        }
        notificationService.push(ctx.team().getLeaderId(), NoticeType.SUPPLEMENT_CLOSED,
                "补交窗口已关闭",
                "作品「%s」的补交窗口已由管理员关闭，原因：%s。"
                        .formatted(flag.getTarget().getDesc(), flag.getCloseReason()),
                ctx.team().getEventId(), flag.getPhaseId(), flag.getSubmissionId(), flag.getFlagId());
    }

    /**
     * 队长完成补交后收敛全部有效窗口，并把补交版本号回写为审计痕迹
     * @param versionNo 补交产生的新版本号
     * @return 本次关闭的窗口数
     */
    @Transactional
    public int resolve(Integer submissionId, Integer versionNo, Integer eventId) {
        LocalDateTime now = LocalDateTime.now();
        List<ReviewFlag> windows = flagMapper.selectOpenWindows(submissionId, now);
        for (ReviewFlag flag : windows) {
            flag.setStatus(FlagStatus.RESOLVED);
            flag.setResolvedVersionNo(versionNo);
            flag.setResolveTime(now);
            flag.setUpdateTime(now);
            flagMapper.updateById(flag);
            notificationService.push(flag.getJudgeId(), NoticeType.DEMO_FLAG, "队伍已完成补交",
                    "你标记异常的作品 %d 已由队长补交，新版本为 v%d，请重新查看并打分。"
                            .formatted(submissionId, versionNo),
                    eventId, flag.getPhaseId(), submissionId, flag.getFlagId());
        }
        return windows.size();
    }

    ReviewFlagVO toVO(ReviewFlag po, String judgeName, LocalDateTime now) {
        ReviewFlagVO vo = new ReviewFlagVO();
        vo.setFlagId(po.getFlagId());
        vo.setSubmissionId(po.getSubmissionId());
        vo.setTarget(po.getTarget());
        vo.setDescription(po.getDescription());
        vo.setStatus(po.getStatus());
        vo.setJudgeId(po.getJudgeId());
        vo.setJudgeName(judgeName);
        vo.setSupplementEnd(po.getSupplementEnd());
        vo.setSupplementOpenTime(po.getSupplementOpenTime());
        vo.setWindowOpen(po.windowOpen(now));
        vo.setResolvedVersionNo(po.getResolvedVersionNo());
        vo.setResolveTime(po.getResolveTime());
        vo.setCloseReason(po.getCloseReason());
        vo.setCreateTime(po.getCreateTime());
        vo.setVersion(po.getVersion());
        return vo;
    }
}
