/*
 * PAGE: Order Confirmation
 * ---------------------------------------------------------------------------
 * Route:  /order/:id/confirmation  ·  Access: Buyer (own) · Scope: core
 * Shell:  StorefrontShell  ·  Figma: 1:12481, 1:12670
 * Spec:   gse-requirement-docs/frontend-design/specs/order-confirmation.md
 *
 * API: getOrder(id) → { status, items[], total, createdAt }. Poll while
 *      AWAITING_PAYMENT (saga is async) until terminal (PAID/FAILED/CANCELLED).
 * STATES (the whole point): AWAITING_PAYMENT → processing spinner · PAID → success
 *      (order #, items, total) · FAILED → first-class PaymentFailedPanel + retry
 *      (~10% by design) · 404 not found / not owner.
 * BUILD NOTE (MCP): pull 1:12481/1:12670 → success view + PaymentFailedPanel +
 *      OrderSummary. The bounded poll loop below is the contract.
 * SAGA DEP: until the saga lane lands, a stub backend may stay AWAITING_PAYMENT —
 *      the attempt bound stops the poll gracefully.
 * ---------------------------------------------------------------------------
 */
import { useEffect, useRef, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { getOrder } from "@/api/orders";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { Spinner, ErrorState } from "@/components/common/States";
import { formatMoney } from "@/lib/format";
import {
  ORDER_STATUS,
  ORDER_STATUS_COLOR,
  ORDER_TERMINAL,
  PAYMENT_POLL_INTERVAL_MS,
  PAYMENT_POLL_MAX_ATTEMPTS,
} from "@/lib/constants";

export default function OrderConfirmation() {
  const { id } = useParams();
  const [order, setOrder] = useState(null);
  const [status, setStatus] = useState("loading"); // loading | ok | notfound | error
  const attempts = useRef(0);

  useEffect(() => {
    let timer;
    let active = true;

    const poll = async () => {
      try {
        const o = await getOrder(id);
        if (!active) return;
        setOrder(o);
        setStatus("ok");
        attempts.current += 1;
        const terminal = ORDER_TERMINAL.includes(o.status);
        if (!terminal && attempts.current < PAYMENT_POLL_MAX_ATTEMPTS) {
          timer = setTimeout(poll, PAYMENT_POLL_INTERVAL_MS);
        }
      } catch (e) {
        if (active) setStatus(e?.status === 404 ? "notfound" : "error");
      }
    };
    poll();
    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [id]);

  if (status === "loading") return <Spinner label="Loading order…" />;
  if (status === "notfound") return <ErrorState message="Order not found." />;
  if (status === "error") return <ErrorState message="Couldn't load the order." />;

  const isProcessing = order.status === ORDER_STATUS.AWAITING_PAYMENT || order.status === ORDER_STATUS.PENDING;
  const isFailed = order.status === ORDER_STATUS.FAILED;

  // Minimal proof-of-wiring render — MCP agent implements Figma 1:12481/1:12670.
  return (
    <div className="max-w-lg">
      {isProcessing && <Spinner label="Confirming your payment…" />}
      {!isProcessing && (
        <>
          <div className="mb-3 flex items-center gap-2">
            <h1 className="font-display text-2xl font-extrabold">
              {isFailed ? "Payment failed" : "Order confirmed"}
            </h1>
            <Badge tone={ORDER_STATUS_COLOR[order.status]}>{order.status}</Badge>
          </div>
          <p className="text-sm text-muted-foreground">Order #{order.id}</p>
          <p className="my-2 font-medium">Total {formatMoney(order.total)}</p>
          {isFailed ? (
            <Link to="/cart">
              <Button variant="brand">Retry — back to cart</Button>
            </Link>
          ) : (
            <Link to="/products">
              <Button variant="outline">Continue shopping</Button>
            </Link>
          )}
        </>
      )}
    </div>
  );
}
