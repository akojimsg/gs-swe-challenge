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
// eventId before doing any work. ProcessPayment is itself idempotent on orderId as
// a second backstop. The record is ack'd by the container only when this returns
// normally; a thrown exception leaves it pending for redelivery.
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

        if (!processedEvents.markIfNew(eventId)) {
            log.debug("Skipping already-processed event {}", eventId);
            return;
        }

        try {
            OrderPlacedEvent event = objectMapper.readValue(body.get("payload"), OrderPlacedEvent.class);
            processPayment.process(event.orderId(), event.userId(), event.total());
            log.info("Processed payment for order {} (event {})", event.orderId(), eventId);
        } catch (Exception e) {
            // Non-recoverable parse/processing error: log and swallow so the record is
            // ack'd rather than redelivered forever. A production system would route to a
            // dead-letter stream here; the payment.failed compensation path is exercised
            // by the saga itself, not by malformed inbound events.
            log.error("Failed to process order.placed event {} — dropping", eventId, e);
        }
    }
}
