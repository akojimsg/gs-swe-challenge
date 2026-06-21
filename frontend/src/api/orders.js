/*
 * Orders endpoints — bound to docs/api/orders-openapi.yaml.
 *
 * placeOrder: POST /orders with REQUIRED `Idempotency-Key` header. Body
 *   { items: [{ productId, quantity }] }. → 201 OrderResponse (or 200 on
 *   idempotent replay). 409 → StockReservationError { message, shortfalls[] }.
 * OrderResponse: { id, userId, status, items[], total, createdAt, updatedAt }.
 *   status set (design): PENDING|AWAITING_PAYMENT|PAID|FAILED|CANCELLED.
 *
 * The saga is async: after placement, poll getOrder(id) until terminal (PAID/
 * FAILED). The saga lane delivers the real status transitions; until it merges,
 * a stub backend may keep returning AWAITING_PAYMENT — poll bound handles that.
 */
import { api } from "./client";

function toQuery(params = {}) {
  const q = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== "") q.append(k, v);
  });
  const s = q.toString();
  return s ? `?${s}` : "";
}

// Place an order [BUYER]. idempotencyKey = client-generated UUID per attempt.
export const placeOrder = (items, idempotencyKey) =>
  api.post(`/orders`, { items }, { headers: { "Idempotency-Key": idempotencyKey } });

export const getOrder = (id) => api.get(`/orders/${id}`);
export const listOrders = (params) => api.get(`/orders${toQuery(params)}`);
export const cancelOrder = (id) => api.del(`/orders/${id}/cancel`);
// Admin status advance [ADMIN].
export const setOrderStatus = (id, status) => api.patch(`/orders/${id}/status`, { status });
