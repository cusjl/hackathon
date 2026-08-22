package org.hackathon.data.vo;

import org.hackathon.data.po.SubmissionSnapshot;

import java.time.LocalDateTime;

/**
 * 某个历史版本的完整快照
 */
public record SubmissionVersionVO(
        Integer submissionId,
        Integer versionNo,
        Integer submitterId,
        String submitterName,
        LocalDateTime submitTime,
        String changeLog,
        SubmissionSnapshot snapshot
) {
}
