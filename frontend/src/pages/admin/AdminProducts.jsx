/*
 * PAGE: Admin Product List
 * ---------------------------------------------------------------------------
 * Route:  /admin/products  ·  Access: Admin · Scope: core  ·  Shell: AdminShell
 * Spec:   gse-requirement-docs/frontend-design/specs/admin-products.md
 *
 * API: getProducts({q,category,page,size,sort}) (admin view incl. inactive) ·
 *      deleteProduct(id) [ADMIN].
 * STATES: loading (skeleton rows) · empty · error · delete-confirm (AlertDialog).
 * INTERACTIONS: search/filter/sort/paginate (URL) · edit → form · delete → confirm
 *      → DELETE → refresh + toast · stock + status badges.
 * BUILD NOTE (MCP): AdminProductTable (shadcn Table + DropdownMenu) + ConfirmDialog.
 * ---------------------------------------------------------------------------
 */
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getProducts, deleteProduct } from "@/api/products";
import { Button } from "@/components/ui/Button";
import { Spinner, EmptyState, ErrorState } from "@/components/common/States";

export default function AdminProducts() {
  const [page, setPage] = useState(null);
  const [state, setState] = useState("loading");
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;
    setState("loading");
    getProducts({ size: 50, sort: "name,asc" })
      .then((d) => active && (setPage(d), setState("ok")))
      .catch(() => active && setState("error"));
    return () => {
      active = false;
    };
  }, [reloadKey]);

  const onDelete = async (id) => {
    await deleteProduct(id);
    setReloadKey((k) => k + 1);
  };

  if (state === "loading") return <Spinner />;
  if (state === "error") return <ErrorState onRetry={() => setReloadKey((k) => k + 1)} />;
  const items = page?.content ?? [];
  if (items.length === 0)
    return (
      <EmptyState
        title="No products yet"
        action={<Link to="/admin/products/new"><Button>New product</Button></Link>}
      />
    );

  // Minimal proof-of-wiring render — MCP agent implements AdminProductTable.
  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="font-display text-xl font-extrabold">Products</h1>
        <Link to="/admin/products/new"><Button>New product</Button></Link>
      </div>
      <table className="w-full text-left text-sm">
        <thead className="text-muted-foreground">
          <tr><th className="py-2">Name</th><th>SKU</th><th>Price</th><th>Stock</th><th /></tr>
        </thead>
        <tbody>
          {items.map((p) => (
            <tr key={p.id} className="border-t border-border">
              <td className="py-2">{p.name}</td>
              <td>{p.sku}</td>
              <td>{p.price}</td>
              <td>{p.stock}</td>
              <td className="text-right">
                <Link to={`/admin/products/${p.id}/edit`} className="text-brand">Edit</Link>{" "}
                <button onClick={() => onDelete(p.id)} className="text-danger">Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
