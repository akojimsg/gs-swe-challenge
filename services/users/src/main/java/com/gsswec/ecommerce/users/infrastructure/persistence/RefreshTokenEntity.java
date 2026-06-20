package com.gsswec.ecommerce.users.infrastructure.persistence;

import com.gsswec.ecommerce.users.domain.model.RefreshToken;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens", schema = "users_schema")
public class RefreshTokenEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected RefreshTokenEntity() {
    }

    static RefreshTokenEntity fromDomain(RefreshToken token) {
        RefreshTokenEntity e = new RefreshTokenEntity();
        e.id = token.id() == null ? UUID.randomUUID() : token.id();
        e.userId = token.userId();
        e.tokenHash = token.tokenHash();
        e.expiresAt = token.expiresAt();
        e.revoked = token.revoked();
        return e;
    }

    RefreshToken toDomain() {
        return new RefreshToken(id, userId, tokenHash, expiresAt, revoked, createdAt);
    }
}
