package com.gsswec.ecommerce.users.application.usecase;

import com.gsswec.ecommerce.users.application.port.out.RefreshTokenRepository;
import com.gsswec.ecommerce.users.application.port.out.TokenIssuer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RevokeRefreshToken {

    private final RefreshTokenRepository refreshTokens;
    private final TokenIssuer tokenIssuer;

    public RevokeRefreshToken(RefreshTokenRepository refreshTokens, TokenIssuer tokenIssuer) {
        this.refreshTokens = refreshTokens;
        this.tokenIssuer = tokenIssuer;
    }

    // Idempotent: a missing, unknown, or already-revoked token is a no-op so that
    // logout always succeeds and never leaks whether a token existed.
    @Transactional
    public void revoke(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String hash = tokenIssuer.hashRefreshToken(rawRefreshToken);
        refreshTokens.findByTokenHash(hash)
                .filter(t -> !t.revoked())
                .ifPresent(t -> refreshTokens.save(t.revoke()));
    }
}
