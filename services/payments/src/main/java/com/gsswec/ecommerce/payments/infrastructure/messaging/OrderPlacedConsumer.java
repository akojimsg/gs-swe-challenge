package com.gsswec.ecommerce.payments.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gsswec.ecommerce.payments.application.port.out.ProcessedEventStore;
import com.gsswec.ecommerce.payments.application.usecase.ProcessPayment;
import com.gsswec.ecommerce.shared.events.order.OrderPlacedEvent;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

// Saga entry point: consumes order.placed via a Redis Streams consumer group.
// At-least-once delivery means redeliveries happen, so we dedupe on the event's
// eventId, marking it processed only AFTER a successful charge — a transient failure
// is retried on redelivery rather than permanently suppressed. ProcessPayment is
// itself idempotent on orderId as a second backstop. A malformed payload
// (non-retryable) is marked so it is not redelivered forever.
@Component
public class OrderPlacedConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private static final Logger log = LoggerFactory.getLogger(OrderPlacedConsumer.class);

    private final ProcessPayment processPayment;
    private final ProcessedEventStore processedEvents;
    private final ObjectMapper objectMapper;

    public OrderPlacedConsumer(ProcessPayment processPayment, ProcessedEventStore processedEvents,
            ObjectMapper objectMapper) {
        this.processPayment = processPayment;
        this.processedEvents = processedEvents;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(MapRecord<String, String, String> record) {
        Map<String, String> body = record.getValue();
        UUID eventId = UUID.fromString(body.get("eventId"));

        if (processedEvents.isProcessed(eventId)) {
            log.debug("Skipping already-processed event {}", eventId);
            return;
        }

        OrderPlacedEvent event;
        try {
            event = objectMapper.readValue(body.get("payload"), OrderPlacedEvent.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // Malformed payload is non-retryable — mark so it is not redelivered forever
            // (a production system would route to a dead-letter stream here).
            log.error("Failed to parse order.placed event {} — dropping", eventId, e);
            processedEvents.markProcessed(eventId);
            return;
        }

        // A processing failure (DB/charge) propagates so the record stays pending and is
        // retried on redelivery; ProcessPayment is idempotent on orderId. Mark only on
        // success.
        processPayment.process(event.orderId(), event.userId(), event.total());
        processedEvents.markProcessed(eventId);
        log.info("Processed payment for order {} (event {})", event.orderId(), eventId);
    }
}
