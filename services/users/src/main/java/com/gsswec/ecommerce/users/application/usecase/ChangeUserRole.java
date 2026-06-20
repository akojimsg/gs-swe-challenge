package com.gsswec.ecommerce.users.application.usecase;

import com.gsswec.ecommerce.shared.constants.StreamNames;
import com.gsswec.ecommerce.shared.events.base.BaseEvent;
import com.gsswec.ecommerce.shared.events.user.UserRoleChangedEvent;
import com.gsswec.ecommerce.users.application.port.out.EventPublisher;
import com.gsswec.ecommerce.users.application.port.out.UserRepository;
import com.gsswec.ecommerce.users.domain.exception.UserNotFoundException;
import com.gsswec.ecommerce.users.domain.model.Role;
import com.gsswec.ecommerce.users.domain.model.User;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangeUserRole {

    private final UserRepository users;
    private final EventPublisher events;
    private final Clock clock;

    public ChangeUserRole(UserRepository users, EventPublisher events, Clock clock) {
        this.users = users;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public User change(UUID userId, Role newRole) {
        User current = users.findById(userId).orElseThrow(UserNotFoundException::new);
        if (current.role() == newRole) {
            return current;
        }

        Role oldRole = current.role();
        User updated = users.save(current.withRole(newRole));

        events.publish(StreamNames.USER_ROLE_CHANGED, new UserRoleChangedEvent(
                new BaseEvent(UUID.randomUUID(), StreamNames.USER_ROLE_CHANGED, "1.0",
                        Instant.now(clock), null, "users"),
                updated.id(),
                oldRole.name(),
                newRole.name()));

        return updated;
    }
}
