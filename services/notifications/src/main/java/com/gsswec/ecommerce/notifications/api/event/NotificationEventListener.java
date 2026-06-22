package com.gsswec.ecommerce.notifications.api.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gsswec.ecommerce.notifications.application.port.out.UserDirectory;
import com.gsswec.ecommerce.notifications.application.usecase.SendNotification;
import com.gsswec.ecommerce.notifications.domain.model.EmailTemplate;
import com.gsswec.ecommerce.notifications.infrastructure.config.NotificationsProperties;
import com.gsswec.ecommerce.shared.constants.StreamNames;
import com.gsswec.ecommerce.shared.events.order.OrderFailedEvent;
import com.gsswec.ecommerce.shared.events.order.OrderPaidEvent;
import com.gsswec.ecommerce.shared.events.payment.PaymentFailedEvent;
import com.gsswec.ecommerce.shared.events.payment.PaymentSucceededEvent;
import com.gsswec.ecommerce.shared.events.product.ProductCreatedEvent;
import com.gsswec.ecommerce.shared.events.product.ProductImportedEvent;
import com.gsswec.ecommerce.shared.events.product.ProductStockLowEvent;
import com.gsswec.ecommerce.shared.events.user.UserRegisteredEvent;
import com.gsswec.ecommerce.shared.events.user.UserRoleChangedEvent;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener implements StreamListener<String, MapRecord<String, String, String>> {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final SendNotification sendNotification;
    private final UserDirectory userDirectory;
    private final ObjectMapper objectMapper;
    private final String adminEmail;

    public NotificationEventListener(SendNotification sendNotification, UserDirectory userDirectory,
            ObjectMapper objectMapper, NotificationsProperties properties) {
        this.sendNotification = sendNotification;
        this.userDirectory = userDirectory;
        this.objectMapper = objectMapper;
        this.adminEmail = properties.adminEmail();
    }

    @Override
    public void onMessage(MapRecord<String, String, String> record) {
        String eventType = record.getStream();
        Map<String, String> fields = record.getValue();
        String payload = fields.get("payload");
        if (payload == null) {
            log.warn("Missing payload on stream {}, record {}", eventType, record.getId());
            return;
        }

        try {
            switch (eventType) {
                case StreamNames.USER_REGISTERED -> onUserRegistered(read(payload, UserRegisteredEvent.class));
                case StreamNames.USER_ROLE_CHANGED -> onRoleChanged(read(payload, UserRoleChangedEvent.class));
                case StreamNames.PRODUCT_CREATED -> onProductCreated(read(payload, ProductCreatedEvent.class));
                case StreamNames.PRODUCT_STOCK_LOW -> onStockLow(read(payload, ProductStockLowEvent.class));
                case StreamNames.PRODUCT_IMPORTED -> onProductImported(read(payload, ProductImportedEvent.class));
                case StreamNames.ORDER_PAID -> onOrderPaid(read(payload, OrderPaidEvent.class));
                case StreamNames.ORDER_FAILED -> onOrderFailed(read(payload, OrderFailedEvent.class));
                case StreamNames.PAYMENT_SUCCEEDED -> onPaymentSucceeded(read(payload, PaymentSucceededEvent.class));
                case StreamNames.PAYMENT_FAILED -> onPaymentFailed(read(payload, PaymentFailedEvent.class));
                default -> log.debug("No notification handler for event type '{}'", eventType);
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Failed to parse {} payload, record {} — dropping", eventType, record.getId(), e);
        }
    }

    private <T> T read(String payload, Class<T> type) throws com.fasterxml.jackson.core.JsonProcessingException {
        return objectMapper.readValue(payload, type);
    }

    private void onUserRegistered(UserRegisteredEvent e) {
        sendNotification.execute(e.base().eventId(), StreamNames.USER_REGISTERED, e.email(),
                EmailTemplate.WELCOME, "Welcome to United Deals!");
    }

    private void onRoleChanged(UserRoleChangedEvent e) {
        dispatchToUser(e.base().eventId(), StreamNames.USER_ROLE_CHANGED, e.userId(),
                EmailTemplate.ROLE_CHANGED, "Your account role has been updated to: " + e.newRole());
    }

    private void onProductCreated(ProductCreatedEvent e) {
        sendNotification.execute(e.base().eventId(), StreamNames.PRODUCT_CREATED, adminEmail,
                EmailTemplate.PRODUCT_CREATED, "Product listed: " + e.name());
    }

    private void onStockLow(ProductStockLowEvent e) {
        sendNotification.execute(e.base().eventId(), StreamNames.PRODUCT_STOCK_LOW, adminEmail,
                EmailTemplate.STOCK_LOW,
                "Low stock alert — SKU " + e.sku() + " has " + e.currentStock() + " units remaining.");
    }

    private void onProductImported(ProductImportedEvent e) {
        sendNotification.execute(e.base().eventId(), StreamNames.PRODUCT_IMPORTED, adminEmail,
                EmailTemplate.PRODUCT_IMPORTED, "CSV import complete — " + e.imported() + " products imported.");
    }

    private void onOrderPaid(OrderPaidEvent e) {
        dispatchToUser(e.base().eventId(), StreamNames.ORDER_PAID, e.userId(),
                EmailTemplate.ORDER_PAID, "Your order " + e.orderId() + " is confirmed — payment received.");
    }

    private void onOrderFailed(OrderFailedEvent e) {
        dispatchToUser(e.base().eventId(), StreamNames.ORDER_FAILED, e.userId(),
                EmailTemplate.ORDER_FAILED, "Order " + e.orderId() + " could not be completed. Please try again.");
    }

    private void onPaymentSucceeded(PaymentSucceededEvent e) {
        dispatchToUser(e.base().eventId(), StreamNames.PAYMENT_SUCCEEDED, e.userId(),
                EmailTemplate.PAYMENT_SUCCEEDED, "Payment of $" + e.amount() + " was successful.");
    }

    private void onPaymentFailed(PaymentFailedEvent e) {
        dispatchToUser(e.base().eventId(), StreamNames.PAYMENT_FAILED, e.userId(),
                EmailTemplate.PAYMENT_FAILED,
                "Payment failed: " + e.reason() + ". Please update your payment method.");
    }

    // Resolve the recipient email from userId; skip the notification if it can't
    // be resolved (e.g. the Users lookup is unavailable) rather than emailing a
    // placeholder address.
    private void dispatchToUser(UUID eventId, String eventType, UUID userId,
            EmailTemplate template, String body) {
        Optional<String> recipient = userDirectory.emailFor(userId);
        if (recipient.isEmpty()) {
            log.warn("No email resolved for user {} ({}); skipping notification for event {}",
                    userId, eventType, eventId);
            return;
        }
        sendNotification.execute(eventId, eventType, recipient.get(), template, body);
    }
}
