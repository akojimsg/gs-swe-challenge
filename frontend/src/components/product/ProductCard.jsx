import { Link } from "react-router-dom";
import { ShoppingCart } from "lucide-react";
import { useCartStore } from "@/store/cart";
import { StockBadge } from "./StockBadge";
import ProductImage from "./ProductImage";
import { Button } from "@/components/ui/Button";
import { formatPrice } from "@/lib/format";
import { stockLevel } from "@/lib/constants";
import { cn } from "@/lib/cn";

export function ProductCard({ product, categoryLabel }) {
  const addItem = useCartStore((s) => s.addItem);
  const level = stockLevel(product.stock);

  const handleAddToCart = (e) => {
    e.preventDefault();
    if (!level.inStock) return;
    addItem({
      productId: product.id,
      name: product.name,
      sku: product.sku,
      price: product.price,
      stock: product.stock,
      imageUrl: product.imageUrl,
    });
  };

  return (
    <Link
      to={`/products/${product.id}`}
      className="group flex flex-col rounded-lg border border-border bg-card transition hover:shadow-md"
    >
      <ProductImage size="card" product={product} className="rounded-b-none" />

      <div className="flex flex-1 flex-col gap-2 p-3">
        {categoryLabel && (
          <span className="text-[11px] uppercase tracking-wide text-muted-foreground">
            {categoryLabel}
          </span>
        )}

        <p className="line-clamp-2 text-sm font-medium leading-snug group-hover:text-brand">
          {product.name}
        </p>

        <div className="mt-auto flex items-end justify-between gap-2">
          <span className="text-base font-bold text-brand">{formatPrice(product.price)}</span>
          <StockBadge stock={product.stock} />
        </div>

        <Button
          variant="default"
          size="sm"
          className={cn("w-full gap-1.5 text-xs")}
          onClick={handleAddToCart}
          disabled={!level.inStock}
          aria-label={level.inStock ? `Add ${product.name} to cart` : "Out of stock"}
        >
          <ShoppingCart className="h-3.5 w-3.5" />
          {level.inStock ? "Add to cart" : "Out of stock"}
        </Button>
      </div>
    </Link>
  );
}

export function ProductCardSkeleton() {
  return (
    <div className="flex flex-col rounded-lg border border-border">
      <div className="h-48 animate-pulse rounded-t-lg bg-muted" />
      <div className="flex flex-col gap-2 p-3">
        <div className="h-3 w-1/3 animate-pulse rounded bg-muted" />
        <div className="h-4 w-full animate-pulse rounded bg-muted" />
        <div className="h-4 w-3/4 animate-pulse rounded bg-muted" />
        <div className="mt-2 h-9 animate-pulse rounded bg-muted" />
      </div>
    </div>
  );
}
