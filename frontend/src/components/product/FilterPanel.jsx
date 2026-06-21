import { useState } from "react";
import { ChevronDown, ChevronUp, SlidersHorizontal, X } from "lucide-react";
import { Button } from "@/components/ui/Button";

function AccordionSection({ title, children, defaultOpen = false }) {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <div className="border-b border-border py-3">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        className="flex w-full items-center justify-between text-sm font-medium"
      >
        {title}
        {open ? <ChevronUp className="h-4 w-4 text-muted-foreground" /> : <ChevronDown className="h-4 w-4 text-muted-foreground" />}
      </button>
      {open && <div className="mt-3 space-y-2">{children}</div>}
    </div>
  );
}

export function FilterPanel({ categories = [], params, onApply, onClear }) {
  const [local, setLocal] = useState({
    category: params.category ?? "",
    minPrice: params.minPrice ?? "",
    maxPrice: params.maxPrice ?? "",
    inStock: params.inStock === "true" || params.inStock === true,
  });

  const toggle = (key, value) =>
    setLocal((p) => ({ ...p, [key]: p[key] === value ? "" : value }));

  const apply = () => {
    const out = {};
    if (local.category) out.category = local.category;
    if (local.minPrice) out.minPrice = local.minPrice;
    if (local.maxPrice) out.maxPrice = local.maxPrice;
    if (local.inStock) out.inStock = "true";
    onApply(out);
  };

  const clear = () => {
    setLocal({ category: "", minPrice: "", maxPrice: "", inStock: false });
    onClear();
  };

  const hasActive = local.category || local.minPrice || local.maxPrice || local.inStock;

  return (
    <aside className="w-full space-y-1 lg:w-56 lg:shrink-0">
      <div className="flex items-center justify-between pb-2">
        <span className="flex items-center gap-1.5 text-sm font-semibold">
          <SlidersHorizontal className="h-4 w-4" /> Filters
        </span>
        {hasActive && (
          <button onClick={clear} className="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground">
            <X className="h-3 w-3" /> Clear
          </button>
        )}
      </div>

      {categories.length > 0 && (
        <AccordionSection title="Category" defaultOpen>
          {categories.map((cat) => (
            <label key={cat.name} className="flex cursor-pointer items-center gap-2 text-sm">
              <input
                type="radio"
                name="category"
                checked={local.category === cat.name}
                onChange={() => toggle("category", cat.name)}
                className="accent-brand"
              />
              <span className="flex-1">{cat.name}</span>
              {cat.productCount != null && (
                <span className="text-xs text-muted-foreground">({cat.productCount})</span>
              )}
            </label>
          ))}
        </AccordionSection>
      )}

      <AccordionSection title="Price range" defaultOpen>
        <div className="flex items-center gap-2">
          <input
            type="number"
            min="0"
            placeholder="Min"
            value={local.minPrice}
            onChange={(e) => setLocal((p) => ({ ...p, minPrice: e.target.value }))}
            className="w-full rounded-md border border-input px-2 py-1.5 text-sm"
          />
          <span className="text-muted-foreground">–</span>
          <input
            type="number"
            min="0"
            placeholder="Max"
            value={local.maxPrice}
            onChange={(e) => setLocal((p) => ({ ...p, maxPrice: e.target.value }))}
            className="w-full rounded-md border border-input px-2 py-1.5 text-sm"
          />
        </div>
      </AccordionSection>

      <AccordionSection title="Availability" defaultOpen>
        <label className="flex cursor-pointer items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={local.inStock}
            onChange={(e) => setLocal((p) => ({ ...p, inStock: e.target.checked }))}
            className="accent-brand"
          />
          In stock only
        </label>
      </AccordionSection>

      <div className="pt-3">
        <Button onClick={apply} size="sm" className="w-full">
          Apply filters
        </Button>
      </div>
    </aside>
  );
}
