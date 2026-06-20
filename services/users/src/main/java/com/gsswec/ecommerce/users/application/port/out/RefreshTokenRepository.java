package com.gsswec.ecommerce.users.application.port.out;

import com.gsswec.ecommerce.users.domain.model.RefreshToken;
import java.util.Optional;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
