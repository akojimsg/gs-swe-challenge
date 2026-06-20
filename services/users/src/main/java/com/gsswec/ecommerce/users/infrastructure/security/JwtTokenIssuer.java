package com.gsswec.ecommerce.users.infrastructure.security;

import com.gsswec.ecommerce.users.application.port.out.TokenIssuer;
import com.gsswec.ecommerce.users.domain.model.User;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenIssuer implements TokenIssuer {

    private final JwtProperties properties;
    private final SecretKey signingKey;
    private final Clock clock;

    public JwtTokenIssuer(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.signingKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public AccessToken issueAccessToken(User user) {
        Instant now = Instant.now(clock);
        Instant expiry = now.plus(properties.accessTokenTtl());
        String jwt = Jwts.builder()
                .subject(user.id().toString())
                .claim("email", user.email())
                .claim("role", user.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
        return new AccessToken(jwt, expiry);
    }

    @Override
    public RefreshTokenValue issueRefreshToken() {
        String raw = UUID.randomUUID().toString();
        Instant expiry = Instant.now(clock).plus(properties.refreshTokenTtl());
        return new RefreshTokenValue(raw, hashRefreshToken(raw), expiry);
    }

    @Override
    public String hashRefreshToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
