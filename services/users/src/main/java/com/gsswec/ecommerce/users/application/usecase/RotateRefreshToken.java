package com.gsswec.ecommerce.users.application.usecase;

import com.gsswec.ecommerce.users.application.port.out.RefreshTokenRepository;
import com.gsswec.ecommerce.users.application.port.out.TokenIssuer;
import com.gsswec.ecommerce.users.application.port.out.UserRepository;
import com.gsswec.ecommerce.users.domain.exception.InvalidRefreshTokenException;
import com.gsswec.ecommerce.users.domain.model.RefreshToken;
import com.gsswec.ecommerce.users.domain.model.User;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RotateRefreshToken {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final TokenIssuer tokenIssuer;
    private final Clock clock;

    public RotateRefreshToken(
            UserRepository users,
            RefreshTokenRepository refreshTokens,
            TokenIssuer tokenIssuer,
            Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.tokenIssuer = tokenIssuer;
        this.clock = clock;
    }

    @Transactional
    public TokenRefreshResult rotate(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        String hash = tokenIssuer.hashRefreshToken(rawRefreshToken);
        RefreshToken current = refreshTokens.findByTokenHash(hash)
                .filter(t -> t.isActive(Instant.now(clock)))
                .orElseThrow(InvalidRefreshTokenException::new);

        User user = users.findById(current.userId())
                .filter(User::active)
                .orElseThrow(InvalidRefreshTokenException::new);

        // Rotate: revoke the presented token and issue a fresh one.
        refreshTokens.save(current.revoke());
        TokenIssuer.RefreshTokenValue next = tokenIssuer.issueRefreshToken();
        refreshTokens.save(RefreshToken.issue(user.id(), next.hash(), next.expiresAt()));

        TokenIssuer.AccessToken access = tokenIssuer.issueAccessToken(user);
        return new TokenRefreshResult(
                access.value(), access.expiresAt(),
                next.rawValue(), next.expiresAt());
    }
}
