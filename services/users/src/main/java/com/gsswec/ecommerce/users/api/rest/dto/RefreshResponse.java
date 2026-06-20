package com.gsswec.ecommerce.users.api.rest.dto;

import com.gsswec.ecommerce.users.application.usecase.TokenRefreshResult;

public record RefreshResponse(
        String accessToken,
        String tokenType) {

    public static RefreshResponse from(TokenRefreshResult result) {
        return new RefreshResponse(result.accessToken(), "Bearer");
    }
}
