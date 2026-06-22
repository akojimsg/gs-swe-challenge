package com.gsswec.ecommerce.users.api.rest.dto;

import com.gsswec.ecommerce.users.domain.model.User;
import java.util.UUID;

public record UserEmailResponse(UUID userId, String email) {

    public static UserEmailResponse from(User user) {
        return new UserEmailResponse(user.id(), user.email());
    }
}
