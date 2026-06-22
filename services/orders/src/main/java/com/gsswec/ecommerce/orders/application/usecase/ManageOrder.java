package com.gsswec.ecommerce.orders.application.usecase;

import com.gsswec.ecommerce.orders.application.port.out.EventPublisher;
import com.gsswec.ecommerce.orders.application.port.out.OrderRepository;
import com.gsswec.ecommerce.orders.domain.exception.OrderNotFoundException;
import com.gsswec.ecommerce.orders.domain.model.Order;
import com.gsswec.ecommerce.orders.domain.model.OrderItem;
import com.gsswec.ecommerce.orders.domain.model.OrderStatus;
import com.gsswec.ecommerce.shared.constants.StreamNames;
import com.gsswec.ecommerce.shared.dto.OrderItemSnapshot;
import com.gsswec.ecommerce.shared.events.base.BaseEvent;
import com.gsswec.ecommerce.shared.events.order.OrderCancelledEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManageOrder {

    private final OrderRepository orders;
    private final EventPublisher events;
    private final Clock clock;

    public ManageOrder(OrderRepository orders, EventPublisher events, Clock clock) {
        this.orders = orders;
        this.events = events;
        this.clock = clock;
    }

    // Buyer-initiated cancellation. Only the owner may cancel, and only while PENDING
    // (the state machine rejects any other source state). Publishes order.cancelled.
    @Transactional
    public Order cancel(UUID orderId, UUID callerId) {
        Order order = orders.findById(orderId).orElseThrow(OrderNotFoundException::new);
        if (!order.ownedBy(callerId)) {
            throw new AccessDeniedException("Not the owner of this order");
        }
        Order cancelled = orders.save(order.transitionTo(OrderStatus.CANCELLED));
        events.publish(StreamNames.ORDER_CANCELLED, new OrderCancelledEvent(
                base(StreamNames.ORDER_CANCELLED), cancelled.id(), cancelled.userId(),
                "Cancelled by buyer", toSnapshots(cancelled.items())));
        return cancelled;
    }

    // Admin status change. The domain state machine is the single source of truth for
    // which transitions are legal, so an illegal target is rejected the same way
    // everywhere.
    @Transactional
    public Order changeStatus(UUID orderId, OrderStatus target) {
        Order order = orders.findById(orderId).orElseThrow(OrderNotFoundException::new);
        return orders.save(order.transitionTo(target));
    }

    private static List<OrderItemSnapshot> toSnapshots(List<OrderItem> items) {
        return items.stream()
                .map(i -> new OrderItemSnapshot(i.productId(), i.sku(), i.name(), i.quantity(), i.unitPrice()))
                .toList();
    }

    private BaseEvent base(String eventType) {
        return new BaseEvent(UUID.randomUUID(), eventType, "1.0", Instant.now(clock), null, "orders");
    }
}
