import { useEffect, useRef, useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { useCartStore } from "@/store/cart";
import { useAuthStore } from "@/store/auth";
import { placeOrder, getOrder } from "@/api/orders";
import { ApiError } from "@/api/client";
import { Button } from "@/components/ui/Button";
import { formatMoney, newIdempotencyKey } from "@/lib/format";
import { ORDER_TERMINAL, PAYMENT_POLL_INTERVAL_MS, PAYMENT_POLL_MAX_ATTEMPTS } from "@/lib/constants";
import { Loader2, ShieldCheck } from "lucide-react";

function OrderSummaryPanel({ items, subtotal }) {
  return (
    <div className="rounded-xl border border-border bg-card p-5">
      <h2 className="mb-4 text-sm font-semibold">Order summary</h2>
      <ul className="space-y-2">
        {items.map((item) => (
          <li key={item.productId} className="flex items-center justify-between gap-2 text-sm">
            <span className="flex-1 line-clamp-1 text-muted-foreground">
              {item.name} × {item.qty}
            </span>
            <span>{formatMoney(item.price * item.qty)}</span>
          </li>
        ))}
      </ul>
      <div className="mt-3 border-t border-border pt-3">
        <div className="flex justify-between text-sm">
          <span className="text-muted-foreground">Subtotal</span>
          <span>{formatMoney(subtotal)}</span>
        </div>
        <div className="mt-1 flex justify-between text-sm text-muted-foreground">
          <span>Shipping</span>
          <span className="text-success text-xs font-medium">Free</span>
        </div>
        <div className="mt-3 flex justify-between font-semibold">
          <span>Total</span>
          <span className="text-brand">{formatMoney(subtotal)}</span>
        </div>
      </div>
    </div>
  );
}

export default function Checkout() {
  const items = useCartStore((s) => s.items);
  const subtotal = useCartStore((s) => s.subtotal());
  const toOrderLines = useCartStore((s) => s.toOrderLines);
  const clearCart = useCartStore((s) => s.clear);
  const user = useAuthStore((s) => s.user);
  const navigate = useNavigate();

  const [form, setForm] = useState({
    firstName: user?.firstName ?? "",
    lastName: user?.lastName ?? "",
    email: user?.email ?? "",
    address: "",
    city: "",
    zip: "",
    // fake card
    cardNumber: "4111 1111 1111 1111",
    cardExpiry: "12/28",
    cardCvv: "123",
  });
  const [status, setStatus] = useState("idle"); // idle | submitting | polling | success | failed | error
  const [errorMsg, setErrorMsg] = useState(null);
  const idempotencyKey = useRef(newIdempotencyKey());
  const pollRef = useRef(null);

  // cleanup poll on unmount — must run before any early return (rules-of-hooks)
  useEffect(() => () => clearTimeout(pollRef.current), []);

  // Empty cart → back to cart. Safe after the hook above; also covers the
  // post-success state where the cart was cleared.
  if (items.length === 0 && status !== "success" && status !== "polling")
    return <Navigate to="/cart" replace />;

  const onChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  const pollOrder = (orderId, attempt = 0) => {
    if (attempt >= PAYMENT_POLL_MAX_ATTEMPTS) {
      setStatus("failed");
      setErrorMsg("Payment timed out. Please try again.");
      return;
    }
    pollRef.current = setTimeout(async () => {
      try {
        const order = await getOrder(orderId);
        if (ORDER_TERMINAL.includes(order.status)) {
          if (order.status === "PAID") {
            clearCart();
            navigate(`/order/${orderId}/confirmation`, { replace: true });
          } else {
            setStatus("failed");
            setErrorMsg(`Payment ${order.status.toLowerCase()}. Please try again.`);
          }
        } else {
          pollOrder(orderId, attempt + 1);
        }
      } catch {
        pollOrder(orderId, attempt + 1);
      }
    }, PAYMENT_POLL_INTERVAL_MS);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (status === "submitting" || status === "polling") return;
    setStatus("submitting");
    setErrorMsg(null);
    try {
      const order = await placeOrder(toOrderLines(), idempotencyKey.current);
      setStatus("polling");
      pollOrder(order.id);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 409 || err.status === 422) {
          const msg = err.body?.message ?? "Some items are out of stock.";
          setErrorMsg(msg);
        } else {
          setErrorMsg("Couldn't place order. Please try again.");
        }
      } else {
        setErrorMsg("Network error. Please try again.");
      }
      setStatus("error");
      // refresh idempotency key only on non-409 errors (not stock issues)
      if (!(err instanceof ApiError && (err.status === 409 || err.status === 422))) {
        idempotencyKey.current = newIdempotencyKey();
      }
    }
  };

  const isProcessing = status === "submitting" || status === "polling";

  const inputClass = "w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring disabled:opacity-50";

  return (
    <div className="grid gap-8 lg:grid-cols-3">
      {/* form */}
      <form onSubmit={handleSubmit} className="lg:col-span-2 space-y-6">
        {/* contact */}
        <section>
          <h2 className="mb-3 text-base font-semibold">Contact information</h2>
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <label className="mb-1 block text-sm font-medium">First name</label>
              <input name="firstName" required value={form.firstName} onChange={onChange} disabled={isProcessing} className={inputClass} />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium">Last name</label>
              <input name="lastName" required value={form.lastName} onChange={onChange} disabled={isProcessing} className={inputClass} />
            </div>
            <div className="sm:col-span-2">
              <label className="mb-1 block text-sm font-medium">Email</label>
              <input name="email" type="email" required value={form.email} onChange={onChange} disabled={isProcessing} className={inputClass} />
            </div>
          </div>
        </section>

        {/* shipping */}
        <section>
          <h2 className="mb-3 text-base font-semibold">Shipping address</h2>
          <div className="grid gap-3">
            <div>
              <label className="mb-1 block text-sm font-medium">Street address</label>
              <input name="address" required value={form.address} onChange={onChange} disabled={isProcessing} placeholder="123 Main St" className={inputClass} />
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              <div>
                <label className="mb-1 block text-sm font-medium">City</label>
                <input name="city" required value={form.city} onChange={onChange} disabled={isProcessing} className={inputClass} />
              </div>
              <div>
                <label className="mb-1 block text-sm font-medium">ZIP code</label>
                <input name="zip" required value={form.zip} onChange={onChange} disabled={isProcessing} className={inputClass} />
              </div>
            </div>
          </div>
        </section>

        {/* payment */}
        <section>
          <h2 className="mb-1 text-base font-semibold">Payment</h2>
          <p className="mb-3 flex items-center gap-1.5 text-xs text-muted-foreground">
            <ShieldCheck className="h-3.5 w-3.5 text-success" />
            Test mode — any valid-format card is accepted. Use the pre-filled values.
          </p>
          <div className="grid gap-3">
            <div>
              <label className="mb-1 block text-sm font-medium">Card number</label>
              <input name="cardNumber" value={form.cardNumber} onChange={onChange} disabled={isProcessing} className={inputClass} maxLength={19} />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="mb-1 block text-sm font-medium">Expiry</label>
                <input name="cardExpiry" value={form.cardExpiry} onChange={onChange} disabled={isProcessing} placeholder="MM/YY" className={inputClass} />
              </div>
              <div>
                <label className="mb-1 block text-sm font-medium">CVV</label>
                <input name="cardCvv" value={form.cardCvv} onChange={onChange} disabled={isProcessing} placeholder="123" className={inputClass} maxLength={4} />
              </div>
            </div>
          </div>
        </section>

        {/* error */}
        {errorMsg && (
          <div className="rounded-md border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
            <p>{errorMsg}</p>
            {(status === "failed" || status === "error") && (
              <Link to="/cart" className="mt-1 block text-xs underline">← Back to cart</Link>
            )}
          </div>
        )}

        {/* submit (desktop — hidden in the summary panel on mobile) */}
        <div className="hidden lg:block">
          <PlaceOrderButton isProcessing={isProcessing} status={status} />
        </div>
      </form>

      {/* summary sidebar */}
      <div className="space-y-4">
        <OrderSummaryPanel items={items} subtotal={subtotal} />
        <div className="lg:hidden">
          <PlaceOrderButton isProcessing={isProcessing} status={status} onClick={handleSubmit} />
        </div>
        <Button
          type="submit"
          form="checkout-form"
          disabled={isProcessing}
          size="lg"
          className="hidden w-full lg:flex gap-2"
          onClick={handleSubmit}
        >
          {isProcessing && <Loader2 className="h-4 w-4 animate-spin" />}
          {status === "polling" ? "Processing payment…" : "Place order"}
        </Button>
      </div>
    </div>
  );
}

function PlaceOrderButton({ isProcessing, status, onClick }) {
  return (
    <Button
      type={onClick ? "button" : "submit"}
      onClick={onClick}
      disabled={isProcessing}
      size="lg"
      className="w-full gap-2"
    >
      {isProcessing && <Loader2 className="h-4 w-4 animate-spin" />}
      {status === "polling" ? "Processing payment…" : "Place order"}
    </Button>
  );
}
