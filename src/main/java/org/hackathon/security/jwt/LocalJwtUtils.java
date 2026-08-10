package org.hackathon.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.exception.BusinessException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LocalJwtUtils {
    private final LocalJwtProperties localProperties;

    private SecretKey getKey(){
        return Keys.hmacShaKeyFor(localProperties.secretKey().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(LocalJwt jwt, boolean isTemp) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("name", jwt.getName());
        claims.put("isStudent", jwt.getIsStudent());
        claims.put("casId", jwt.getCasId());
        return Jwts.builder()
                .claims(claims)
                .subject(jwt.getUserId().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() +
                        (isTemp ? localProperties.shortExpiration(): localProperties.expiration())))
                .signWith(getKey())
                .compact();
    }

    public LocalJwt parseToken(String jwt) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();
            return new LocalJwt(
                    Integer.valueOf(claims.getSubject()),
                    claims.get("name", String.class),
                    claims.get("isStudent", Boolean.class),
                    claims.get("casId", String.class)
            );
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.TOKEN_UNREADABLE);
        }
    }

    public LocalJwt extractJwt(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null) {
            throw new BusinessException(ResultCode.TOKEN_IS_BLANK);
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token.isBlank()) {
            throw new BusinessException(ResultCode.TOKEN_IS_BLANK);
        }
        LocalJwt jwt = parseToken(token);
        if (jwt.getUserId() == -1) {
            throw new BusinessException(ResultCode.NOT_REGISTERED);
        }
        return jwt;
    }
}
