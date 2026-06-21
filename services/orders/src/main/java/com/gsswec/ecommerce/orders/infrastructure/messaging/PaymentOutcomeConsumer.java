package com.gsswec.ecommerce.orders.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gsswec.ecommerce.orders.application.port.out.ProcessedEventStore;
import com.gsswec.ecommerce.orders.application.usecase.HandlePaymentOutcome;
import com.gsswec.ecommerce.shared.constants.StreamNames;
import com.gsswec.ecommerce.shared.events.payment.PaymentFailedEvent;
import com.gsswec.ecommerce.shared.events.payment.PaymentSucceededEvent;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

// Saga consumer (Orders side): listens to both payment.succeeded and payment.failed.
// Routes by the stream key the record arrived on. Dedupes on eventId (at-least-once),
// then delegates to HandlePaymentOutcome, which is itself state-machine guarded.
// Throwing on a transient failure leaves the record pending for redelivery; an
// unrecoverable parse error is logged and swallowed so the record is ack'd.
@Component
public class PaymentOutcomeConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private static final Logger log = LoggerFactory.getLogger(PaymentOutcomeConsumer.class);

    private final HandlePaymentOutcome handler;
    private final ProcessedEventStore processedEvents;
    private final ObjectMapper objectMapper;

    public PaymentOutcomeConsumer(HandlePaymentOutcome handler, ProcessedEventStore processedEvents,
            ObjectMapper objectMapper) {
        this.handler = handler;
        this.processedEvents = processedEvents;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(MapRecord<String, String, String> record) {
        String stream = record.getStream();
        Map<String, String> body = record.getValue();
        UUID eventId = UUID.fromString(body.get("eventId"));

        if (!processedEvents.markIfNew(eventId)) {
            log.debug("Skipping already-processed event {} on {}", eventId, stream);
            return;
        }

        try {
            if (StreamNames.PAYMENT_SUCCEEDED.equals(stream)) {
                PaymentSucceededEvent e = objectMapper.readValue(
                        body.get("payload"), PaymentSucceededEvent.class);
                handler.onPaymentSucceeded(e.orderId(), e.paymentId());
            } else if (StreamNames.PAYMENT_FAILED.equals(stream)) {
                PaymentFailedEvent e = objectMapper.readValue(
                        body.get("payload"), PaymentFailedEvent.class);
                handler.onPaymentFailed(e.orderId(), e.reason());
            } else {
                log.warn("Unexpected stream {} for event {}", stream, eventId);
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Failed to parse payment event {} on {} — dropping", eventId, stream, e);
        }
    }
}
