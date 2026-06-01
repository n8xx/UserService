package com.innowise.userservice.security;

import com.innowise.userservice.config.JwtConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtConfig jwtConfig;

    public void validateToken(String token) {
        parser().parseSignedClaims(token);
    }

    public Long getUserIdFromToken(String token) {
        return parser().parseSignedClaims(token)
                .getPayload()
                .get("userId", Long.class);
    }

    public String getRoleFromToken(String token) {
        return parser().parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    private io.jsonwebtoken.JwtParser parser() {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtConfig.getSecret()));
    }
}