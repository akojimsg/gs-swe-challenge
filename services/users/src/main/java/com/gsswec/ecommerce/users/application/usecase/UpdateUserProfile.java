package com.gsswec.ecommerce.users.application.usecase;

import com.gsswec.ecommerce.users.application.port.out.UserRepository;
import com.gsswec.ecommerce.users.domain.exception.UserNotFoundException;
import com.gsswec.ecommerce.users.domain.model.User;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateUserProfile {

    private final UserRepository users;

    public UpdateUserProfile(UserRepository users) {
        this.users = users;
    }

    @Transactional
    public User update(UUID userId, Command command) {
        User current = users.findById(userId).orElseThrow(UserNotFoundException::new);
        return users.save(current.withName(
                command.firstName().trim(), command.lastName().trim()));
    }

    public record Command(String firstName, String lastName) {
    }
}
