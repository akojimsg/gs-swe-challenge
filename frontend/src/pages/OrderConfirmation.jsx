import { useEffect, useRef, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { getOrder } from "@/api/orders";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { Spinner, ErrorState } from "@/components/common/States";
import ProductImage from "@/components/product/ProductImage";
import { formatMoney } from "@/lib/format";
import { ORDER_STATUS, ORDER_STATUS_COLOR, ORDER_TERMINAL, PAYMENT_POLL_INTERVAL_MS, PAYMENT_POLL_MAX_ATTEMPTS } from "@/lib/constants";
import { CheckCircle2, XCircle, Clock, ShoppingBag } from "lucide-react";

function StatusIcon({ status }) {
  if (status === ORDER_STATUS.PAID)
    return <CheckCircle2 className="h-16 w-16 text-success" />;
  if (status === ORDER_STATUS.FAILED || status === ORDER_STATUS.CANCELLED)
    return <XCircle className="h-16 w-16 text-danger" />;
  return <Clock className="h-16 w-16 text-warning animate-pulse" />;
}

function StatusHeading({ status }) {
  if (status === ORDER_STATUS.PAID) return "Payment confirmed!";
  if (status === ORDER_STATUS.FAILED) return "Payment failed";
  if (status === ORDER_STATUS.CANCELLED) return "Order cancelled";
  return "Processing your payment…";
}

export default function OrderConfirmation() {
  const { id } = useParams();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const pollRef = useRef(null);

  const load = async () => {
    try {
      const o = await getOrder(id);
      setOrder(o);
      if (!ORDER_TERMINAL.includes(o.status)) {
        startPolling();
      }
    } catch (e) {
      setError(e);
    } finally {
      setLoading(false);
    }
  };

  const startPolling = (attempt = 0) => {
    if (attempt >= PAYMENT_POLL_MAX_ATTEMPTS) return;
    pollRef.current = setTimeout(async () => {
      try {
        const o = await getOrder(id);
        setOrder(o);
        if (!ORDER_TERMINAL.includes(o.status)) {
          startPolling(attempt + 1);
        }
      } catch {
        startPolling(attempt + 1);
      }
    }, PAYMENT_POLL_INTERVAL_MS);
  };

  useEffect(() => {
    load();
    return () => clearTimeout(pollRef.current);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  if (loading) return <Spinner label="Loading order…" />;
  if (error) return <ErrorState message="Couldn't load order." onRetry={load} />;
  if (!order) return null;

  const isTerminal = ORDER_TERMINAL.includes(order.status);
  const isFailed = order.status === ORDER_STATUS.FAILED || order.status === ORDER_STATUS.CANCELLED;
  const statusColor = ORDER_STATUS_COLOR[order.status] ?? "neutral";

  return (
    <div className="mx-auto max-w-lg py-8">
      {/* status hero */}
      <div className="mb-8 flex flex-col items-center gap-3 text-center">
        <StatusIcon status={order.status} />
        <h1 className="font-display text-2xl font-extrabold">
          {StatusHeading({ status: order.status })}
        </h1>
        <Badge tone={statusColor} className="text-sm px-3 py-1">
          {order.status}
        </Badge>
        {!isTerminal && (
          <p className="text-sm text-muted-foreground">
            Your payment is being processed. This page updates automatically.
          </p>
        )}
        {isFailed && (
          <p className="text-sm text-muted-foreground">
            Your cart has been restored. Please try again.
          </p>
        )}
      </div>

      {/* order details */}
      <div className="rounded-xl border border-border bg-card">
        <div className="border-b border-border px-5 py-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs text-muted-foreground">Order ID</p>
              <p className="font-mono text-sm font-medium">{order.id}</p>
            </div>
            <div className="text-right">
              <p className="text-xs text-muted-foreground">Total</p>
              <p className="text-lg font-bold text-brand">{formatMoney(order.total)}</p>
            </div>
          </div>
        </div>

        {order.items?.length > 0 && (
          <ul className="divide-y divide-border px-5">
            {order.items.map((item) => (
              <li key={item.id ?? item.productId} className="flex items-center justify-between gap-3 py-3 text-sm">
                <ProductImage
                  size="thumb"
                  product={{ id: item.productId, name: item.name, imageUrl: null }}
                  className="h-12 w-12 shrink-0"
                />
                <div className="flex-1 min-w-0">
                  <p className="font-medium line-clamp-1">{item.name}</p>
                  <p className="text-xs text-muted-foreground">SKU: {item.sku} · Qty: {item.quantity}</p>
                </div>
                <span>{formatMoney(Number(item.unitPrice) * item.quantity)}</span>
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* CTAs */}
      <div className="mt-6 flex flex-col gap-3 sm:flex-row">
        <Button asChild className="flex-1">
          <Link to="/products">
            <ShoppingBag className="h-4 w-4" />
            Continue shopping
          </Link>
        </Button>
        {isFailed && (
          <Button asChild variant="outline" className="flex-1">
            <Link to="/checkout">Try again</Link>
          </Button>
        )}
      </div>
    </div>
  );
}
