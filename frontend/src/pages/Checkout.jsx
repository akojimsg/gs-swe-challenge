/*
 * PAGE: Checkout
 * ---------------------------------------------------------------------------
 * Route:  /checkout  ·  Access: Buyer · Scope: core  ·  Shell: CheckoutShell
 * Figma:  1:12229 (customer info) → 1:12347 (shipping & payment)
 * Spec:   gse-requirement-docs/frontend-design/specs/checkout.md
 *
 * API: placeOrder(items, idempotencyKey) → 201 OrderResponse | 200 replay |
 *      409 StockReservationError { shortfalls[] }.  Then poll getOrder(id).
 * CRITICAL: one Idempotency-Key (UUID) per attempt · disable submit on click
 *      (double-click must not create two orders) · after 201 show processing,
 *      navigate to confirmation which polls to PAID/FAILED · 409 → name short item.
 * STATES: empty cart → redirect /cart · ready · submitting (disabled) · out-of-stock · error.
 * BUILD NOTE (MCP): pull 1:12229/1:12347, collapse to a lean single-page form +
 *      sticky OrderSummary. The idempotency + submit-guard wiring below is the contract.
 * SAGA DEP: real PAID/FAILED needs the saga lane; confirmation page handles the poll.
 * ---------------------------------------------------------------------------
 */
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { placeOrder } from "@/api/orders";
import { ApiError } from "@/api/client";
import { useCartStore } from "@/store/cart";
import { Button } from "@/components/ui/Button";
import { newIdempotencyKey, formatMoney } from "@/lib/format";

export default function Checkout() {
  const items = useCartStore((s) => s.items);
  const toOrderLines = useCartStore((s) => s.toOrderLines);
  const clear = useCartStore((s) => s.clear);
  const subtotal = useCartStore((s) => s.subtotal());
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  // Empty cart → back to /cart (checkout.md).
  useEffect(() => {
    if (items.length === 0) navigate("/cart", { replace: true });
  }, [items.length, navigate]);

  const onSubmit = async (e) => {
    e.preventDefault();
    if (submitting) return; // guard double-submit
    setSubmitting(true);
    setError(null);
    const idempotencyKey = newIdempotencyKey();
    try {
      const order = await placeOrder(toOrderLines(), idempotencyKey);
      clear();
      navigate(`/order/${order.id}/confirmation`);
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setError("Some items are out of stock: " + JSON.stringify(err.body?.shortfalls ?? []));
      } else {
        setError("Couldn't place the order. Please try again.");
      }
      setSubmitting(false); // allow retry with a NEW key
    }
  };

  // Minimal proof-of-wiring render — MCP agent implements Figma 1:12229/1:12347.
  return (
    <form onSubmit={onSubmit} className="grid gap-6 lg:grid-cols-[1fr_320px]">
      <div className="space-y-3">
        <h1 className="font-display text-xl font-extrabold">Checkout (STUB)</h1>
        <p className="text-sm text-muted-foreground">
          Test payment — fake processor (~90% succeed). Implement the Figma form here.
        </p>
        {error && <p className="text-sm text-danger">{error}</p>}
      </div>
      <aside className="h-fit rounded-lg border border-border p-4">
        <p className="flex justify-between font-medium">
          <span>Total</span>
          <span>{formatMoney(subtotal)}</span>
        </p>
        <Button type="submit" className="mt-4 w-full" disabled={submitting}>
          {submitting ? "Placing order…" : "Place order"}
        </Button>
      </aside>
    </form>
  );
}
