/*
 * Display formatters. Single source so "Free" / currency rules stay consistent
 * across catalogue, PDP, cart, checkout (per the page specs).
 */

// price === 0 renders "Free"; otherwise "$" + 2 decimals.
export function formatPrice(value) {
  const n = Number(value);
  if (!Number.isFinite(n)) return "—";
  if (n === 0) return "Free";
  return `$${n.toFixed(2)}`;
}

export function formatMoney(value) {
  return `$${Number(value || 0).toFixed(2)}`;
}

// A client-generated idempotency key per checkout attempt (POST /orders header).
export function newIdempotencyKey() {
  if (typeof crypto !== "undefined" && crypto.randomUUID) return crypto.randomUUID();
  // Fallback (older browsers): timestamp + random.
  return `idem-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}
