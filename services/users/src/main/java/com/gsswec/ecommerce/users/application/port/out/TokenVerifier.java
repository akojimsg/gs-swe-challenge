package com.gsswec.ecommerce.users.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface TokenVerifier {

    Optional<VerifiedToken> verify(String accessToken);

    record VerifiedToken(UUID userId, String email, String role) {
    }
}
