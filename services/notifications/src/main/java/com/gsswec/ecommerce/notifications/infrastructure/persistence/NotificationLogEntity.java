package com.gsswec.ecommerce.notifications.infrastructure.persistence;

import com.gsswec.ecommerce.notifications.domain.model.NotificationLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_log", schema = "notifications_schema")
public class NotificationLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "template", nullable = false)
    private String template;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "error")
    private String error;

    @Column(name = "created_at")
    private Instant createdAt;

    protected NotificationLogEntity() {}

    public NotificationLogEntity(UUID eventId, String eventType, String recipient,
            String template, String status, String error, Instant createdAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.recipient = recipient;
        this.template = template;
        this.status = status;
        this.error = error;
        this.createdAt = createdAt;
    }

    public NotificationLog toDomain() {
        return new NotificationLog(id, eventId, eventType, recipient, template, status, error, createdAt);
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getRecipient() { return recipient; }
    public String getTemplate() { return template; }
    public String getStatus() { return status; }
    public String getError() { return error; }
    public Instant getCreatedAt() { return createdAt; }
}
