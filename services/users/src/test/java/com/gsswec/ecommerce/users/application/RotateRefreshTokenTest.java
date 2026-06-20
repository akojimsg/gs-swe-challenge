package com.gsswec.ecommerce.users.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gsswec.ecommerce.users.application.port.out.RefreshTokenRepository;
import com.gsswec.ecommerce.users.application.port.out.TokenIssuer;
import com.gsswec.ecommerce.users.application.port.out.UserRepository;
import com.gsswec.ecommerce.users.application.usecase.RotateRefreshToken;
import com.gsswec.ecommerce.users.application.usecase.TokenRefreshResult;
import com.gsswec.ecommerce.users.domain.exception.InvalidRefreshTokenException;
import com.gsswec.ecommerce.users.domain.model.RefreshToken;
import com.gsswec.ecommerce.users.domain.model.Role;
import com.gsswec.ecommerce.users.domain.model.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RotateRefreshTokenTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-20T00:00:00Z"), ZoneOffset.UTC);
    private UserRepository users;
    private RefreshTokenRepository refreshTokens;
    private TokenIssuer tokenIssuer;
    private RotateRefreshToken rotate;

    @BeforeEach
    void setUp() {
        users = org.mockito.Mockito.mock(UserRepository.class);
        refreshTokens = org.mockito.Mockito.mock(RefreshTokenRepository.class);
        tokenIssuer = org.mockito.Mockito.mock(TokenIssuer.class);
        rotate = new RotateRefreshToken(users, refreshTokens, tokenIssuer, clock);
    }

    private final UUID userId = UUID.randomUUID();

    private User activeUser() {
        return new User(userId, "user@test.com", "hash", "Test", "User",
                Role.BUYER, true, Instant.now(clock), Instant.now(clock));
    }

    private RefreshToken storedToken(boolean revoked, Instant expiry) {
        return new RefreshToken(UUID.randomUUID(), userId, "oldhash", expiry, revoked, Instant.now(clock));
    }

    private void stubIssuer() {
        when(tokenIssuer.hashRefreshToken("raw-current")).thenReturn("oldhash");
        when(tokenIssuer.issueRefreshToken())
                .thenReturn(new TokenIssuer.RefreshTokenValue("raw-new", "newhash", Instant.parse("2026-06-27T00:00:00Z")));
        when(tokenIssuer.issueAccessToken(any()))
                .thenReturn(new TokenIssuer.AccessToken("new-jwt", Instant.parse("2026-06-20T00:15:00Z")));
    }

    @Test
    void rotatesByRevokingOldAndIssuingNew() {
        stubIssuer();
        when(refreshTokens.findByTokenHash("oldhash"))
                .thenReturn(Optional.of(storedToken(false, Instant.parse("2026-06-25T00:00:00Z"))));
        when(users.findById(userId)).thenReturn(Optional.of(activeUser()));

        TokenRefreshResult result = rotate.rotate("raw-current");

        assertThat(result.accessToken()).isEqualTo("new-jwt");
        assertThat(result.refreshToken()).isEqualTo("raw-new");

        // Two saves: the revoked old token, then the freshly issued one.
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokens, times(2)).save(saved.capture());
        assertThat(saved.getAllValues().get(0).revoked()).isTrue();
        assertThat(saved.getAllValues().get(1).tokenHash()).isEqualTo("newhash");
        assertThat(saved.getAllValues().get(1).revoked()).isFalse();
    }

    @Test
    void rejectsBlankToken() {
        assertThatThrownBy(() -> rotate.rotate("  "))
                .isInstanceOf(InvalidRefreshTokenException.class);
        verify(refreshTokens, never()).save(any());
    }

    @Test
    void rejectsUnknownToken() {
        when(tokenIssuer.hashRefreshToken(anyString())).thenReturn("oldhash");
        when(refreshTokens.findByTokenHash("oldhash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rotate.rotate("raw-current"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rejectsRevokedToken() {
        when(tokenIssuer.hashRefreshToken("raw-current")).thenReturn("oldhash");
        when(refreshTokens.findByTokenHash("oldhash"))
                .thenReturn(Optional.of(storedToken(true, Instant.parse("2026-06-25T00:00:00Z"))));

        assertThatThrownBy(() -> rotate.rotate("raw-current"))
                .isInstanceOf(InvalidRefreshTokenException.class);
        verify(refreshTokens, never()).save(any());
    }

    @Test
    void rejectsExpiredToken() {
        when(tokenIssuer.hashRefreshToken("raw-current")).thenReturn("oldhash");
        when(refreshTokens.findByTokenHash("oldhash"))
                .thenReturn(Optional.of(storedToken(false, Instant.parse("2026-06-19T00:00:00Z"))));

        assertThatThrownBy(() -> rotate.rotate("raw-current"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rejectsWhenUserInactive() {
        when(tokenIssuer.hashRefreshToken("raw-current")).thenReturn("oldhash");
        when(refreshTokens.findByTokenHash("oldhash"))
                .thenReturn(Optional.of(storedToken(false, Instant.parse("2026-06-25T00:00:00Z"))));
        User inactive = new User(userId, "user@test.com", "hash", "Test", "User",
                Role.BUYER, false, Instant.now(clock), Instant.now(clock));
        when(users.findById(userId)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> rotate.rotate("raw-current"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }
}
