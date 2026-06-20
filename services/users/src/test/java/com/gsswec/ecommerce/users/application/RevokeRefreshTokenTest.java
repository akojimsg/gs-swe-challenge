package com.gsswec.ecommerce.users.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gsswec.ecommerce.users.application.port.out.RefreshTokenRepository;
import com.gsswec.ecommerce.users.application.port.out.TokenIssuer;
import com.gsswec.ecommerce.users.application.usecase.RevokeRefreshToken;
import com.gsswec.ecommerce.users.domain.model.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RevokeRefreshTokenTest {

    private RefreshTokenRepository refreshTokens;
    private TokenIssuer tokenIssuer;
    private RevokeRefreshToken revoke;

    @BeforeEach
    void setUp() {
        refreshTokens = org.mockito.Mockito.mock(RefreshTokenRepository.class);
        tokenIssuer = org.mockito.Mockito.mock(TokenIssuer.class);
        revoke = new RevokeRefreshToken(refreshTokens, tokenIssuer);
    }

    private RefreshToken stored(boolean revoked) {
        return new RefreshToken(UUID.randomUUID(), UUID.randomUUID(), "hash",
                Instant.parse("2026-06-27T00:00:00Z"), revoked, Instant.EPOCH);
    }

    @Test
    void revokesAnActiveToken() {
        when(tokenIssuer.hashRefreshToken("raw")).thenReturn("hash");
        when(refreshTokens.findByTokenHash("hash")).thenReturn(Optional.of(stored(false)));

        revoke.revoke("raw");

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokens).save(saved.capture());
        assertThat(saved.getValue().revoked()).isTrue();
    }

    @Test
    void isNoOpForBlankToken() {
        revoke.revoke(null);
        revoke.revoke("  ");
        verify(refreshTokens, never()).save(any());
    }

    @Test
    void isNoOpForUnknownToken() {
        when(tokenIssuer.hashRefreshToken(anyString())).thenReturn("hash");
        when(refreshTokens.findByTokenHash("hash")).thenReturn(Optional.empty());

        revoke.revoke("raw");

        verify(refreshTokens, never()).save(any());
    }

    @Test
    void isNoOpForAlreadyRevokedToken() {
        when(tokenIssuer.hashRefreshToken("raw")).thenReturn("hash");
        when(refreshTokens.findByTokenHash("hash")).thenReturn(Optional.of(stored(true)));

        revoke.revoke("raw");

        verify(refreshTokens, never()).save(any());
    }
}
