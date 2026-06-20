package com.gsswec.ecommerce.users.application.usecase;

import com.gsswec.ecommerce.users.domain.model.User;
import java.time.Instant;

public record AuthResult(
        User user,
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt) {
}
