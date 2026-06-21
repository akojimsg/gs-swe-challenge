/*
 * Reusable Loading / Empty / Error states — every page uses these (per the
 * page-state inventory in 01-journeys-and-sitemap.md). Kept intentionally plain;
 * the MCP agent restyles against the design system.
 */
import { Button } from "@/components/ui/Button";

export function Spinner({ label = "Loading…" }) {
  return (
    <div className="flex items-center justify-center gap-2 py-12 text-muted-foreground">
      <span className="h-4 w-4 animate-spin rounded-full border-2 border-border border-t-foreground" />
      <span className="text-sm">{label}</span>
    </div>
  );
}

export function EmptyState({ title = "Nothing here", message, action }) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 py-16 text-center">
      <p className="font-medium">{title}</p>
      {message && <p className="text-sm text-muted-foreground">{message}</p>}
      {action}
    </div>
  );
}

export function ErrorState({ message = "Something went wrong", onRetry }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-16 text-center">
      <p className="text-sm text-danger">{message}</p>
      {onRetry && (
        <Button variant="outline" size="sm" onClick={onRetry}>
          Retry
        </Button>
      )}
    </div>
  );
}
