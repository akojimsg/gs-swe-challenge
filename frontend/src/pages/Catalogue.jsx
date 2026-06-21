/*
 * PAGE: Catalogue (storefront — the hottest read path)
 * ---------------------------------------------------------------------------
 * Route:   /products  (also resolves /search?q=…)   ·   Access: Public · Scope: core
 * Shell:   StorefrontShell
 * Figma:   PLP node 1:39935  (file eK34KAtvQCGRw6QPc1jzPU, "United Deals")
 * Spec:    gse-requirement-docs/frontend-design/specs/catalogue.md
 * Mockup:  gse-requirement-docs/frontend-design/mockups/catalogue.html
 *
 * API (api/products.js):
 *   getProducts({ q, category, minPrice, maxPrice, inStock, page, size, sort })
 *     → PagedResponseProductResponse { content[], page, size, totalElements, totalPages }
 *   getCategories() → [{ name, productCount }]  (join categoryId → label)
 *
 * STATES to implement: loading (skeleton grid) · success (grid + pagination +
 *   count) · empty (EmptyState, 200 not 404) · error (ErrorState + retry; Redis
 *   -down still returns data) · rate-limited (429 toast, keep last results).
 *
 * INTERACTIONS: debounce search ~300ms → q, reset page=0 · filters/sort/page are
 *   URL-driven (shareable, back/forward safe) · add-to-cart via cart store; out of
 *   stock disables it · price "Free" at 0 · stock badge in/low/out (constants.js).
 *
 * BUILD NOTE (MCP agent): pull Figma 1:39935, implement against ProductGrid /
 *   ProductCard / FilterPanel / SortControl / Pagination using design-system
 *   tokens. The fetch + URL-param wiring below is the contract; replace the markup.
 * ---------------------------------------------------------------------------
 */
import { useEffect, useState } from "react";
import { useSearchParams, Link } from "react-router-dom";
import { getProducts, getCategories } from "@/api/products";
import { ApiError } from "@/api/client";
import { Spinner, EmptyState, ErrorState } from "@/components/common/States";
import { formatPrice } from "@/lib/format";

export default function Catalogue() {
  const [searchParams] = useSearchParams();
  const [page, setPage] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  // URL params drive the query (catalogue.md). Stub reads them straight through.
  const params = Object.fromEntries(searchParams.entries());

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    getProducts(params)
      .then((data) => active && setPage(data))
      .catch((e) => active && setError(e))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  // Load categories once (filter labels). Result intentionally unused in the stub.
  useEffect(() => {
    getCategories().catch(() => {});
  }, []);

  if (loading) return <Spinner label="Loading products…" />;
  if (error) {
    if (error instanceof ApiError && error.status === 429)
      return <ErrorState message="Slow down a moment — too many requests." />;
    return <ErrorState message="Couldn't load products." onRetry={() => setPage(null)} />;
  }
  const items = page?.content ?? [];
  if (items.length === 0)
    return <EmptyState title="No products match" message="Try adjusting your filters." />;

  // Minimal proof-of-wiring render — MCP agent replaces with the Figma grid.
  return (
    <div>
      <p className="mb-4 text-sm text-muted-foreground">
        Showing {items.length} of {page.totalElements} — STUB view (implement Figma 1:39935)
      </p>
      <ul className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
        {items.map((p) => (
          <li key={p.id} className="rounded-lg border border-border p-3">
            <Link to={`/products/${p.id}`} className="font-medium hover:text-brand">
              {p.name}
            </Link>
            <p className="text-brand">{formatPrice(p.price)}</p>
            <p className="text-xs text-muted-foreground">SKU {p.sku}</p>
          </li>
        ))}
      </ul>
    </div>
  );
}
