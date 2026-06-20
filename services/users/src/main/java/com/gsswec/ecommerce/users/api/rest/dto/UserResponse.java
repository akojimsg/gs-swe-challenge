package com.gsswec.ecommerce.users.api.rest.dto;

import com.gsswec.ecommerce.users.domain.model.User;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.id(), user.email(), user.firstName(), user.lastName(), user.role().name());
    }
}
