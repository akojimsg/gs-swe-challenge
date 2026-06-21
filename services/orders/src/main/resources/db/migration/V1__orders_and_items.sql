CREATE TABLE orders_schema.orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    total           NUMERIC(10,2) NOT NULL,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE orders_schema.order_items (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id   UUID NOT NULL REFERENCES orders_schema.orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    sku        VARCHAR(100) NOT NULL,
    name       VARCHAR(255) NOT NULL,
    quantity   INTEGER NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(10,2) NOT NULL
);

CREATE INDEX idx_orders_user_id ON orders_schema.orders(user_id);
CREATE INDEX idx_orders_status ON orders_schema.orders(status);
CREATE INDEX idx_orders_idempotency_key ON orders_schema.orders(idempotency_key);
CREATE INDEX idx_order_items_order_id ON orders_schema.order_items(order_id);
