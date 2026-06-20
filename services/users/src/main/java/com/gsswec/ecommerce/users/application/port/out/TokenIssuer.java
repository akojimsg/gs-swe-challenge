package com.gsswec.ecommerce.users.application.port.out;

import com.gsswec.ecommerce.users.domain.model.User;
import java.time.Instant;

public interface TokenIssuer {

    AccessToken issueAccessToken(User user);

    RefreshTokenValue issueRefreshToken();

    String hashRefreshToken(String rawToken);

    record AccessToken(String value, Instant expiresAt) {
    }

    record RefreshTokenValue(String rawValue, String hash, Instant expiresAt) {
    }
}
