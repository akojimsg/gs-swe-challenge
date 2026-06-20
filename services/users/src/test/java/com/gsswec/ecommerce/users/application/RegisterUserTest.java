package com.gsswec.ecommerce.users.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gsswec.ecommerce.shared.constants.StreamNames;
import com.gsswec.ecommerce.shared.events.base.DomainEvent;
import com.gsswec.ecommerce.shared.events.user.UserRegisteredEvent;
import com.gsswec.ecommerce.users.application.port.out.EventPublisher;
import com.gsswec.ecommerce.users.application.port.out.PasswordHasher;
import com.gsswec.ecommerce.users.application.port.out.RefreshTokenRepository;
import com.gsswec.ecommerce.users.application.port.out.TokenIssuer;
import com.gsswec.ecommerce.users.application.port.out.UserRepository;
import com.gsswec.ecommerce.users.application.usecase.AuthResult;
import com.gsswec.ecommerce.users.application.usecase.RegisterUser;
import com.gsswec.ecommerce.users.domain.exception.EmailAlreadyExistsException;
import com.gsswec.ecommerce.users.domain.model.Role;
import com.gsswec.ecommerce.users.domain.model.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RegisterUserTest {

    private UserRepository users;
    private RefreshTokenRepository refreshTokens;
    private PasswordHasher passwordHasher;
    private TokenIssuer tokenIssuer;
    private EventPublisher events;
    private RegisterUser registerUser;

    @BeforeEach
    void setUp() {
        users = org.mockito.Mockito.mock(UserRepository.class);
        refreshTokens = org.mockito.Mockito.mock(RefreshTokenRepository.class);
        passwordHasher = org.mockito.Mockito.mock(PasswordHasher.class);
        tokenIssuer = org.mockito.Mockito.mock(TokenIssuer.class);
        events = org.mockito.Mockito.mock(EventPublisher.class);
        Clock clock = Clock.fixed(Instant.parse("2026-06-20T00:00:00Z"), ZoneOffset.UTC);
        registerUser = new RegisterUser(users, refreshTokens, passwordHasher, tokenIssuer, events, clock);
    }

    private RegisterUser.Command command() {
        return new RegisterUser.Command("New@User.com", "password123", "New", "User");
    }

    private void stubHappyPath() {
        when(passwordHasher.hash(anyString())).thenReturn("hashed");
        when(users.existsByEmail(anyString())).thenReturn(false);
        when(users.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return new User(UUID.randomUUID(), u.email(), u.passwordHash(), u.firstName(),
                    u.lastName(), u.role(), u.active(), Instant.now(), Instant.now());
        });
        when(tokenIssuer.issueAccessToken(any()))
                .thenReturn(new TokenIssuer.AccessToken("jwt", Instant.now().plusSeconds(900)));
        when(tokenIssuer.issueRefreshToken())
                .thenReturn(new TokenIssuer.RefreshTokenValue("raw", "hash", Instant.now().plusSeconds(604800)));
    }

    @Test
    void registersBuyerWithHashedPasswordAndNormalisedEmail() {
        stubHappyPath();

        AuthResult result = registerUser.register(command());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().email()).isEqualTo("new@user.com");
        assertThat(saved.getValue().passwordHash()).isEqualTo("hashed");
        assertThat(saved.getValue().role()).isEqualTo(Role.BUYER);
        assertThat(result.accessToken()).isEqualTo("jwt");
        assertThat(result.refreshToken()).isEqualTo("raw");
    }

    @Test
    void persistsHashedRefreshTokenAndPublishesEvent() {
        stubHappyPath();

        registerUser.register(command());

        verify(refreshTokens).save(any());
        ArgumentCaptor<DomainEvent> event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(events).publish(eq(StreamNames.USER_REGISTERED), event.capture());
        assertThat(event.getValue()).isInstanceOf(UserRegisteredEvent.class);
        UserRegisteredEvent e = (UserRegisteredEvent) event.getValue();
        assertThat(e.email()).isEqualTo("new@user.com");
        assertThat(e.role()).isEqualTo("BUYER");
        assertThat(e.base().producedBy()).isEqualTo("users");
    }

    @Test
    void rejectsDuplicateEmailWithoutSavingOrPublishing() {
        when(users.existsByEmail("new@user.com")).thenReturn(true);

        assertThatThrownBy(() -> registerUser.register(command()))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(users, never()).save(any());
        verify(events, never()).publish(anyString(), any());
    }
}
