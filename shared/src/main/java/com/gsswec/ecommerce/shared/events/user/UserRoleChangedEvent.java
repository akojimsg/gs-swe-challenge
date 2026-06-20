package com.gsswec.ecommerce.shared.events.user;

import com.gsswec.ecommerce.shared.events.base.BaseEvent;
import com.gsswec.ecommerce.shared.events.base.DomainEvent;
import java.util.UUID;

public record UserRoleChangedEvent(
        BaseEvent base,
        UUID userId,
        String oldRole,
        String newRole) implements DomainEvent {
}
