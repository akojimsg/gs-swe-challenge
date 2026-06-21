package com.gsswec.ecommerce.orders.application.usecase;

import com.gsswec.ecommerce.orders.application.port.out.EventPublisher;
import com.gsswec.ecommerce.orders.application.port.out.OrderRepository;
import com.gsswec.ecommerce.orders.application.port.out.StockClient;
import com.gsswec.ecommerce.orders.domain.model.Order;
import com.gsswec.ecommerce.orders.domain.model.OrderItem;
import com.gsswec.ecommerce.orders.domain.model.OrderStatus;
import com.gsswec.ecommerce.shared.constants.StreamNames;
import com.gsswec.ecommerce.shared.dto.OrderItemSnapshot;
import com.gsswec.ecommerce.shared.events.base.BaseEvent;
import com.gsswec.ecommerce.shared.events.order.OrderFailedEvent;
import com.gsswec.ecommerce.shared.events.order.OrderPaidEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// The saga's second half (Orders side). Consumes payment.succeeded / payment.failed
// and drives the order to its terminal state:
//   succeeded -> PAID, emit order.paid
//   failed    -> FAILED, emit order.failed, and COMPENSATE by releasing the stock
//                that PlaceOrder reserved up front (Option 1: Orders owns the release).
// Every step is guarded by the order's state machine and is safe to replay.
@Service
public class HandlePaymentOutcome {

    private static final Logger log = LoggerFactory.getLogger(HandlePaymentOutcome.class);

    private final OrderRepository orders;
    private final StockClient stock;
    private final EventPublisher events;
    private final Clock clock;

    public HandlePaymentOutcome(OrderRepository orders, StockClient stock,
            EventPublisher events, Clock clock) {
        this.orders = orders;
        this.stock = stock;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public void onPaymentSucceeded(UUID orderId, UUID paymentId) {
        Order order = orders.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("payment.succeeded for unknown order {} — ignoring", orderId);
            return;
        }
        // Replay / out-of-order guard: only AWAITING_PAYMENT can move to PAID.
        if (order.status() != OrderStatus.AWAITING_PAYMENT) {
            log.debug("Order {} not awaiting payment (status={}) — skipping PAID", orderId, order.status());
            return;
        }

        Order paid = orders.save(order.transitionTo(OrderStatus.PAID));
        events.publish(StreamNames.ORDER_PAID, new OrderPaidEvent(
                base(StreamNames.ORDER_PAID), paid.id(), paid.userId(), paid.total(), paymentId));
        log.info("Order {} marked PAID (payment {})", orderId, paymentId);
    }

    @Transactional
    public void onPaymentFailed(UUID orderId, String reason) {
        Order order = orders.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("payment.failed for unknown order {} — ignoring", orderId);
            return;
        }
        if (order.status() != OrderStatus.AWAITING_PAYMENT) {
            log.debug("Order {} not awaiting payment (status={}) — skipping FAILED", orderId, order.status());
            return;
        }

        Order failed = orders.save(order.transitionTo(OrderStatus.FAILED));

        // Compensation: give back the stock PlaceOrder reserved. Release is idempotent
        // on the Products side only insofar as we call it once — the state-machine guard
        // above ensures a redelivered payment.failed won't double-release.
        releaseStock(failed);

        events.publish(StreamNames.ORDER_FAILED, new OrderFailedEvent(
                base(StreamNames.ORDER_FAILED), failed.id(), failed.userId(),
                reason == null ? "PAYMENT_FAILED" : reason, toSnapshots(failed.items())));
        log.info("Order {} marked FAILED ({}); stock released", orderId, reason);
    }

    private void releaseStock(Order order) {
        List<StockClient.ReserveLine> lines = order.items().stream()
                .map(i -> new StockClient.ReserveLine(i.productId().toString(), i.sku(), i.quantity()))
                .toList();
        stock.release(order.id().toString(), lines);
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
