package com.gsswec.ecommerce.users.api.rest;

import com.gsswec.ecommerce.users.api.rest.dto.AuthResponse;
import com.gsswec.ecommerce.users.api.rest.dto.LoginRequest;
import com.gsswec.ecommerce.users.api.rest.dto.RegisterRequest;
import com.gsswec.ecommerce.users.application.usecase.AuthResult;
import com.gsswec.ecommerce.users.application.usecase.AuthenticateUser;
import com.gsswec.ecommerce.users.application.usecase.RegisterUser;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_COOKIE = "refresh_token";

    private final RegisterUser registerUser;
    private final AuthenticateUser authenticateUser;

    public AuthController(RegisterUser registerUser, AuthenticateUser authenticateUser) {
        this.registerUser = registerUser;
        this.authenticateUser = authenticateUser;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResult result = registerUser.register(new RegisterUser.Command(
                request.email(), request.password(), request.firstName(), request.lastName()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, refreshCookie(result).toString())
                .body(AuthResponse.from(result));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authenticateUser.login(new AuthenticateUser.Command(
                request.email(), request.password()));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(result).toString())
                .body(AuthResponse.from(result));
    }

    private ResponseCookie refreshCookie(AuthResult result) {
        Duration maxAge = Duration.between(Instant.now(), result.refreshTokenExpiresAt());
        return ResponseCookie.from(REFRESH_COOKIE, result.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/api/v1/auth")
                .sameSite("Strict")
                .maxAge(maxAge)
                .build();
    }
}
