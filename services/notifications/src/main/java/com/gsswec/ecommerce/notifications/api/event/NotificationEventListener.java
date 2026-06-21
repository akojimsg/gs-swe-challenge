package com.gsswec.ecommerce.notifications.api.event;

import com.gsswec.ecommerce.notifications.application.usecase.SendNotification;
import com.gsswec.ecommerce.notifications.domain.model.EmailTemplate;
import com.gsswec.ecommerce.shared.constants.StreamNames;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

// Single listener wired to multiple streams by NotificationStreamConsumerConfig.
// Each dispatch method selects the correct template and extracts the recipient
// from event fields; unknown events are logged and skipped.
@Component
public class NotificationEventListener implements StreamListener<String, MapRecord<String, String, String>> {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final SendNotification sendNotification;

    public NotificationEventListener(SendNotification sendNotification) {
        this.sendNotification = sendNotification;
    }

    @Override
    public void onMessage(MapRecord<String, String, String> record) {
        Map<String, String> fields = record.getValue();
        String eventType = record.getStream();
        String rawEventId = fields.get("eventId");

        if (rawEventId == null) {
            log.warn("Missing eventId on stream {}, record {}", eventType, record.getId());
            return;
        }

        UUID eventId;
        try {
            eventId = UUID.fromString(rawEventId);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid eventId '{}' on stream {}", rawEventId, eventType);
            return;
        }

        switch (eventType) {
            case StreamNames.USER_REGISTERED -> dispatchUserRegistered(eventId, fields);
            case StreamNames.USER_ROLE_CHANGED -> dispatchRoleChanged(eventId, fields);
            case StreamNames.PRODUCT_CREATED -> dispatchProductCreated(eventId, fields);
            case StreamNames.PRODUCT_STOCK_LOW -> dispatchStockLow(eventId, fields);
            case StreamNames.PRODUCT_IMPORTED -> dispatchProductImported(eventId, fields);
            case StreamNames.ORDER_PAID -> dispatchOrderPaid(eventId, fields);
            case StreamNames.ORDER_FAILED -> dispatchOrderFailed(eventId, fields);
            case StreamNames.PAYMENT_SUCCEEDED -> dispatchPaymentSucceeded(eventId, fields);
            case StreamNames.PAYMENT_FAILED -> dispatchPaymentFailed(eventId, fields);
            default -> log.debug("No notification handler for event type '{}'", eventType);
        }
    }

    private void dispatchUserRegistered(UUID eventId, Map<String, String> fields) {
        String recipient = fields.getOrDefault("email", "unknown@gsswec.com");
        String name = fields.getOrDefault("name", "Customer");
        sendNotification.execute(eventId, StreamNames.USER_REGISTERED, recipient,
                EmailTemplate.WELCOME, "Welcome to United Deals, " + name + "!");
    }

    private void dispatchRoleChanged(UUID eventId, Map<String, String> fields) {
        String recipient = fields.getOrDefault("email", "unknown@gsswec.com");
        String newRole = fields.getOrDefault("newRole", "USER");
        sendNotification.execute(eventId, StreamNames.USER_ROLE_CHANGED, recipient,
                EmailTemplate.ROLE_CHANGED, "Your account role has been updated to: " + newRole);
    }

    private void dispatchProductCreated(UUID eventId, Map<String, String> fields) {
        String adminEmail = fields.getOrDefault("adminEmail", "admin@gsswec.com");
        String productName = fields.getOrDefault("name", "a new product");
        sendNotification.execute(eventId, StreamNames.PRODUCT_CREATED, adminEmail,
                EmailTemplate.PRODUCT_CREATED, "Product listed: " + productName);
    }

    private void dispatchStockLow(UUID eventId, Map<String, String> fields) {
        String adminEmail = fields.getOrDefault("adminEmail", "admin@gsswec.com");
        String sku = fields.getOrDefault("sku", "unknown");
        String qty = fields.getOrDefault("stockQty", "?");
        sendNotification.execute(eventId, StreamNames.PRODUCT_STOCK_LOW, adminEmail,
                EmailTemplate.STOCK_LOW, "Low stock alert — SKU " + sku + " has " + qty + " units remaining.");
    }

    private void dispatchProductImported(UUID eventId, Map<String, String> fields) {
        String adminEmail = fields.getOrDefault("adminEmail", "admin@gsswec.com");
        String count = fields.getOrDefault("importedCount", "?");
        sendNotification.execute(eventId, StreamNames.PRODUCT_IMPORTED, adminEmail,
                EmailTemplate.PRODUCT_IMPORTED, "CSV import complete — " + count + " products imported.");
    }

    private void dispatchOrderPaid(UUID eventId, Map<String, String> fields) {
        String recipient = fields.getOrDefault("userEmail", "unknown@gsswec.com");
        String orderId = fields.getOrDefault("orderId", "");
        sendNotification.execute(eventId, StreamNames.ORDER_PAID, recipient,
                EmailTemplate.ORDER_PAID, "Your order " + orderId + " is confirmed — payment received.");
    }

    private void dispatchOrderFailed(UUID eventId, Map<String, String> fields) {
        String recipient = fields.getOrDefault("userEmail", "unknown@gsswec.com");
        String orderId = fields.getOrDefault("orderId", "");
        sendNotification.execute(eventId, StreamNames.ORDER_FAILED, recipient,
                EmailTemplate.ORDER_FAILED, "Order " + orderId + " could not be completed. Please try again.");
    }

    private void dispatchPaymentSucceeded(UUID eventId, Map<String, String> fields) {
        String recipient = fields.getOrDefault("userEmail", "unknown@gsswec.com");
        String amount = fields.getOrDefault("amount", "");
        sendNotification.execute(eventId, StreamNames.PAYMENT_SUCCEEDED, recipient,
                EmailTemplate.PAYMENT_SUCCEEDED, "Payment of $" + amount + " was successful.");
    }

    private void dispatchPaymentFailed(UUID eventId, Map<String, String> fields) {
        String recipient = fields.getOrDefault("userEmail", "unknown@gsswec.com");
        String reason = fields.getOrDefault("reason", "Unknown reason");
        sendNotification.execute(eventId, StreamNames.PAYMENT_FAILED, recipient,
                EmailTemplate.PAYMENT_FAILED, "Payment failed: " + reason + ". Please update your payment method.");
    }
}
