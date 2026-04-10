package com.smartgym.gateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.Date;

@Component
public class JwtValidationService {

    private final SecretKey signingKey;
    private final String expectedIssuer;
    private final String expectedAudience;

    public JwtValidationService(
            @Value("${security.jwt.secret:}") String sharedSecret,
            @Value("${security.jwt.issuer:smartgym-auth}") String expectedIssuer,
            @Value("${security.jwt.audience:smartgym-gateway}") String expectedAudience
    ) {
        this.expectedIssuer = expectedIssuer;
        this.expectedAudience = expectedAudience;

        if (sharedSecret == null || sharedSecret.isBlank()) {
            throw new IllegalStateException("JWT configuration missing: provide security.jwt.secret");
        }

        this.signingKey = Keys.hmacShaKeyFor(sharedSecret.getBytes());
    }

    public String validateAndExtractUserId(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);

            if (!"HS256".equals(jws.getHeader().getAlgorithm())) {
                throw new IllegalStateException("Invalid JWT algorithm");
            }

            Claims claims = jws.getPayload();

            String issuer = claims.getIssuer();
            if (!expectedIssuer.equals(issuer)) {
                throw new JwtException("Invalid token issuer");
            }

            Object audienceClaim = claims.get("aud");
            if (audienceClaim instanceof String aud) {
                if (!expectedAudience.equals(aud)) {
                    throw new JwtException("Invalid token audience");
                }
            } else if (audienceClaim instanceof Collection<?> audList) {
                if (!audList.contains(expectedAudience)) {
                    throw new JwtException("Invalid token audience");
                }
            } else {
                throw new JwtException("Missing token audience");
            }

            Date expiration = claims.getExpiration();
            if (expiration == null) {
                throw new JwtException("Missing token expiration");
            }
            if (!expiration.after(new Date())) {
                throw new JwtException("Expired token");
            }

            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new IllegalStateException("Invalid JWT token", ex);
        }
    }
}