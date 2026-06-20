package com.gsswec.ecommerce.users.api.rest;

import com.gsswec.ecommerce.users.api.rest.dto.AuthResponse;
import com.gsswec.ecommerce.users.api.rest.dto.LoginRequest;
import com.gsswec.ecommerce.users.api.rest.dto.RefreshResponse;
import com.gsswec.ecommerce.users.api.rest.dto.RegisterRequest;
import com.gsswec.ecommerce.users.application.usecase.AuthResult;
import com.gsswec.ecommerce.users.application.usecase.AuthenticateUser;
import com.gsswec.ecommerce.users.application.usecase.RegisterUser;
import com.gsswec.ecommerce.users.application.usecase.RevokeRefreshToken;
import com.gsswec.ecommerce.users.application.usecase.RotateRefreshToken;
import com.gsswec.ecommerce.users.application.usecase.TokenRefreshResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Registration, login, and token issuance")
public class AuthController {

    private static final String REFRESH_COOKIE = "refresh_token";

    private final RegisterUser registerUser;
    private final AuthenticateUser authenticateUser;
    private final RotateRefreshToken rotateRefreshToken;
    private final RevokeRefreshToken revokeRefreshToken;

    public AuthController(
            RegisterUser registerUser,
            AuthenticateUser authenticateUser,
            RotateRefreshToken rotateRefreshToken,
            RevokeRefreshToken revokeRefreshToken) {
        this.registerUser = registerUser;
        this.authenticateUser = authenticateUser;
        this.rotateRefreshToken = rotateRefreshToken;
        this.revokeRefreshToken = revokeRefreshToken;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new BUYER account and issue tokens")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Account created; access token returned, refresh cookie set"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @io.swagger.v3.oas.annotations.media.Content),
        @ApiResponse(responseCode = "409", description = "Email already registered", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResult result = registerUser.register(new RegisterUser.Command(
                request.email(), request.password(), request.firstName(), request.lastName()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, refreshCookie(result).toString())
                .body(AuthResponse.from(result));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email and password and issue tokens")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authenticated; access token returned, refresh cookie set"),
        @ApiResponse(responseCode = "401", description = "Invalid email or password", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authenticateUser.login(new AuthenticateUser.Command(
                request.email(), request.password()));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(result).toString())
                .body(AuthResponse.from(result));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate the refresh token and issue a new access token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "New access token returned; refresh cookie rotated"),
        @ApiResponse(responseCode = "401", description = "Missing, expired, or revoked refresh token", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<RefreshResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        TokenRefreshResult result = rotateRefreshToken.rotate(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        refreshCookie(result.refreshToken(), result.refreshTokenExpiresAt()).toString())
                .body(RefreshResponse.from(result));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the refresh token and clear the session cookie")
    @ApiResponses(
        @ApiResponse(responseCode = "204", description = "Logged out (idempotent)"))
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        revokeRefreshToken.revoke(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearedRefreshCookie().toString())
                .build();
    }

    private ResponseCookie refreshCookie(AuthResult result) {
        return refreshCookie(result.refreshToken(), result.refreshTokenExpiresAt());
    }

    private ResponseCookie refreshCookie(String rawValue, Instant expiresAt) {
        return baseRefreshCookie(rawValue)
                .maxAge(Duration.between(Instant.now(), expiresAt))
                .build();
    }

    private ResponseCookie clearedRefreshCookie() {
        return baseRefreshCookie("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder baseRefreshCookie(String rawValue) {
        return ResponseCookie.from(REFRESH_COOKIE, rawValue)
                .httpOnly(true)
                .secure(true)
                .path("/api/v1/auth")
                .sameSite("Strict");
    }
}
