/*
 * PAGE: Product Detail (PDP)
 * ---------------------------------------------------------------------------
 * Route:  /products/:id  ·  Access: Public · Scope: core  ·  Shell: StorefrontShell
 * Figma:  node 1:17310  (file eK34KAtvQCGRw6QPc1jzPU)
 * Spec:   gse-requirement-docs/frontend-design/specs/product-detail.md
 *
 * API: getProduct(id) → { id,name,sku,description,categoryId,price,stock,weightKg,active }
 *      getCategories() → resolve categoryId → label.  404 when missing/inactive.
 * STATES: loading (skeleton) · success (gallery + buy box) · 404 · out-of-stock
 *      (disable add-to-cart) · error.
 * INTERACTIONS: qty stepper clamps 1..stock · add-to-cart → cart store (snapshot
 *      productId,name,sku,price,qty,stock) · price "Free" at 0.
 * BUILD NOTE (MCP): pull 1:17310 → ProductGallery + buy box + QuantityStepper +
 *      StockBadge from the design system. Wiring below is the contract.
 * ---------------------------------------------------------------------------
 */
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { getProduct } from "@/api/products";
import { useCartStore } from "@/store/cart";
import { Spinner, ErrorState } from "@/components/common/States";
import { Button } from "@/components/ui/Button";
import { formatPrice } from "@/lib/format";
import { stockLevel } from "@/lib/constants";

export default function ProductDetail() {
  const { id } = useParams();
  const [product, setProduct] = useState(null);
  const [status, setStatus] = useState("loading"); // loading | ok | notfound | error
  const addItem = useCartStore((s) => s.addItem);

  useEffect(() => {
    let active = true;
    setStatus("loading");
    getProduct(id)
      .then((p) => active && (setProduct(p), setStatus("ok")))
      .catch((e) => active && setStatus(e?.status === 404 ? "notfound" : "error"));
    return () => {
      active = false;
    };
  }, [id]);

  if (status === "loading") return <Spinner />;
  if (status === "notfound") return <ErrorState message="Product not found." />;
  if (status === "error") return <ErrorState message="Couldn't load product." />;

  const stock = stockLevel(product.stock);
  // Minimal proof-of-wiring render — MCP agent implements Figma 1:17310.
  return (
    <div className="max-w-xl">
      <h1 className="font-display text-2xl font-extrabold">{product.name}</h1>
      <p className="my-2 text-xl text-brand">{formatPrice(product.price)}</p>
      <p className="text-sm text-muted-foreground">{stock.label} · SKU {product.sku}</p>
      <p className="my-4">{product.description}</p>
      <Button
        disabled={!stock.inStock}
        onClick={() =>
          addItem({
            productId: product.id,
            name: product.name,
            sku: product.sku,
            price: product.price,
            stock: product.stock,
          })
        }
      >
        {stock.inStock ? "Add to cart" : "Out of stock"}
      </Button>
    </div>
  );
}
