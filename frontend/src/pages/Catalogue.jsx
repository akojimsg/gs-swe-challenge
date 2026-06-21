import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { getProducts, getCategories } from "@/api/products";
import { ApiError } from "@/api/client";
import { ProductCard, ProductCardSkeleton } from "@/components/product/ProductCard";
import { FilterPanel } from "@/components/product/FilterPanel";
import { Pagination } from "@/components/common/Pagination";
import { EmptyState, ErrorState } from "@/components/common/States";
import { useToast } from "@/components/common/Toast";
import { LayoutGrid, LayoutList } from "lucide-react";
import { cn } from "@/lib/cn";

const SORT_OPTIONS = [
  { value: "", label: "Relevance" },
  { value: "price,asc", label: "Price: low to high" },
  { value: "price,desc", label: "Price: high to low" },
  { value: "name,asc", label: "Name A–Z" },
  { value: "createdAt,desc", label: "Newest" },
];
const SIZE_OPTIONS = [12, 24, 48];
const DEBOUNCE_MS = 300;

export default function Catalogue() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [page, setPage] = useState(null);
  const [categories, setCategories] = useState([]);
  const [categoryMap, setCategoryMap] = useState({});
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [viewMode, setViewMode] = useState("grid");
  const toast = useToast();
  const debounceRef = useRef(null);

  const params = useMemo(() => Object.fromEntries(searchParams.entries()), [searchParams]);

  useEffect(() => {
    getCategories()
      .then((cats) => {
        const list = Array.isArray(cats) ? cats : cats?.content ?? [];
        setCategories(list);
        const map = {};
        list.forEach((c) => { map[c.id] = c.name; });
        setCategoryMap(map);
      })
      .catch(() => {});
  }, []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);

    getProducts(params)
      .then((data) => { if (active) setPage(data); })
      .catch((e) => {
        if (!active) return;
        if (e instanceof ApiError && e.status === 429) {
          toast?.("Slow down — too many requests. Showing cached results.");
        } else {
          setError(e);
        }
      })
      .finally(() => { if (active) setLoading(false); });

    return () => { active = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  const updateParam = useCallback((updates) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      Object.entries(updates).forEach(([k, v]) => {
        if (v == null || v === "") next.delete(k);
        else next.set(k, String(v));
      });
      return next;
    });
  }, [setSearchParams]);

  const handleSearch = (e) => {
    const q = e.target.value;
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      updateParam({ q: q || null, page: null });
    }, DEBOUNCE_MS);
  };

  const handleSort = (e) => updateParam({ sort: e.target.value || null, page: null });
  const handleSize = (e) => updateParam({ size: e.target.value, page: null });
  const handlePage = (p) => {
    updateParam({ page: p === 0 ? null : p });
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const handleApplyFilters = (filters) => updateParam({ ...filters, page: null });
  const handleClearFilters = () => {
    setSearchParams((prev) => {
      const next = new URLSearchParams();
      ["q", "sort", "size"].forEach((k) => {
        if (prev.get(k)) next.set(k, prev.get(k));
      });
      return next;
    });
  };

  const items = page?.content ?? [];
  const currentPage = Number(params.page ?? 0);
  const totalPages = page?.totalPages ?? 0;
  const totalElements = page?.totalElements ?? 0;
  const pageSize = Number(params.size ?? 24);
  const firstIdx = currentPage * pageSize + 1;
  const lastIdx = Math.min(firstIdx + items.length - 1, totalElements);

  return (
    <div>
      <div className="mb-4">
        <input
          defaultValue={params.q ?? ""}
          onChange={handleSearch}
          placeholder="Search products…"
          className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring sm:max-w-md"
          aria-label="Search products"
        />
      </div>

      <div className="flex flex-col gap-6 lg:flex-row">
        <FilterPanel
          categories={categories}
          params={params}
          onApply={handleApplyFilters}
          onClear={handleClearFilters}
        />

        <div className="min-w-0 flex-1">
          {/* toolbar */}
          <div className="mb-4 flex flex-wrap items-center justify-between gap-2 text-sm">
            <span className="text-muted-foreground">
              {loading
                ? "Loading��"
                : totalElements === 0
                ? "No products found"
                : `Showing ${firstIdx}–${lastIdx} of ${totalElements}`}
            </span>
            <div className="flex items-center gap-2">
              <select
                value={params.size ?? "24"}
                onChange={handleSize}
                className="rounded-md border border-input bg-background px-2 py-1.5 text-sm"
                aria-label="Items per page"
              >
                {SIZE_OPTIONS.map((s) => (
                  <option key={s} value={s}>{s} per page</option>
                ))}
              </select>
              <select
                value={params.sort ?? ""}
                onChange={handleSort}
                className="rounded-md border border-input bg-background px-2 py-1.5 text-sm"
                aria-label="Sort"
              >
                {SORT_OPTIONS.map((o) => (
                  <option key={o.value} value={o.value}>{o.label}</option>
                ))}
              </select>
              <div className="hidden items-center gap-1 sm:flex">
                <button
                  onClick={() => setViewMode("grid")}
                  aria-label="Grid view"
                  className={cn("rounded p-1.5 hover:bg-muted", viewMode === "grid" && "bg-muted text-brand")}
                >
                  <LayoutGrid className="h-4 w-4" />
                </button>
                <button
                  onClick={() => setViewMode("list")}
                  aria-label="List view"
                  className={cn("rounded p-1.5 hover:bg-muted", viewMode === "list" && "bg-muted text-brand")}
                >
                  <LayoutList className="h-4 w-4" />
                </button>
              </div>
            </div>
          </div>

          {error && (
            <ErrorState
              message="Couldn't load products. Please try again."
              onRetry={() => updateParam({ _r: Date.now() })}
            />
          )}

          {!error && loading && (
            <div className={cn(
              "grid gap-4",
              viewMode === "list"
                ? "grid-cols-1"
                : "grid-cols-2 sm:grid-cols-3 lg:grid-cols-4",
            )}>
              {Array.from({ length: pageSize }).map((_, i) => <ProductCardSkeleton key={i} />)}
            </div>
          )}

          {!error && !loading && items.length === 0 && (
            <EmptyState
              title="No products match"
              message="Try adjusting your filters or search term."
            />
          )}

          {!error && !loading && items.length > 0 && (
            <>
              <div className={cn(
                "grid gap-4",
                viewMode === "list"
                  ? "grid-cols-1"
                  : "grid-cols-2 sm:grid-cols-3 lg:grid-cols-4",
              )}>
                {items.map((p) => (
                  <ProductCard
                    key={p.id}
                    product={p}
                    categoryLabel={categoryMap[p.categoryId]}
                  />
                ))}
              </div>

              {totalPages > 1 && (
                <div className="mt-8">
                  <Pagination
                    page={currentPage}
                    totalPages={totalPages}
                    onPageChange={handlePage}
                  />
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
