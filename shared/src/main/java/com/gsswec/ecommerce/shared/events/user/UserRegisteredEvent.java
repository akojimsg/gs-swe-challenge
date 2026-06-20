package com.gsswec.ecommerce.shared.events.user;

import com.gsswec.ecommerce.shared.events.base.BaseEvent;
import com.gsswec.ecommerce.shared.events.base.DomainEvent;
import java.util.UUID;

public record UserRegisteredEvent(
        BaseEvent base,
        UUID userId,
        String email,
        String role) implements DomainEvent {
}
