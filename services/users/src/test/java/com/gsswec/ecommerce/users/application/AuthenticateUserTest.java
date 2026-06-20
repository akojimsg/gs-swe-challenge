package com.gsswec.ecommerce.users.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gsswec.ecommerce.users.application.port.out.PasswordHasher;
import com.gsswec.ecommerce.users.application.port.out.RefreshTokenRepository;
import com.gsswec.ecommerce.users.application.port.out.TokenIssuer;
import com.gsswec.ecommerce.users.application.port.out.UserRepository;
import com.gsswec.ecommerce.users.application.usecase.AuthResult;
import com.gsswec.ecommerce.users.application.usecase.AuthenticateUser;
import com.gsswec.ecommerce.users.domain.exception.InvalidCredentialsException;
import com.gsswec.ecommerce.users.domain.model.Role;
import com.gsswec.ecommerce.users.domain.model.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthenticateUserTest {

    private UserRepository users;
    private RefreshTokenRepository refreshTokens;
    private PasswordHasher passwordHasher;
    private TokenIssuer tokenIssuer;
    private AuthenticateUser authenticateUser;

    @BeforeEach
    void setUp() {
        users = org.mockito.Mockito.mock(UserRepository.class);
        refreshTokens = org.mockito.Mockito.mock(RefreshTokenRepository.class);
        passwordHasher = org.mockito.Mockito.mock(PasswordHasher.class);
        tokenIssuer = org.mockito.Mockito.mock(TokenIssuer.class);
        authenticateUser = new AuthenticateUser(users, refreshTokens, passwordHasher, tokenIssuer);
    }

    private User existingUser(boolean active) {
        return new User(UUID.randomUUID(), "user@test.com", "hashed", "Test", "User",
                Role.BUYER, active, Instant.now(), Instant.now());
    }

    private void stubTokens() {
        when(tokenIssuer.issueAccessToken(any()))
                .thenReturn(new TokenIssuer.AccessToken("jwt", Instant.now().plusSeconds(900)));
        when(tokenIssuer.issueRefreshToken())
                .thenReturn(new TokenIssuer.RefreshTokenValue("raw", "hash", Instant.now().plusSeconds(604800)));
    }

    @Test
    void issuesTokensForValidCredentials() {
        when(users.findByEmail("user@test.com")).thenReturn(Optional.of(existingUser(true)));
        when(passwordHasher.matches("password123", "hashed")).thenReturn(true);
        stubTokens();

        AuthResult result = authenticateUser.login(
                new AuthenticateUser.Command("User@Test.com", "password123"));

        assertThat(result.accessToken()).isEqualTo("jwt");
        verify(refreshTokens).save(any());
    }

    @Test
    void rejectsWrongPassword() {
        when(users.findByEmail("user@test.com")).thenReturn(Optional.of(existingUser(true)));
        when(passwordHasher.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authenticateUser.login(
                new AuthenticateUser.Command("user@test.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(refreshTokens, never()).save(any());
    }

    @Test
    void rejectsUnknownEmail() {
        when(users.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticateUser.login(
                new AuthenticateUser.Command("ghost@test.com", "password123")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsInactiveUser() {
        when(users.findByEmail("user@test.com")).thenReturn(Optional.of(existingUser(false)));
        when(passwordHasher.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> authenticateUser.login(
                new AuthenticateUser.Command("user@test.com", "password123")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
