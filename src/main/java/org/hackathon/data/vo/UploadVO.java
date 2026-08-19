package org.hackathon.data.vo;

import java.time.LocalDateTime;

public record UploadVO(
        Long fileId,
        String uploadUrl,
        String contentType,
        Long maxSizeBytes,
        LocalDateTime expiresAt
) {
}
