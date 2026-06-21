import { useEffect, useState } from "react";
import { useNavigate, useParams, Link } from "react-router-dom";
import { getProduct, getCategories, createProduct, replaceProduct } from "@/api/products";
import { Button } from "@/components/ui/Button";
import { Spinner, ErrorState } from "@/components/common/States";
import { useToast } from "@/components/common/Toast";
import { ChevronLeft } from "lucide-react";

const EMPTY = {
  name: "", sku: "", description: "", price: "", stock: "",
  weightKg: "", active: true, categoryId: "",
};

export default function AdminProductForm({ mode }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const toast = useToast();

  const [form, setForm] = useState(EMPTY);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(mode === "edit");
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState(null);

  useEffect(() => {
    getCategories()
      .then((cats) => setCategories(Array.isArray(cats) ? cats : cats?.content ?? []))
      .catch(() => {});

    if (mode === "edit") {
      getProduct(id)
        .then((p) => {
          setForm({
            name: p.name ?? "",
            sku: p.sku ?? "",
            description: p.description ?? "",
            price: String(p.price ?? ""),
            stock: String(p.stock ?? ""),
            weightKg: p.weightKg != null ? String(p.weightKg) : "",
            active: p.active ?? true,
            categoryId: p.categoryId != null ? String(p.categoryId) : "",
          });
        })
        .catch((e) => setError(e))
        .finally(() => setLoading(false));
    }
  }, [id, mode]);

  const onChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm((f) => ({ ...f, [name]: type === "checkbox" ? checked : value }));
    setErrors((er) => ({ ...er, [name]: undefined }));
  };

  const validate = () => {
    const errs = {};
    if (!form.name.trim()) errs.name = "Required";
    if (!form.sku.trim()) errs.sku = "Required";
    const price = Number(form.price);
    if (isNaN(price) || price < 0) errs.price = "Enter a valid non-negative number";
    const stock = Number(form.stock);
    if (!Number.isInteger(stock) || stock < 0) errs.stock = "Enter a valid non-negative integer";
    return errs;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length) { setErrors(errs); return; }

    setSaving(true);
    const body = {
      name: form.name.trim(),
      sku: form.sku.trim(),
      description: form.description.trim() || null,
      price: Number(form.price),
      stock: Number(form.stock),
      weightKg: form.weightKg ? Number(form.weightKg) : null,
      active: form.active,
      categoryId: form.categoryId ? Number(form.categoryId) : null,
    };

    try {
      if (mode === "create") {
        await createProduct(body);
        toast?.("Product created.");
      } else {
        await replaceProduct(id, body);
        toast?.("Product updated.");
      }
      navigate("/admin/products");
    } catch (err) {
      const msg = err?.body?.message ?? "Couldn't save product.";
      toast?.(msg, "error");
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <Spinner label="Loading product…" />;
  if (error) return <ErrorState message="Couldn't load product." />;

  const inputClass = "w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring disabled:opacity-50";

  const field = (name, label, opts = {}) => (
    <div>
      <label htmlFor={name} className="mb-1 block text-sm font-medium">{label}</label>
      <input
        id={name}
        name={name}
        value={form[name]}
        onChange={onChange}
        disabled={saving}
        className={inputClass}
        {...opts}
      />
      {errors[name] && <p className="mt-1 text-xs text-danger">{errors[name]}</p>}
    </div>
  );

  return (
    <div className="mx-auto max-w-xl">
      <div className="mb-6 flex items-center gap-2">
        <Button asChild variant="ghost" size="icon">
          <Link to="/admin/products" aria-label="Back">
            <ChevronLeft className="h-5 w-5" />
          </Link>
        </Button>
        <h1 className="text-xl font-bold">
          {mode === "create" ? "Add product" : "Edit product"}
        </h1>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        {field("name", "Name *", { placeholder: "Product name" })}
        {field("sku", "SKU *", { placeholder: "PROD-001", disabled: mode === "edit" || saving })}

        <div>
          <label htmlFor="description" className="mb-1 block text-sm font-medium">Description</label>
          <textarea
            id="description"
            name="description"
            value={form.description}
            onChange={onChange}
            disabled={saving}
            rows={3}
            className={inputClass + " resize-none"}
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          {field("price", "Price *", { type: "number", min: "0", step: "0.01", placeholder: "29.99" })}
          {field("stock", "Stock *", { type: "number", min: "0", step: "1", placeholder: "100" })}
        </div>

        <div className="grid grid-cols-2 gap-4">
          {field("weightKg", "Weight (kg)", { type: "number", min: "0", step: "0.001", placeholder: "1.2" })}
          <div>
            <label htmlFor="categoryId" className="mb-1 block text-sm font-medium">Category</label>
            <select
              id="categoryId"
              name="categoryId"
              value={form.categoryId}
              onChange={onChange}
              disabled={saving}
              className={inputClass}
            >
              <option value="">— None —</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </div>
        </div>

        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            name="active"
            checked={form.active}
            onChange={onChange}
            disabled={saving}
            className="accent-brand"
          />
          Active (visible in the store)
        </label>

        <div className="flex gap-3 pt-2">
          <Button type="submit" disabled={saving} className="flex-1">
            {saving ? "Saving…" : mode === "create" ? "Create product" : "Save changes"}
          </Button>
          <Button asChild variant="outline">
            <Link to="/admin/products">Cancel</Link>
          </Button>
        </div>
      </form>
    </div>
  );
}
