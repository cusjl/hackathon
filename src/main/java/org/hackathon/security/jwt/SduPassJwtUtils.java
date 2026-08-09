package org.hackathon.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;

import static io.jsonwebtoken.security.Keys.hmacShaKeyFor;

@Component
@Slf4j
public class SduPassJwtUtils {

    private final SecretKey sduPassSecretKey;

    private final static String ALGO = "PBKDF2WithHmacSHA256";

    public SduPassJwtUtils(SduPassJwtProperties properties) throws Exception {

        KeySpec spec = new PBEKeySpec(
                properties.sduPassSecret().toCharArray(),
                properties.salt().getBytes(StandardCharsets.UTF_8),
                properties.iterationCound(),
                properties.keyLength()
        );
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGO);

        this.sduPassSecretKey = hmacShaKeyFor(
                factory.generateSecret(spec).getEncoded()
        );
    }

    public SduPassJwtPayload parseSduPassJwt(String sduPassJwt) {
        Claims claims = Jwts.parser().verifyWith(sduPassSecretKey)
                .build()
                .parseSignedClaims(sduPassJwt)
                .getPayload();

        return SduPassJwtPayload.fromClaims(claims);
    }

}
