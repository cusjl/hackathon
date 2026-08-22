package org.hackathon.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hackathon.data.enums.SubmissionStatus;
import org.hackathon.data.po.Submission;
import org.hackathon.mapper.SubmissionMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提交截止自动锁定：轮次提交截止后把作品状态推进为“已提交，待评审”。
 * 查询侧另有 {@link Submission#derivedStatus} 按当前时间派生状态兜底。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubmissionLockTask {

    private final SubmissionMapper submissionMapper;

    @Scheduled(cron = "0 */5 * * * ?")
    public void lockExpired() {
        List<Submission> submissions = submissionMapper.selectExpiredSubmissions();
        LocalDateTime now = LocalDateTime.now();
        for (Submission submission : submissions) {
            submission.setStatus(SubmissionStatus.LOCKED);
            submission.setUpdateTime(now);
            submissionMapper.updateById(submission);
        }
        if (!submissions.isEmpty()) {
            log.info("锁定已截止作品 {} 份", submissions.size());
        }
    }
}
