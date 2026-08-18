package org.hackathon.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.exception.BusinessException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LocalJwtUtils {

    private static final String BEARER = "Bearer ";

    private final LocalJwtProperties localProperties;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(localProperties.secretKey().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(LocalJwt jwt, LocalJwt.Type type) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("name", jwt.getName());
        claims.put("studentFlag", jwt.getStudentFlag());
        claims.put("casId", jwt.getCasId());
        claims.put("typ", type.name());
        long ttl = type == LocalJwt.Type.ACCESS
                ? localProperties.expiration() : localProperties.shortExpiration();
        return Jwts.builder()
                .claims(claims)
                .subject(jwt.getUserId() == null ? "0" : jwt.getUserId().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttl))
                .signWith(getKey())
                .compact();
    }

    public LocalJwt parseToken(String token, LocalJwt.Type expected) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!expected.name().equals(claims.get("typ", String.class))) {
                throw new BusinessException(ResultCode.TOKEN_UNREADABLE, "token 类型不符");
            }
            return toJwt(claims);
        } catch (BusinessException e) {
            throw e;
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.TOKEN_UNREADABLE);
        }
    }

    public LocalJwt.Resolved tryResolve(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || header.isBlank()) return LocalJwt.Resolved.ANONYMOUS;

        String token = header.startsWith(BEARER) ? header.substring(BEARER.length()) : header;
        if (token.isBlank()) return LocalJwt.Resolved.ANONYMOUS;

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!LocalJwt.Type.ACCESS.name().equals(claims.get("typ", String.class))) {
                return LocalJwt.Resolved.INVALID;
            }
            return LocalJwt.Resolved.ok(toJwt(claims));
        } catch (ExpiredJwtException e) {
            return LocalJwt.Resolved.EXPIRED;
        } catch (Exception e) {
            return LocalJwt.Resolved.INVALID;
        }
    }

    private LocalJwt toJwt(Claims claims) {
        int userId = Integer.parseInt(claims.getSubject());
        return new LocalJwt(
                userId == 0 ? null : userId,
                claims.get("name", String.class),
                claims.get("studentFlag", Boolean.class),
                claims.get("casId", String.class)
        );
    }
}
