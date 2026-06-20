package com.gsswec.ecommerce.users.domain.exception;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Refresh token is missing, expired, or revoked");
    }
}
