import { useEffect, useRef, useState } from "react";
import { listNotificationLogs } from "@/api/notifications";
import { Badge } from "@/components/ui/Badge";
import { Pagination } from "@/components/common/Pagination";
import { Spinner, ErrorState, EmptyState } from "@/components/common/States";

const PAGE_SIZE = 20;

function DeliveryStatusBadge({ status }) {
  return <Badge tone={status === "SENT" ? "success" : "danger"}>{status}</Badge>;
}

export default function AdminNotifications() {
  const [pageData, setPageData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [page, setPage] = useState(0);
  const [typeFilter, setTypeFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");

  // Event-type options are derived from the first unfiltered load and held stable
  // so the select doesn't shift while filtering.
  const [typeOptions, setTypeOptions] = useState([]);
  const typesLocked = useRef(false);
  const tableTop = useRef(null);

  const load = () => {
    setLoading(true);
    setError(null);
    listNotificationLogs({
      type: typeFilter || undefined,
      status: statusFilter || undefined,
      page,
      size: PAGE_SIZE,
    })
      .then((data) => {
        setPageData(data);
        if (!typesLocked.current) {
          const types = [...new Set((data?.content ?? []).map((l) => l.type))].sort();
          setTypeOptions(types);
          typesLocked.current = true;
        }
      })
      .catch((e) => setError(e))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, typeFilter, statusFilter]);

  const onFilterChange = (setter) => (e) => {
    setter(e.target.value);
    setPage(0);
  };

  const onPageChange = (p) => {
    setPage(p);
    tableTop.current?.scrollIntoView({ behavior: "smooth", block: "start" });
  };

  const logs = pageData?.content ?? [];

  return (
    <div ref={tableTop}>
      <div className="mb-6">
        <h1 className="text-xl font-bold">Notifications</h1>
        <p className="text-sm text-muted-foreground">{pageData?.totalElements ?? 0} total</p>
      </div>

      <div className="mb-4 flex flex-wrap gap-3">
        <select
          value={typeFilter}
          onChange={onFilterChange(setTypeFilter)}
          aria-label="Filter by event type"
          className="rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
        >
          <option value="">All types</option>
          {typeOptions.map((t) => (
            <option key={t} value={t}>
              {t}
            </option>
          ))}
        </select>
        <select
          value={statusFilter}
          onChange={onFilterChange(setStatusFilter)}
          aria-label="Filter by delivery status"
          className="rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
        >
          <option value="">All</option>
          <option value="SENT">Sent</option>
          <option value="FAILED">Failed</option>
        </select>
      </div>

      {loading && !pageData ? (
        <Spinner label="Loading notifications…" />
      ) : error ? (
        <ErrorState message="Couldn't load notifications." onRetry={load} />
      ) : logs.length === 0 ? (
        <EmptyState
          title="No notifications"
          message="Delivery logs will appear here once events are processed."
        />
      ) : (
        <>
          <div className="overflow-x-auto rounded-xl border border-border">
            <table className="w-full text-sm">
              <thead className="border-b border-border bg-muted/50">
                <tr>
                  <th className="px-4 py-3 text-left font-medium">Type</th>
                  <th className="px-4 py-3 text-left font-medium">Recipient</th>
                  <th className="px-4 py-3 text-left font-medium hidden sm:table-cell">Template</th>
                  <th className="px-4 py-3 text-left font-medium">Status</th>
                  <th className="px-4 py-3 text-left font-medium hidden md:table-cell">Sent at</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {logs.map((log) => (
                  <tr key={log.id} className="hover:bg-muted/30 transition">
                    <td className="px-4 py-3 font-mono text-xs">{log.type}</td>
                    <td className="px-4 py-3 max-w-[200px] truncate" title={log.recipient}>
                      {log.recipient}
                    </td>
                    <td className="px-4 py-3 font-mono text-xs text-muted-foreground hidden sm:table-cell">
                      {log.template}
                    </td>
                    <td className="px-4 py-3">
                      <DeliveryStatusBadge status={log.status} />
                    </td>
                    <td className="px-4 py-3 text-muted-foreground hidden md:table-cell">
                      {log.sentAt ? new Date(log.sentAt).toLocaleString() : "—"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {(pageData?.totalPages ?? 0) > 1 && (
            <div className="mt-6">
              <Pagination page={page} totalPages={pageData.totalPages} onPageChange={onPageChange} />
            </div>
          )}
        </>
      )}
    </div>
  );
}
