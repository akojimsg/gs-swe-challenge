package com.gsswec.ecommerce.users.api.rest.dto;

import com.gsswec.ecommerce.users.application.usecase.AuthResult;

public record AuthResponse(
        String accessToken,
        String tokenType,
        UserResponse user) {

    public static AuthResponse from(AuthResult result) {
        return new AuthResponse(result.accessToken(), "Bearer", UserResponse.from(result.user()));
    }
}
