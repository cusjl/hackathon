package org.hackathon.data.vo;

import org.hackathon.data.enums.FileScope;
import org.hackathon.data.enums.FileStatus;

import java.time.LocalDateTime;

public record FileInfoVO(
        Long fileId,
        FileScope scope,
        String originName,
        String contentType,
        Long sizeBytes,
        FileStatus status,
        LocalDateTime createTime
) {
}
