package com.gsswec.ecommerce.users.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gsswec.ecommerce.shared.constants.StreamNames;
import com.gsswec.ecommerce.shared.events.base.DomainEvent;
import com.gsswec.ecommerce.shared.events.user.UserRoleChangedEvent;
import com.gsswec.ecommerce.users.application.port.out.EventPublisher;
import com.gsswec.ecommerce.users.application.port.out.UserRepository;
import com.gsswec.ecommerce.users.application.usecase.ChangeUserRole;
import com.gsswec.ecommerce.users.domain.exception.UserNotFoundException;
import com.gsswec.ecommerce.users.domain.model.Role;
import com.gsswec.ecommerce.users.domain.model.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ChangeUserRoleTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-20T00:00:00Z"), ZoneOffset.UTC);
    private final UUID userId = UUID.randomUUID();
    private UserRepository users;
    private EventPublisher events;
    private ChangeUserRole changeUserRole;

    @BeforeEach
    void setUp() {
        users = org.mockito.Mockito.mock(UserRepository.class);
        events = org.mockito.Mockito.mock(EventPublisher.class);
        changeUserRole = new ChangeUserRole(users, events, clock);
    }

    private User buyer() {
        return new User(userId, "u@test.com", "h", "Test", "User",
                Role.BUYER, true, Instant.now(clock), Instant.now(clock));
    }

    @Test
    void promotesAndPublishesRoleChangedEvent() {
        when(users.findById(userId)).thenReturn(Optional.of(buyer()));
        when(users.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = changeUserRole.change(userId, Role.ADMIN);

        assertThat(result.role()).isEqualTo(Role.ADMIN);
        ArgumentCaptor<DomainEvent> event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(events).publish(org.mockito.ArgumentMatchers.eq(StreamNames.USER_ROLE_CHANGED), event.capture());
        UserRoleChangedEvent e = (UserRoleChangedEvent) event.getValue();
        assertThat(e.oldRole()).isEqualTo("BUYER");
        assertThat(e.newRole()).isEqualTo("ADMIN");
        assertThat(e.userId()).isEqualTo(userId);
    }

    @Test
    void noOpAndNoEventWhenRoleUnchanged() {
        when(users.findById(userId)).thenReturn(Optional.of(buyer()));

        User result = changeUserRole.change(userId, Role.BUYER);

        assertThat(result.role()).isEqualTo(Role.BUYER);
        verify(users, never()).save(any());
        verify(events, never()).publish(anyString(), any());
    }

    @Test
    void rejectsUnknownUser() {
        when(users.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> changeUserRole.change(userId, Role.ADMIN))
                .isInstanceOf(UserNotFoundException.class);
        verify(events, never()).publish(anyString(), any());
    }
}
