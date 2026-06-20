package com.gsswec.ecommerce.users.application.usecase;

import com.gsswec.ecommerce.users.application.port.out.PasswordHasher;
import com.gsswec.ecommerce.users.application.port.out.RefreshTokenRepository;
import com.gsswec.ecommerce.users.application.port.out.TokenIssuer;
import com.gsswec.ecommerce.users.application.port.out.UserRepository;
import com.gsswec.ecommerce.users.domain.exception.InvalidCredentialsException;
import com.gsswec.ecommerce.users.domain.model.RefreshToken;
import com.gsswec.ecommerce.users.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticateUser {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;

    public AuthenticateUser(
            UserRepository users,
            RefreshTokenRepository refreshTokens,
            PasswordHasher passwordHasher,
            TokenIssuer tokenIssuer) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
    }

    @Transactional
    public AuthResult login(Command command) {
        String email = command.email().trim().toLowerCase();

        User user = users.findByEmail(email)
                .filter(u -> passwordHasher.matches(command.password(), u.passwordHash()))
                .filter(User::active)
                .orElseThrow(InvalidCredentialsException::new);

        TokenIssuer.AccessToken access = tokenIssuer.issueAccessToken(user);
        TokenIssuer.RefreshTokenValue refresh = tokenIssuer.issueRefreshToken();
        refreshTokens.save(RefreshToken.issue(user.id(), refresh.hash(), refresh.expiresAt()));

        return new AuthResult(
                user,
                access.value(), access.expiresAt(),
                refresh.rawValue(), refresh.expiresAt());
    }

    public record Command(String email, String password) {
    }
}
