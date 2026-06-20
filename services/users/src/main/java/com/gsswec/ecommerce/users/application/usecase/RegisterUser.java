package com.gsswec.ecommerce.users.application.usecase;

import com.gsswec.ecommerce.shared.constants.StreamNames;
import com.gsswec.ecommerce.shared.events.base.BaseEvent;
import com.gsswec.ecommerce.shared.events.user.UserRegisteredEvent;
import com.gsswec.ecommerce.users.application.port.out.EventPublisher;
import com.gsswec.ecommerce.users.application.port.out.PasswordHasher;
import com.gsswec.ecommerce.users.application.port.out.RefreshTokenRepository;
import com.gsswec.ecommerce.users.application.port.out.TokenIssuer;
import com.gsswec.ecommerce.users.application.port.out.UserRepository;
import com.gsswec.ecommerce.users.domain.exception.EmailAlreadyExistsException;
import com.gsswec.ecommerce.users.domain.model.RefreshToken;
import com.gsswec.ecommerce.users.domain.model.User;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUser {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;
    private final EventPublisher events;
    private final Clock clock;

    public RegisterUser(
            UserRepository users,
            RefreshTokenRepository refreshTokens,
            PasswordHasher passwordHasher,
            TokenIssuer tokenIssuer,
            EventPublisher events,
            Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public AuthResult register(Command command) {
        String email = command.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        User saved = users.save(User.register(
                email,
                passwordHasher.hash(command.password()),
                command.firstName().trim(),
                command.lastName().trim()));

        TokenIssuer.AccessToken access = tokenIssuer.issueAccessToken(saved);
        TokenIssuer.RefreshTokenValue refresh = tokenIssuer.issueRefreshToken();
        refreshTokens.save(RefreshToken.issue(saved.id(), refresh.hash(), refresh.expiresAt()));

        events.publish(StreamNames.USER_REGISTERED, new UserRegisteredEvent(
                baseEvent(StreamNames.USER_REGISTERED),
                saved.id(),
                saved.email(),
                saved.role().name()));

        return new AuthResult(
                saved,
                access.value(), access.expiresAt(),
                refresh.rawValue(), refresh.expiresAt());
    }

    private BaseEvent baseEvent(String eventType) {
        return new BaseEvent(
                UUID.randomUUID(),
                eventType,
                "1.0",
                Instant.now(clock),
                null,
                "users");
    }

    public record Command(String email, String password, String firstName, String lastName) {
    }
}
