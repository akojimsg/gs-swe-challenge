package com.gsswec.ecommerce.users.infrastructure.persistence;

import com.gsswec.ecommerce.users.application.port.out.RefreshTokenRepository;
import com.gsswec.ecommerce.users.domain.model.RefreshToken;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        return jpa.save(RefreshTokenEntity.fromDomain(token)).toDomain();
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(RefreshTokenEntity::toDomain);
    }
}
