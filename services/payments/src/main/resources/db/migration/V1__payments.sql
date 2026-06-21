CREATE TABLE payments_schema.payments (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id       UUID UNIQUE NOT NULL,
    user_id        UUID NOT NULL,
    amount         NUMERIC(10,2) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    method         VARCHAR(30) NOT NULL DEFAULT 'FAKE_CARD',
    last4          VARCHAR(4),
    failure_reason VARCHAR(50),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_order_id ON payments_schema.payments(order_id);
CREATE INDEX idx_payments_user_id ON payments_schema.payments(user_id);
CREATE INDEX idx_payments_status ON payments_schema.payments(status);
