package com.gsswec.ecommerce.orders.infrastructure;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gsswec.ecommerce.orders.infrastructure.messaging.RedisStreamEventPublisher;
import com.gsswec.ecommerce.shared.constants.StreamNames;
import com.gsswec.ecommerce.shared.events.base.BaseEvent;
import com.gsswec.ecommerce.shared.events.order.OrderPlacedEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// Proves the dual-write race fix: inside a transaction the event is NOT written to the
// stream until commit, and is NOT written at all on rollback. With no active
// transaction it publishes immediately. This is the invariant that prevents a consumer
// observing order.placed before the order row is durably committed.
class RedisStreamEventPublisherTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final StreamOperations<String, Object, Object> streamOps = mock(StreamOperations.class);
    private final RedisStreamEventPublisher publisher =
            new RedisStreamEventPublisher(redis, new ObjectMapper().registerModule(new JavaTimeModule()));

    private OrderPlacedEvent event() {
        return new OrderPlacedEvent(
                new BaseEvent(UUID.randomUUID(), StreamNames.ORDER_PLACED, "1.0", Instant.EPOCH, null, "orders"),
                UUID.randomUUID(), UUID.randomUUID(), List.of(), new BigDecimal("10.00"));
    }

    @AfterEach
    void clearTx() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void withinTransaction_doesNotWriteUntilCommit() {
        when(redis.opsForStream()).thenReturn(streamOps);
        TransactionSynchronizationManager.initSynchronization();

        publisher.publish(StreamNames.ORDER_PLACED, event());

        // Nothing written yet — the write is deferred to afterCommit. opsForStream()
        // is the gateway to every write, so its non-invocation proves no write happened.
        verify(redis, never()).opsForStream();

        // Simulate commit: run the registered afterCommit callbacks.
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());

        verify(redis).opsForStream();
    }

    @Test
    void rolledBackTransaction_writesNothing() {
        TransactionSynchronizationManager.initSynchronization();

        publisher.publish(StreamNames.ORDER_PLACED, event());

        // afterCommit is never invoked on rollback -> no stream write at all.
        verify(redis, never()).opsForStream();
    }

    @Test
    void noActiveTransaction_publishesImmediately() {
        when(redis.opsForStream()).thenReturn(streamOps);

        publisher.publish(StreamNames.ORDER_PLACED, event());

        verify(redis).opsForStream();
    }
}
