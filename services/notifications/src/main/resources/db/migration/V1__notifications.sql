CREATE SCHEMA IF NOT EXISTS notifications_schema;

CREATE TABLE notifications_schema.notification_log (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id   UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    recipient  VARCHAR(255) NOT NULL,
    template   VARCHAR(100) NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'SENT',
    error      TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE UNIQUE INDEX idx_notification_log_event_id ON notifications_schema.notification_log(event_id);
CREATE INDEX idx_notification_log_event_type ON notifications_schema.notification_log(event_type);
CREATE INDEX idx_notification_log_recipient ON notifications_schema.notification_log(recipient);
