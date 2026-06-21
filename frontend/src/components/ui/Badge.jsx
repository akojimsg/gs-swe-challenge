/*
 * Minimal Badge primitive. `tone` maps to the semantic status/stock colors
 * (constants.js). Used by OrderStatusBadge and StockBadge.
 */
import { cn } from "@/lib/cn";

const TONE = {
  neutral: "bg-muted text-muted-foreground",
  brand: "bg-brand/15 text-brand",
  info: "bg-info/15 text-info",
  warning: "bg-warning/20 text-[hsl(45_90%_30%)]",
  success: "bg-success/15 text-success",
  danger: "bg-danger/15 text-danger",
};

export function Badge({ tone = "neutral", className, ...props }) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-md px-2 py-0.5 text-xs font-medium",
        TONE[tone] || TONE.neutral,
        className,
      )}
      {...props}
    />
  );
}
