package org.hackathon.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hackathon.data.enums.FlagStatus;
import org.hackathon.data.enums.NoticeType;
import org.hackathon.data.po.ReviewFlag;
import org.hackathon.data.po.Team;
import org.hackathon.data.po.Submission;
import org.hackathon.mapper.ReviewFlagMapper;
import org.hackathon.mapper.SubmissionMapper;
import org.hackathon.mapper.TeamMapper;
import org.hackathon.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 补交窗口到期自动关闭。
 * 校验侧另有 {@link ReviewFlag#windowOpen} 按当前时间判定，定时任务只负责落库与通知，
 * 任务未跑到时窗口也不会被误判为有效。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SupplementWindowTask {

    private final ReviewFlagMapper flagMapper;
    private final SubmissionMapper submissionMapper;
    private final TeamMapper teamMapper;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 */5 * * * ?")
    public void closeExpired() {
        LocalDateTime now = LocalDateTime.now();
        List<ReviewFlag> flags = flagMapper.selectExpiredWindows(now);
        for (ReviewFlag flag : flags) {
            flag.setStatus(FlagStatus.CLOSED);
            flag.setCloseReason("补交窗口已到期，系统自动关闭");
            flag.setUpdateTime(now);
            if (flagMapper.updateById(flag) == 0) continue;
            notifyLeader(flag);
        }
        if (!flags.isEmpty()) {
            log.info("关闭到期的补交窗口 {} 个", flags.size());
        }
    }

    private void notifyLeader(ReviewFlag flag) {
        Submission submission = submissionMapper.selectById(flag.getSubmissionId());
        if (submission == null) return;
        Team team = teamMapper.selectById(submission.getTeamId());
        if (team == null) return;
        notificationService.push(team.getLeaderId(), NoticeType.SUPPLEMENT_CLOSED,
                "补交窗口已关闭",
                "作品「%s」的补交窗口已到期关闭，如仍需补交请联系赛事管理员。"
                        .formatted(flag.getTarget().getDesc()),
                team.getEventId(), flag.getPhaseId(), flag.getSubmissionId(), flag.getFlagId());
    }
}
