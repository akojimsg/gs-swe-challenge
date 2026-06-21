import { Badge } from "@/components/ui/Badge";
import { stockLevel } from "@/lib/constants";

export function StockBadge({ stock }) {
  const level = stockLevel(stock);
  return <Badge tone={level.color}>{level.label}</Badge>;
}
