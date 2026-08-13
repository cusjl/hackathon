package org.hackathon.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("jwt.sdu-pass")
public record SduPassJwtProperties(
        String sduPassSecret,
        String salt,
        Integer iterationCound,
        Integer keyLength
) {
}
