/*
 * PAGE: Cart
 * ---------------------------------------------------------------------------
 * Route:  /cart  ·  Access: Public (checkout requires auth) · Scope: core
 * Shell:  StorefrontShell  ·  Figma: node 1:12102
 * Spec:   gse-requirement-docs/frontend-design/specs/cart.md
 *
 * DATA: client-side only — cart store (no cart API). Items
 *   { productId,name,sku,price,qty,stock }. Optional: re-fetch each product to
 *   flag price/stock changes.
 * STATES: empty (EmptyState + browse CTA) · has items (line items + summary) ·
 *   qty change (live totals, rounded) · guest→checkout (redirect /login?redirect=/checkout).
 * INTERACTIONS: qty stepper clamps 1..stock; 0/remove deletes line · totals rounded,
 *   "Free" at 0 · "Proceed to checkout": authed → /checkout, guest → sign-in (cart preserved).
 * BUILD NOTE (MCP): pull 1:12102 → CartLineItem + CartSummary (sticky rail).
 * ---------------------------------------------------------------------------
 */
import { Link, useNavigate } from "react-router-dom";
import { useCartStore } from "@/store/cart";
import { useAuthStore } from "@/store/auth";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/common/States";
import { formatPrice, formatMoney } from "@/lib/format";

export default function Cart() {
  const items = useCartStore((s) => s.items);
  const setQty = useCartStore((s) => s.setQty);
  const removeItem = useCartStore((s) => s.removeItem);
  const subtotal = useCartStore((s) => s.subtotal());
  const isAuthed = useAuthStore((s) => !!s.accessToken);
  const navigate = useNavigate();

  if (items.length === 0)
    return (
      <EmptyState
        title="Your cart is empty"
        action={
          <Link to="/products">
            <Button variant="outline">Browse products</Button>
          </Link>
        }
      />
    );

  const checkout = () =>
    navigate(isAuthed ? "/checkout" : "/login?redirect=/checkout");

  // Minimal proof-of-wiring render — MCP agent implements Figma 1:12102.
  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
      <ul className="space-y-3">
        {items.map((i) => (
          <li key={i.productId} className="flex items-center justify-between rounded-lg border border-border p-3">
            <div>
              <p className="font-medium">{i.name}</p>
              <p className="text-sm text-brand">{formatPrice(i.price)}</p>
            </div>
            <div className="flex items-center gap-2">
              <input
                type="number"
                min={1}
                max={i.stock}
                value={i.qty}
                onChange={(e) => setQty(i.productId, Number(e.target.value))}
                className="w-16 rounded-md border border-input px-2 py-1"
                aria-label={`Quantity for ${i.name}`}
              />
              <Button variant="ghost" size="sm" onClick={() => removeItem(i.productId)}>
                Remove
              </Button>
            </div>
          </li>
        ))}
      </ul>
      <aside className="h-fit rounded-lg border border-border p-4">
        <p className="flex justify-between font-medium">
          <span>Total</span>
          <span>{formatMoney(subtotal)}</span>
        </p>
        <Button className="mt-4 w-full" onClick={checkout}>
          Proceed to checkout
        </Button>
      </aside>
    </div>
  );
}
