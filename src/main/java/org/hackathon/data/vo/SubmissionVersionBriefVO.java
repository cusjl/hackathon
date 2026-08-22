package org.hackathon.data.vo;

import java.time.LocalDateTime;

public record SubmissionVersionBriefVO(
        Integer versionNo,
        Integer submitterId,
        String submitterName,
        LocalDateTime submitTime,
        String changeLog
) {
}
