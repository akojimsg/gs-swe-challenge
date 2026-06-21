package com.gsswec.ecommerce.orders.application.usecase;

import com.gsswec.ecommerce.orders.application.port.out.EventPublisher;
import com.gsswec.ecommerce.orders.application.port.out.IdempotencyStore;
import com.gsswec.ecommerce.orders.application.port.out.OrderRepository;
import com.gsswec.ecommerce.orders.application.port.out.StockClient;
import com.gsswec.ecommerce.orders.domain.exception.InsufficientStockException;
import com.gsswec.ecommerce.orders.domain.model.Order;
import com.gsswec.ecommerce.orders.domain.model.OrderItem;
import com.gsswec.ecommerce.orders.domain.model.OrderStatus;
import com.gsswec.ecommerce.shared.constants.StreamNames;
import com.gsswec.ecommerce.shared.dto.OrderItemSnapshot;
import com.gsswec.ecommerce.shared.events.base.BaseEvent;
import com.gsswec.ecommerce.shared.events.order.OrderPlacedEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceOrder {

    private final OrderRepository orders;
    private final StockClient stock;
    private final IdempotencyStore idempotency;
    private final EventPublisher events;
    private final Clock clock;

    public PlaceOrder(OrderRepository orders, StockClient stock, IdempotencyStore idempotency,
            EventPublisher events, Clock clock) {
        this.orders = orders;
        this.stock = stock;
        this.idempotency = idempotency;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public Order place(Command command) {
        // 1. Idempotency: a repeated key returns the original order, no new work.
        Optional<UUID> existing = idempotency.get(command.idempotencyKey());
        if (existing.isPresent()) {
            return orders.findById(existing.get())
                    .or(() -> orders.findByIdempotencyKey(command.idempotencyKey()))
                    .orElseThrow();
        }
        // Defence in depth against a race that slipped past the Redis check:
        // the orders.idempotency_key UNIQUE constraint is the hard backstop.
        Optional<Order> persisted = orders.findByIdempotencyKey(command.idempotencyKey());
        if (persisted.isPresent()) {
            return persisted.get();
        }

        // 2. Reserve stock synchronously (gRPC). Throws on any shortfall — atomic,
        //    all-or-nothing on the Products side.
        List<StockClient.ReserveLine> reserveLines = command.lines().stream()
                .map(l -> new StockClient.ReserveLine(l.productId().toString(), null, l.quantity()))
                .toList();
        String provisionalOrderId = UUID.randomUUID().toString();
        StockClient.ReserveResult reservation = stock.reserve(provisionalOrderId, reserveLines);
        if (!reservation.reserved()) {
            throw new InsufficientStockException(reservation.shortfalls().stream()
                    .map(s -> new InsufficientStockException.Shortfall(
                            s.sku(), s.requested(), s.available(), s.reason()))
                    .toList());
        }

        // 3. Build the order from the authoritative reserved-line details (Products
        //    is the price authority; the client never supplies price).
        List<OrderItem> items = reservation.reservedLines().stream()
                .map(r -> OrderItem.of(UUID.fromString(r.productId()), r.sku(), r.name(),
                        r.quantity(), r.unitPrice()))
                .toList();
        Order placed = orders.save(Order.place(command.userId(), command.idempotencyKey(), items));
        idempotency.put(command.idempotencyKey(), placed.id());

        // 4. Publish order.placed — kicks off the saga — then move to AWAITING_PAYMENT.
        events.publish(StreamNames.ORDER_PLACED, new OrderPlacedEvent(
                baseEvent(StreamNames.ORDER_PLACED),
                placed.id(), placed.userId(), toSnapshots(placed.items()), placed.total()));

        return orders.save(placed.transitionTo(OrderStatus.AWAITING_PAYMENT));
    }

    private static List<OrderItemSnapshot> toSnapshots(List<OrderItem> items) {
        return items.stream()
                .map(i -> new OrderItemSnapshot(i.productId(), i.sku(), i.name(), i.quantity(), i.unitPrice()))
                .toList();
    }

    private BaseEvent baseEvent(String eventType) {
        return new BaseEvent(UUID.randomUUID(), eventType, "1.0", Instant.now(clock), null, "orders");
    }

    public record Command(UUID userId, String idempotencyKey, List<Line> lines) {
    }

    public record Line(UUID productId, int quantity) {
    }
}
