package com.gsswec.ecommerce.users.application.usecase;

import java.time.Instant;

public record TokenRefreshResult(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt) {
}
