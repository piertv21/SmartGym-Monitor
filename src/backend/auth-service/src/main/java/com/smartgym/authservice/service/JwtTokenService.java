package com.smartgym.authservice.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenService {

    private final String issuer;
    private final String audience;
    private final long accessTokenTtlSeconds;
    private final SecretKey signingKey;

    public JwtTokenService(
            @Value("${security.jwt.issuer:smartgym-auth}") String issuer,
            @Value("${security.jwt.audience:smartgym-gateway}") String audience,
            @Value("${security.jwt.access-token-ttl-seconds:86400}") long accessTokenTtlSeconds,
            @Value("${security.jwt.secret:}") String sharedSecret) {
        this.issuer = issuer;
        this.audience = audience;
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;

        if (sharedSecret == null || sharedSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT configuration missing: provide security.jwt.secret");
        }

        this.signingKey = Keys.hmacShaKeyFor(sharedSecret.getBytes());
    }

    public String generateAccessToken(String userId) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(userId)
                .issuer(issuer)
                .audience()
                .add(audience)
                .and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenTtlSeconds)))
                .signWith(signingKey)
                .compact();
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }
}
