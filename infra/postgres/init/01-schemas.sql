-- Bootstrap the five service schemas in the single shared PostgreSQL instance
-- (ADR-001). Each service owns its schema exclusively; no cross-schema access.
-- Runs once, on first container init, against POSTGRES_DB.
-- Per-table DDL is owned by each service via its own Flyway migrations.

CREATE SCHEMA IF NOT EXISTS users_schema;
CREATE SCHEMA IF NOT EXISTS products_schema;
CREATE SCHEMA IF NOT EXISTS orders_schema;
CREATE SCHEMA IF NOT EXISTS payments_schema;
CREATE SCHEMA IF NOT EXISTS notifications_schema;
