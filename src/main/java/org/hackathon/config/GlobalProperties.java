package org.hackathon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("hackathon")
public record GlobalProperties(
        Integer teamCapacity,
        String frontendUrl,
        String registerPath,
        String redirectPath,
        String callbackUrl
) {
}
