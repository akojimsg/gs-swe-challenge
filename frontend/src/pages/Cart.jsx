import { Link, useNavigate } from "react-router-dom";
import { Trash2 } from "lucide-react";
import { useCartStore } from "@/store/cart";
import { useAuthStore } from "@/store/auth";
import { QuantityStepper } from "@/components/product/QuantityStepper";
import ProductImage from "@/components/product/ProductImage";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/common/States";
import { formatPrice, formatMoney } from "@/lib/format";

function CartLineItem({ item }) {
  const { setQty, removeItem } = useCartStore();
  const lineTotal = item.price * item.qty;

  return (
    <div className="flex gap-4 border-b border-border py-4">
      <ProductImage
        size="thumb"
        product={{ id: item.productId, name: item.name, imageUrl: item.imageUrl }}
        className="h-20 w-20 shrink-0"
      />

      <div className="flex flex-1 flex-col gap-1 min-w-0">
        <Link
          to={`/products/${item.productId}`}
          className="line-clamp-2 text-sm font-medium hover:text-brand"
        >
          {item.name}
        </Link>
        <p className="text-xs text-muted-foreground">SKU: {item.sku}</p>
        <p className="text-sm font-medium text-brand">{formatPrice(item.price)}</p>
      </div>

      <div className="flex shrink-0 flex-col items-end gap-2">
        <p className="text-sm font-semibold">{formatMoney(lineTotal)}</p>
        <QuantityStepper
          value={item.qty}
          min={1}
          max={item.stock ?? 9999}
          onChange={(q) => setQty(item.productId, q)}
        />
        <button
          onClick={() => removeItem(item.productId)}
          aria-label={`Remove ${item.name}`}
          className="flex items-center gap-1 text-xs text-muted-foreground hover:text-danger"
        >
          <Trash2 className="h-3.5 w-3.5" /> Remove
        </button>
      </div>
    </div>
  );
}

function CartSummary({ subtotal, onCheckout }) {
  return (
    <div className="rounded-xl border border-border bg-card p-5 lg:sticky lg:top-24">
      <h2 className="mb-4 text-base font-semibold">Order summary</h2>
      <div className="flex justify-between text-sm">
        <span className="text-muted-foreground">Subtotal</span>
        <span>{formatMoney(subtotal)}</span>
      </div>
      <div className="my-3 flex justify-between text-sm text-muted-foreground">
        <span>Shipping</span>
        <span className="text-success text-xs font-medium">Free</span>
      </div>
      <div className="border-t border-border pt-3">
        <div className="flex justify-between font-semibold">
          <span>Total</span>
          <span className="text-brand">{formatMoney(subtotal)}</span>
        </div>
      </div>
      <Button size="lg" className="mt-4 w-full" onClick={onCheckout}>
        Proceed to checkout
      </Button>
      <Link
        to="/products"
        className="mt-2 block text-center text-sm text-muted-foreground hover:text-brand"
      >
        Continue shopping
      </Link>
    </div>
  );
}

export default function Cart() {
  const items = useCartStore((s) => s.items);
  const subtotal = useCartStore((s) => s.subtotal());
  const isAuthed = useAuthStore((s) => !!s.accessToken);
  const navigate = useNavigate();

  const handleCheckout = () => {
    if (isAuthed) {
      navigate("/checkout");
    } else {
      navigate("/login?redirect=/checkout");
    }
  };

  if (items.length === 0) {
    return (
      <EmptyState
        title="Your cart is empty"
        message="Browse our products and add some items."
        action={
          <Button asChild>
            <Link to="/products">Browse products</Link>
          </Button>
        }
      />
    );
  }

  return (
    <div>
      <h1 className="mb-6 font-display text-2xl font-extrabold">Shopping Cart</h1>
      <div className="grid gap-6 lg:grid-cols-3">
        {/* line items */}
        <div className="lg:col-span-2">
          {items.map((item) => (
            <CartLineItem key={item.productId} item={item} />
          ))}
        </div>

        {/* summary */}
        <CartSummary subtotal={subtotal} onCheckout={handleCheckout} />
      </div>
    </div>
  );
}
