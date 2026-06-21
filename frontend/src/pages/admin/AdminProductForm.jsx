/*
 * PAGE: Admin Product Form (create / edit)
 * ---------------------------------------------------------------------------
 * Routes: /admin/products/new · /admin/products/:id/edit  ·  Admin · core
 * Shell:  AdminShell  ·  Spec: specs/admin-product-form.md
 *
 * API: getProduct(id) (edit preload) · createProduct(body) → 201 ·
 *      updateProduct(id, body) (PATCH) / replaceProduct (PUT).
 * FIELDS: name·sku·description·category(select from /categories)·price·stock·
 *      weightKg(opt)·active. VALIDATION (mirror server): price≥0, stock≥0,
 *      required name/sku/price, 409 dup SKU → field error.
 * STOCK (ADR-015): on edit treat stock as an explicit set, not a blind overwrite.
 * BUILD NOTE (MCP): ProductForm (shadcn form/input/select/switch + zod).
 * ---------------------------------------------------------------------------
 */
import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { getProduct, createProduct, updateProduct } from "@/api/products";
import { ApiError } from "@/api/client";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/common/States";

export default function AdminProductForm({ mode = "create" }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const [initial, setInitial] = useState(mode === "edit" ? null : {});
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (mode === "edit") getProduct(id).then(setInitial).catch(() => setError("Couldn't load product."));
  }, [mode, id]);

  if (mode === "edit" && !initial) return <Spinner />;

  const onSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    const f = new FormData(e.currentTarget);
    const body = {
      name: f.get("name"),
      sku: f.get("sku"),
      description: f.get("description"),
      price: Number(f.get("price")),
      stock: Number(f.get("stock")),
    };
    try {
      if (mode === "create") await createProduct(body);
      else await updateProduct(id, body);
      navigate("/admin/products");
    } catch (err) {
      setError(err instanceof ApiError && err.status === 409 ? "SKU already exists." : "Save failed.");
      setSubmitting(false);
    }
  };

  // Minimal proof-of-wiring render — MCP agent implements ProductForm.
  return (
    <form onSubmit={onSubmit} className="max-w-lg space-y-3">
      <h1 className="font-display text-xl font-extrabold">
        {mode === "create" ? "New product" : "Edit product"}
      </h1>
      {error && <p className="text-sm text-danger">{error}</p>}
      <input name="name" required defaultValue={initial?.name} placeholder="Name" className="w-full rounded-md border border-input px-3 py-2" />
      <input name="sku" required defaultValue={initial?.sku} placeholder="SKU" className="w-full rounded-md border border-input px-3 py-2" />
      <textarea name="description" defaultValue={initial?.description} placeholder="Description" className="w-full rounded-md border border-input px-3 py-2" />
      <input name="price" type="number" min={0} step="0.01" required defaultValue={initial?.price} placeholder="Price" className="w-full rounded-md border border-input px-3 py-2" />
      <input name="stock" type="number" min={0} defaultValue={initial?.stock} placeholder="Stock" className="w-full rounded-md border border-input px-3 py-2" />
      <Button type="submit" disabled={submitting}>{submitting ? "Saving…" : "Save"}</Button>
    </form>
  );
}
