package com.gsswec.ecommerce.orders.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gsswec.ecommerce.orders.application.port.out.EventPublisher;
import com.gsswec.ecommerce.orders.application.port.out.IdempotencyStore;
import com.gsswec.ecommerce.orders.application.port.out.OrderRepository;
import com.gsswec.ecommerce.orders.application.port.out.StockClient;
import com.gsswec.ecommerce.orders.application.usecase.PlaceOrder;
import com.gsswec.ecommerce.orders.domain.exception.InsufficientStockException;
import com.gsswec.ecommerce.orders.domain.model.Order;
import com.gsswec.ecommerce.orders.domain.model.OrderStatus;
import com.gsswec.ecommerce.shared.constants.StreamNames;
import com.gsswec.ecommerce.shared.events.base.DomainEvent;
import com.gsswec.ecommerce.shared.events.order.OrderPlacedEvent;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PlaceOrderTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-20T00:00:00Z"), ZoneOffset.UTC);
    private final UUID userId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private OrderRepository orders;
    private StockClient stock;
    private IdempotencyStore idempotency;
    private EventPublisher events;
    private PlaceOrder placeOrder;

    @BeforeEach
    void setUp() {
        orders = org.mockito.Mockito.mock(OrderRepository.class);
        stock = org.mockito.Mockito.mock(StockClient.class);
        idempotency = org.mockito.Mockito.mock(IdempotencyStore.class);
        events = org.mockito.Mockito.mock(EventPublisher.class);
        placeOrder = new PlaceOrder(orders, stock, idempotency, events, clock);
        when(orders.save(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            return o.id() == null
                    ? new Order(UUID.randomUUID(), o.userId(), o.status(), o.total(),
                        o.idempotencyKey(), o.items(), Instant.now(clock), Instant.now(clock))
                    : o;
        });
    }

    private PlaceOrder.Command command(String key) {
        return new PlaceOrder.Command(userId, key, List.of(new PlaceOrder.Line(productId, 2)));
    }

    private void stubReserveSuccess() {
        when(stock.reserve(anyString(), any())).thenReturn(new StockClient.ReserveResult(
                true,
                List.of(new StockClient.ReservedLine(productId.toString(), "SKU-1", "Widget",
                        new BigDecimal("9.99"), 2)),
                List.of()));
    }

    @Test
    void placesOrderReservesStockAndPublishesEvent() {
        when(idempotency.get("key-1")).thenReturn(Optional.empty());
        when(orders.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        stubReserveSuccess();

        Order result = placeOrder.place(command("key-1"));

        assertThat(result.status()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
        assertThat(result.total()).isEqualByComparingTo("19.98");
        verify(idempotency).put(org.mockito.ArgumentMatchers.eq("key-1"), any());

        ArgumentCaptor<DomainEvent> ev = ArgumentCaptor.forClass(DomainEvent.class);
        verify(events).publish(org.mockito.ArgumentMatchers.eq(StreamNames.ORDER_PLACED), ev.capture());
        OrderPlacedEvent e = (OrderPlacedEvent) ev.getValue();
        assertThat(e.items()).hasSize(1);
        assertThat(e.items().get(0).unitPrice()).isEqualByComparingTo("9.99");
        assertThat(e.total()).isEqualByComparingTo("19.98");
    }

    @Test
    void idempotentReplayReturnsExistingOrderWithoutReservingAgain() {
        UUID existingId = UUID.randomUUID();
        Order existing = new Order(existingId, userId, OrderStatus.AWAITING_PAYMENT,
                new BigDecimal("19.98"), "key-1", List.of(), Instant.now(clock), Instant.now(clock));
        when(idempotency.get("key-1")).thenReturn(Optional.of(existingId));
        when(orders.findById(existingId)).thenReturn(Optional.of(existing));

        Order result = placeOrder.place(command("key-1"));

        assertThat(result.id()).isEqualTo(existingId);
        verify(stock, never()).reserve(anyString(), any());
        verify(events, never()).publish(anyString(), any());
    }

    @Test
    void insufficientStockThrowsAndPublishesNothing() {
        when(idempotency.get("key-1")).thenReturn(Optional.empty());
        when(orders.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(stock.reserve(anyString(), any())).thenReturn(new StockClient.ReserveResult(
                false, List.of(),
                List.of(new StockClient.Shortfall("SKU-1", 2, 0, "INSUFFICIENT_STOCK"))));

        assertThatThrownBy(() -> placeOrder.place(command("key-1")))
                .isInstanceOf(InsufficientStockException.class);

        verify(orders, never()).save(any());
        verify(events, never()).publish(anyString(), any());
    }
}
