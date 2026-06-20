package com.gsswec.ecommerce.users.domain.model;

import java.time.Instant;
import java.util.UUID;

public record RefreshToken(
        UUID id,
        UUID userId,
        String tokenHash,
        Instant expiresAt,
        boolean revoked,
        Instant createdAt) {

    public static RefreshToken issue(UUID userId, String tokenHash, Instant expiresAt) {
        return new RefreshToken(null, userId, tokenHash, expiresAt, false, null);
    }

    public boolean isActive(Instant now) {
        return !revoked && expiresAt.isAfter(now);
    }
}
