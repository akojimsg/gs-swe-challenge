package com.gsswec.ecommerce.users.application.port.out;

import com.gsswec.ecommerce.shared.events.base.DomainEvent;

public interface EventPublisher {

    void publish(String stream, DomainEvent event);
}
