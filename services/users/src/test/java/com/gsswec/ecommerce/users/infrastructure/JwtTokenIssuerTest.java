package com.gsswec.ecommerce.users.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.gsswec.ecommerce.users.application.port.out.TokenIssuer;
import com.gsswec.ecommerce.users.domain.model.Role;
import com.gsswec.ecommerce.users.domain.model.User;
import com.gsswec.ecommerce.users.infrastructure.security.JwtProperties;
import com.gsswec.ecommerce.users.infrastructure.security.JwtTokenIssuer;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenIssuerTest {

    private static final String SECRET = "test_secret_at_least_256_bits_long_for_hs256_xxxxx";
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-20T00:00:00Z"), ZoneOffset.UTC);
    private JwtTokenIssuer issuer;

    @BeforeEach
    void setUp() {
        issuer = new JwtTokenIssuer(
                new JwtProperties(SECRET, Duration.ofMinutes(15), Duration.ofDays(7)), clock);
    }

    private User user() {
        return new User(UUID.randomUUID(), "user@test.com", "hash", "Test", "User",
                Role.ADMIN, true, Instant.now(clock), Instant.now(clock));
    }

    @Test
    void accessTokenCarriesExpectedClaimsAndExpiry() {
        User user = user();
        TokenIssuer.AccessToken token = issuer.issueAccessToken(user);

        SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        var claims = Jwts.parser().verifyWith(key).clock(() -> java.util.Date.from(Instant.now(clock))).build()
                .parseSignedClaims(token.value()).getPayload();

        assertThat(claims.getSubject()).isEqualTo(user.id().toString());
        assertThat(claims.get("email")).isEqualTo("user@test.com");
        assertThat(claims.get("role")).isEqualTo("ADMIN");
        assertThat(token.expiresAt()).isEqualTo(Instant.parse("2026-06-20T00:15:00Z"));
    }

    @Test
    void refreshTokenHashIsDeterministicAndOpaque() {
        TokenIssuer.RefreshTokenValue refresh = issuer.issueRefreshToken();

        assertThat(refresh.rawValue()).isNotEqualTo(refresh.hash());
        assertThat(issuer.hashRefreshToken(refresh.rawValue())).isEqualTo(refresh.hash());
        assertThat(refresh.expiresAt()).isEqualTo(Instant.parse("2026-06-27T00:00:00Z"));
    }
}
