import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getProducts, getCategories, deleteProduct } from "@/api/products";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { Spinner, ErrorState, EmptyState } from "@/components/common/States";
import { StockBadge } from "@/components/product/StockBadge";
import { Pagination } from "@/components/common/Pagination";
import { useToast } from "@/components/common/Toast";
import { formatPrice } from "@/lib/format";
import { Plus, Pencil, Trash2, Upload } from "lucide-react";

export default function AdminProducts() {
  const [page, setPage] = useState(null);
  const [categoryMap, setCategoryMap] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [deleting, setDeleting] = useState(null);
  const toast = useToast();

  const load = (p = 0) => {
    setLoading(true);
    setError(null);
    getProducts({ page: p, size: 20 })
      .then((data) => { setPage(data); setCurrentPage(p); })
      .catch((e) => setError(e))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    getCategories()
      .then((cats) => {
        const list = Array.isArray(cats) ? cats : cats?.content ?? [];
        const map = {};
        list.forEach((c) => { map[c.id] = c.name; });
        setCategoryMap(map);
      })
      .catch(() => {});
    load(0);
  }, []);

  const handleDelete = async (product) => {
    if (!confirm(`Delete "${product.name}"? This cannot be undone.`)) return;
    setDeleting(product.id);
    try {
      await deleteProduct(product.id);
      toast?.(`"${product.name}" deleted.`);
      load(currentPage);
    } catch {
      toast?.("Couldn't delete product.", "error");
    } finally {
      setDeleting(null);
    }
  };

  if (loading && !page) return <Spinner label="Loading products…" />;
  if (error) return <ErrorState message="Couldn't load products." onRetry={() => load(currentPage)} />;

  const items = page?.content ?? [];

  return (
    <div>
      {/* header */}
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold">Products</h1>
          {page && (
            <p className="text-sm text-muted-foreground">{page.totalElements} total</p>
          )}
        </div>
        <div className="flex gap-2">
          <Button asChild variant="outline" size="sm">
            <Link to="/admin/import">
              <Upload className="h-4 w-4" /> CSV Import
            </Link>
          </Button>
          <Button asChild size="sm">
            <Link to="/admin/products/new">
              <Plus className="h-4 w-4" /> Add product
            </Link>
          </Button>
        </div>
      </div>

      {items.length === 0 && !loading ? (
        <EmptyState
          title="No products yet"
          message="Create your first product or import via CSV."
          action={
            <Button asChild>
              <Link to="/admin/products/new">Add product</Link>
            </Button>
          }
        />
      ) : (
        <>
          <div className="overflow-x-auto rounded-xl border border-border">
            <table className="w-full text-sm">
              <thead className="border-b border-border bg-muted/50">
                <tr>
                  <th className="px-4 py-3 text-left font-medium">Name</th>
                  <th className="px-4 py-3 text-left font-medium">SKU</th>
                  <th className="px-4 py-3 text-left font-medium hidden sm:table-cell">Category</th>
                  <th className="px-4 py-3 text-right font-medium">Price</th>
                  <th className="px-4 py-3 text-left font-medium hidden md:table-cell">Stock</th>
                  <th className="px-4 py-3 text-left font-medium hidden lg:table-cell">Status</th>
                  <th className="px-4 py-3 text-right font-medium">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {items.map((p) => (
                  <tr key={p.id} className="hover:bg-muted/30 transition">
                    <td className="px-4 py-3 font-medium max-w-[180px] truncate">
                      {p.name}
                    </td>
                    <td className="px-4 py-3 font-mono text-xs text-muted-foreground">
                      {p.sku}
                    </td>
                    <td className="px-4 py-3 text-muted-foreground hidden sm:table-cell">
                      {categoryMap[p.categoryId] ?? "—"}
                    </td>
                    <td className="px-4 py-3 text-right font-medium text-brand">
                      {formatPrice(p.price)}
                    </td>
                    <td className="px-4 py-3 hidden md:table-cell">
                      <StockBadge stock={p.stock} />
                    </td>
                    <td className="px-4 py-3 hidden lg:table-cell">
                      <Badge tone={p.active ? "success" : "neutral"}>
                        {p.active ? "Active" : "Inactive"}
                      </Badge>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1">
                        <Button asChild variant="ghost" size="icon" aria-label={`Edit ${p.name}`}>
                          <Link to={`/admin/products/${p.id}/edit`}>
                            <Pencil className="h-4 w-4" />
                          </Link>
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label={`Delete ${p.name}`}
                          onClick={() => handleDelete(p)}
                          disabled={deleting === p.id}
                          className="hover:text-danger"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {(page?.totalPages ?? 0) > 1 && (
            <div className="mt-6">
              <Pagination
                page={currentPage}
                totalPages={page.totalPages}
                onPageChange={load}
              />
            </div>
          )}
        </>
      )}
    </div>
  );
}
