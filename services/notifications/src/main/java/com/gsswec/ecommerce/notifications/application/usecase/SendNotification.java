package com.gsswec.ecommerce.notifications.application.usecase;

import com.gsswec.ecommerce.notifications.application.port.out.EmailSender;
import com.gsswec.ecommerce.notifications.application.port.out.NotificationRepository;
import com.gsswec.ecommerce.notifications.domain.model.EmailTemplate;
import com.gsswec.ecommerce.notifications.domain.model.NotificationLog;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SendNotification {

    private static final Logger log = LoggerFactory.getLogger(SendNotification.class);

    private final NotificationRepository repository;
    private final EmailSender emailSender;

    public SendNotification(NotificationRepository repository, EmailSender emailSender) {
        this.repository = repository;
        this.emailSender = emailSender;
    }

    @Transactional
    public void execute(UUID eventId, String eventType, String recipient, EmailTemplate template, String body) {
        if (repository.existsByEventId(eventId)) {
            log.debug("Duplicate event {} skipped (already delivered)", eventId);
            return;
        }

        String status = "SENT";
        String error = null;
        try {
            emailSender.send(recipient, template, body);
        } catch (Exception e) {
            log.warn("Email send failed for event {}: {}", eventId, e.getMessage());
            status = "FAILED";
            error = e.getMessage();
        }

        repository.save(new NotificationLog(
                null, eventId, eventType, recipient,
                template.name, status, error, Instant.now()));
    }
}
