package org.hackathon.security.jwt;

import io.jsonwebtoken.Claims;

public record SduPassJwtPayload(
        String casID,
        String name,
        Long exp
) {

    public static SduPassJwtPayload fromClaims(Claims claims) {
        return new SduPassJwtPayload(
                claims.get("casID", String.class),
                claims.get("name", String.class),
                claims.get("exp", Long.class)
        );
    }
}
