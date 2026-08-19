package org.hackathon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.s3")
public record S3Properties(
        String endpoint,
        String region,
        String accessKeyId,
        String secretAccessKey,
        String bucketName,
        boolean pathStyle,
        Long putExpireMinutes,
        Long pendingExpireHours,
        Long deletedRetainHours
) {
}
