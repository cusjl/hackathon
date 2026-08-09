package org.hackathon.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("jwt.local")
public record LocalJwtProperties(
        String secretKey,
        Long expiration,
        Long shortExpiration
) {
}
