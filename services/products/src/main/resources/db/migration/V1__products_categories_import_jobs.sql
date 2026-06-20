CREATE TABLE products_schema.categories (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE products_schema.products (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    sku         VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    category_id INTEGER REFERENCES products_schema.categories(id) ON DELETE SET NULL,
    price       NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    stock       INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    weight_kg   NUMERIC(8,3),
    active      BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE products_schema.import_jobs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requested_by UUID NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    total_rows   INTEGER,
    imported     INTEGER,
    updated      INTEGER,
    skipped      INTEGER,
    errors       JSONB,
    duration_ms  BIGINT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_products_sku ON products_schema.products(sku);
CREATE INDEX idx_products_category_id ON products_schema.products(category_id);
CREATE INDEX idx_products_active ON products_schema.products(active);
CREATE INDEX idx_products_fts ON products_schema.products
    USING GIN (to_tsvector('english', name || ' ' || COALESCE(description, '')));
