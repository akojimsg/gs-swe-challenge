import { useEffect, useState } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { getProduct, getCategories, getProducts } from "@/api/products";
import { useCartStore } from "@/store/cart";
import { useToast } from "@/components/common/Toast";
import { StockBadge } from "@/components/product/StockBadge";
import { QuantityStepper } from "@/components/product/QuantityStepper";
import { ProductCard } from "@/components/product/ProductCard";
import ProductImage from "@/components/product/ProductImage";
import { Button } from "@/components/ui/Button";
import { Spinner, ErrorState } from "@/components/common/States";
import { formatPrice } from "@/lib/format";
import { stockLevel } from "@/lib/constants";
import { ShoppingCart } from "lucide-react";

export default function ProductDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const addItem = useCartStore((s) => s.addItem);
  const toast = useToast();

  const [product, setProduct] = useState(null);
  const [categoryName, setCategoryName] = useState(null);
  const [related, setRelated] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [qty, setQty] = useState(1);
  const [added, setAdded] = useState(false);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    setQty(1);
    setAdded(false);

    getProduct(id)
      .then(async (p) => {
        if (!active) return;
        setProduct(p);
        if (p.categoryId) {
          try {
            const cats = await getCategories();
            const list = Array.isArray(cats) ? cats : cats?.content ?? [];
            const cat = list.find((c) => c.id === p.categoryId);
            if (active) setCategoryName(cat?.name ?? null);
            if (cat && active) {
              getProducts({ category: cat.name, size: 4, page: 0 })
                .then((res) => {
                  if (active) {
                    const items = (res?.content ?? []).filter((r) => r.id !== p.id);
                    setRelated(items.slice(0, 4));
                  }
                })
                .catch(() => {});
            }
          } catch { /* related products are best-effort */ }
        }
      })
      .catch((e) => {
        if (!active) return;
        if (e?.status === 404) navigate("/404", { replace: true });
        else setError(e);
      })
      .finally(() => { if (active) setLoading(false); });

    return () => { active = false; };
  }, [id, navigate]);

  const handleAddToCart = () => {
    if (!product) return;
    addItem({
      productId: product.id,
      name: product.name,
      sku: product.sku,
      price: product.price,
      stock: product.stock,
      imageUrl: product.imageUrl,
    }, qty);
    setAdded(true);
    toast?.(`${product.name} added to cart`);
    setTimeout(() => setAdded(false), 2000);
  };

  if (loading) return <Spinner label="Loading product…" />;
  if (error) return <ErrorState message="Couldn't load product." onRetry={() => setLoading(true)} />;
  if (!product) return null;

  const level = stockLevel(product.stock);

  return (
    <div>
      {/* breadcrumb */}
      <nav className="mb-4 flex items-center gap-1.5 text-xs text-muted-foreground">
        <Link to="/" className="hover:text-brand">Home</Link>
        <span>/</span>
        <Link to="/products" className="hover:text-brand">Shop</Link>
        {categoryName && (
          <>
            <span>/</span>
            <Link
              to={`/products?category=${encodeURIComponent(categoryName)}`}
              className="hover:text-brand"
            >
              {categoryName}
            </Link>
          </>
        )}
        <span>/</span>
        <span className="text-foreground line-clamp-1">{product.name}</span>
      </nav>

      {/* two-column layout */}
      <div className="grid gap-8 lg:grid-cols-2">
        <ProductImage size="hero" product={product} className="border border-border" />

        {/* buy box */}
        <div className="flex flex-col gap-4">
          {categoryName && (
            <Link
              to={`/products?category=${encodeURIComponent(categoryName)}`}
              className="text-xs uppercase tracking-wide text-muted-foreground hover:text-brand"
            >
              {categoryName}
            </Link>
          )}

          <h1 className="font-display text-2xl font-extrabold leading-tight sm:text-3xl">
            {product.name}
          </h1>

          <div className="flex items-center gap-3">
            <span className="text-3xl font-bold text-brand">{formatPrice(product.price)}</span>
            <StockBadge stock={product.stock} />
          </div>

          {product.weightKg && (
            <p className="text-sm text-muted-foreground">Weight: {product.weightKg} kg</p>
          )}

          {level.inStock && (
            <div className="flex items-center gap-3">
              <span className="text-sm text-muted-foreground">Qty:</span>
              <QuantityStepper
                value={qty}
                min={1}
                max={product.stock}
                onChange={setQty}
              />
            </div>
          )}

          <Button
            onClick={handleAddToCart}
            disabled={!level.inStock}
            className="gap-2"
            size="lg"
            variant={added ? "brand" : "default"}
          >
            <ShoppingCart className="h-5 w-5" />
            {added ? "Added!" : level.inStock ? "Add to cart" : "Out of stock"}
          </Button>

          <p className="text-xs text-muted-foreground">SKU: {product.sku}</p>
        </div>
      </div>

      {/* description */}
      {product.description && (
        <section className="mt-10">
          <h2 className="mb-2 text-lg font-semibold">Product details</h2>
          <p className="text-sm leading-relaxed text-muted-foreground">{product.description}</p>
        </section>
      )}

      {/* related products */}
      {related.length > 0 && (
        <section className="mt-10">
          <h2 className="mb-4 text-lg font-semibold">Related products</h2>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
            {related.map((p) => (
              <ProductCard key={p.id} product={p} categoryLabel={categoryName} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
