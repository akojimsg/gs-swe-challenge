/*
 * Single source of truth for status/stock → semantic-color mapping and thresholds.
 * Mirrors gse-requirement-docs/frontend-design/design-system.md so every badge is
 * consistent. Tokens (brand/info/warning/success/danger) are Tailwind colors backed
 * by CSS vars in tokens.css.
 */

// Order status set — the AUTHORITATIVE design state machine
// (order-placement.md): PENDING → AWAITING_PAYMENT → PAID/FAILED, + CANCELLED.
// NOTE: docs/api/orders-openapi.yaml currently lists SHIPPED/DELIVERED and omits
// AWAITING_PAYMENT — that drift is tracked for reconciliation; the FE uses THIS set.
export const ORDER_STATUS = {
  PENDING: "PENDING",
  AWAITING_PAYMENT: "AWAITING_PAYMENT",
  PAID: "PAID",
  FAILED: "FAILED",
  CANCELLED: "CANCELLED",
};

// status → semantic color token (used by OrderStatusBadge).
export const ORDER_STATUS_COLOR = {
  PENDING: "neutral",
  AWAITING_PAYMENT: "warning",
  PAID: "success",
  FAILED: "danger",
  CANCELLED: "neutral",
};

// Terminal statuses stop the confirmation-page poll.
export const ORDER_TERMINAL = [ORDER_STATUS.PAID, ORDER_STATUS.FAILED, ORDER_STATUS.CANCELLED];

// Stock badge thresholds + colors.
export const LOW_STOCK_THRESHOLD = 25;

export function stockLevel(stock) {
  if (stock <= 0) return { label: "Out of stock", color: "danger", inStock: false };
  if (stock <= LOW_STOCK_THRESHOLD)
    return { label: `${stock} left`, color: "warning", inStock: true };
  return { label: `${stock} in stock`, color: "success", inStock: true };
}

// Payment failure is ~10% by design (fake processor) — the failed screen is
// first-class, not an edge case.
export const PAYMENT_POLL_INTERVAL_MS = 2000;
export const PAYMENT_POLL_MAX_ATTEMPTS = 30; // ~60s bound

export const MAX_CSV_BYTES = 10 * 1024 * 1024; // 10MB import guard
