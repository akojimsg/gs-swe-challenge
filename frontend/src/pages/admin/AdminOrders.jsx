import { useEffect, useState } from "react";
import { listOrders, setOrderStatus } from "@/api/orders";
import { useAuthStore } from "@/store/auth";
import { Badge } from "@/components/ui/Badge";
import { Spinner, ErrorState, EmptyState } from "@/components/common/States";
import { useToast } from "@/components/common/Toast";
import { formatMoney } from "@/lib/format";
import { ORDER_STATUS, ORDER_STATUS_COLOR } from "@/lib/constants";
import { Loader2 } from "lucide-react";

const MAX_ROWS = 50;
const STATUSES = Object.values(ORDER_STATUS);

export function OrderStatusBadge({ status }) {
  return <Badge tone={ORDER_STATUS_COLOR[status] ?? "neutral"}>{status}</Badge>;
}

export default function AdminOrders() {
  const isAdmin = useAuthStore((s) => s.user?.role === "ADMIN");
  const toast = useToast();

  const [orders, setOrders] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [updating, setUpdating] = useState(null);

  const load = () => {
    setLoading(true);
    setError(null);
    listOrders()
      .then((data) => setOrders(Array.isArray(data) ? data : data?.content ?? []))
      .catch((e) => setError(e))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const onStatusChange = async (id, status) => {
    setUpdating(id);
    try {
      await setOrderStatus(id, status);
      load();
    } catch {
      toast?.("Couldn't update order status", "error");
    } finally {
      setUpdating(null);
    }
  };

  if (loading && !orders) return <Spinner label="Loading orders…" />;
  if (error) return <ErrorState message="Couldn't load orders." onRetry={load} />;

  const all = orders ?? [];
  if (all.length === 0) {
    return (
      <EmptyState
        title="No orders yet"
        message="Orders will appear here once customers start buying."
      />
    );
  }

  const rows = all.slice(0, MAX_ROWS);

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-xl font-bold">{isAdmin ? "Orders" : "My Orders"}</h1>
        <p className="text-sm text-muted-foreground">{all.length} total</p>
      </div>

      <div className="overflow-x-auto rounded-xl border border-border">
        <table className="w-full text-sm">
          <thead className="border-b border-border bg-muted/50">
            <tr>
              <th className="px-4 py-3 text-left font-medium">Order ID</th>
              {isAdmin && <th className="px-4 py-3 text-left font-medium">Customer</th>}
              <th className="px-4 py-3 text-left font-medium">Items</th>
              <th className="px-4 py-3 text-right font-medium">Total</th>
              <th className="px-4 py-3 text-left font-medium">Status</th>
              {isAdmin && <th className="px-4 py-3 text-left font-medium">Actions</th>}
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {rows.map((order) => (
              <tr key={order.id} className="hover:bg-muted/30 transition">
                <td className="px-4 py-3 font-mono text-xs" title={order.id}>
                  {order.id.slice(0, 8)}
                </td>
                {isAdmin && (
                  <td className="px-4 py-3 font-mono text-xs" title={order.userId}>
                    {order.userId?.slice(0, 8) ?? "—"}
                  </td>
                )}
                <td className="px-4 py-3 text-muted-foreground">
                  {order.items?.length ?? 0} items
                </td>
                <td className="px-4 py-3 text-right font-medium text-brand">
                  {formatMoney(order.total)}
                </td>
                <td className="px-4 py-3">
                  <OrderStatusBadge status={order.status} />
                </td>
                {isAdmin && (
                  <td className="px-4 py-3">
                    {updating === order.id ? (
                      <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
                    ) : (
                      <select
                        value={order.status}
                        onChange={(e) => onStatusChange(order.id, e.target.value)}
                        aria-label={`Change status for order ${order.id.slice(0, 8)}`}
                        className="rounded-md border border-input bg-background px-2 py-1 text-sm outline-none focus:ring-2 focus:ring-ring"
                      >
                        {STATUSES.map((s) => (
                          <option key={s} value={s}>
                            {s}
                          </option>
                        ))}
                      </select>
                    )}
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {all.length > MAX_ROWS && (
        <p className="mt-3 text-sm text-muted-foreground">Showing first {MAX_ROWS} orders</p>
      )}
    </div>
  );
}
