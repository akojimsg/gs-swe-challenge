/*
 * Storefront shell — wraps all public/buyer storefront pages.
 * Figma: two-tier header — ① dark utility strip ② main bar (logo · centered
 * search · Sign in / Cart cluster) ③ category nav row (from GET /categories) +
 * footer. See layouts.md "Storefront". Cart count from the cart store.
 *
 * This is a STRUCTURAL stub: regions are present + wired (cart count, auth links,
 * nav to search), styling is minimal. MCP agent implements the Figma header.
 */
import { Link, Outlet, useNavigate } from "react-router-dom";
import { ShoppingCart, User } from "lucide-react";
import { useCartStore } from "@/store/cart";
import { useAuthStore } from "@/store/auth";

export default function StorefrontShell() {
  const count = useCartStore((s) => s.count());
  const isAuthed = useAuthStore((s) => !!s.accessToken);
  const isAdmin = useAuthStore((s) => s.user?.role === "ADMIN");
  const navigate = useNavigate();

  const onSearch = (e) => {
    e.preventDefault();
    const q = new FormData(e.currentTarget).get("q");
    navigate(`/products?q=${encodeURIComponent(q || "")}`);
  };

  return (
    <div className="flex min-h-screen flex-col">
      {/* ① utility strip */}
      <div className="bg-foreground px-4 py-1.5 text-xs text-background md:px-8">
        Free delivery · Track your order · Daily deals
      </div>

      {/* ② main bar */}
      <header className="border-b border-border">
        <div className="mx-auto flex max-w-7xl items-center gap-4 px-4 py-3 md:px-8">
          <Link to="/" className="font-display text-xl font-extrabold">
            United<span className="text-brand">Deals</span>
          </Link>
          <form onSubmit={onSearch} className="flex-1">
            <input
              name="q"
              placeholder="Search products…"
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
              aria-label="Search products"
            />
          </form>
          <nav className="flex items-center gap-4 text-sm">
            {isAdmin && (
              <Link to="/admin/products" className="hover:text-brand">
                Admin
              </Link>
            )}
            <Link to={isAuthed ? "/account" : "/login"} className="flex items-center gap-1 hover:text-brand">
              <User className="h-4 w-4" /> {isAuthed ? "Account" : "Sign in"}
            </Link>
            <Link to="/cart" className="flex items-center gap-1 hover:text-brand" aria-label="Cart">
              <ShoppingCart className="h-4 w-4" />
              <span>{count}</span>
            </Link>
          </nav>
        </div>
        {/* ③ category nav row — MCP agent populates from GET /categories */}
        <div className="border-t border-border px-4 py-2 text-sm text-muted-foreground md:px-8">
          <span className="mx-auto block max-w-7xl">Categories ·{" "}
            <Link to="/products" className="hover:text-brand">All products</Link>
          </span>
        </div>
      </header>

      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-6 md:px-8">
        <Outlet />
      </main>

      <footer className="border-t border-border px-4 py-6 text-sm text-muted-foreground md:px-8">
        <div className="mx-auto max-w-7xl">United Deals — gs-swe-challenge demo storefront.</div>
      </footer>
    </div>
  );
}
