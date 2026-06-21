import { useEffect, useState } from "react";
import { Link, Outlet, useNavigate, useSearchParams } from "react-router-dom";
import { ShoppingCart, User, LogOut, Settings } from "lucide-react";
import { useCartStore } from "@/store/cart";
import { useAuthStore } from "@/store/auth";
import { getCategories } from "@/api/products";
import { logout as apiLogout } from "@/api/auth";

export default function StorefrontShell() {
  const count = useCartStore((s) => s.count());
  const { accessToken, user, logout } = useAuthStore();
  const isAuthed = !!accessToken;
  const isAdmin = user?.role === "ADMIN";
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [categories, setCategories] = useState([]);
  const [searchQuery, setSearchQuery] = useState(searchParams.get("q") ?? "");

  useEffect(() => {
    getCategories()
      .then((cats) => setCategories(Array.isArray(cats) ? cats : cats?.content ?? []))
      .catch(() => {});
  }, []);

  const handleSearch = (e) => {
    e.preventDefault();
    const q = searchQuery.trim();
    navigate(q ? `/products?q=${encodeURIComponent(q)}` : "/products");
  };

  const handleLogout = async () => {
    // Best-effort server logout; clear local session regardless.
    try { await apiLogout(); } catch { /* ignore */ }
    logout();
    navigate("/");
  };

  return (
    <div className="flex min-h-screen flex-col bg-background">
      {/* ① utility strip */}
      <div className="bg-foreground px-4 py-1 text-xs text-background/80 md:px-8">
        <div className="mx-auto flex max-w-7xl items-center justify-between">
          <span>Free delivery on orders over $50</span>
          <span className="hidden gap-4 sm:flex">
            <Link to="/products" className="hover:text-background">All Deals</Link>
            {isAuthed && (
              <Link to="/products" className="hover:text-background">Track order</Link>
            )}
          </span>
        </div>
      </div>

      {/* ② main bar */}
      <header className="sticky top-0 z-30 border-b border-border bg-background shadow-sm">
        <div className="mx-auto flex max-w-7xl items-center gap-3 px-4 py-3 md:px-8">
          {/* logo */}
          <Link to="/" className="shrink-0 font-display text-xl font-extrabold tracking-tight">
            United<span className="text-brand">Deals</span>
          </Link>

          {/* search */}
          <form onSubmit={handleSearch} className="flex flex-1 items-center">
            <input
              name="q"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search essentials, groceries and more…"
              className="w-full rounded-l-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
              aria-label="Search products"
            />
            <button
              type="submit"
              className="rounded-r-md border border-l-0 border-input bg-primary px-3 py-2 text-sm text-primary-foreground hover:opacity-90"
            >
              Search
            </button>
          </form>

          {/* right cluster */}
          <nav className="flex shrink-0 items-center gap-3 text-sm">
            {isAdmin && (
              <Link
                to="/admin/products"
                className="hidden items-center gap-1 text-muted-foreground hover:text-brand sm:flex"
              >
                <Settings className="h-4 w-4" />
                <span className="hidden lg:inline">Admin</span>
              </Link>
            )}

            {isAuthed ? (
              <div className="flex items-center gap-2">
                <span className="hidden text-xs text-muted-foreground lg:inline">
                  Hi, {user?.firstName}
                </span>
                <button
                  onClick={handleLogout}
                  className="flex items-center gap-1 text-muted-foreground hover:text-brand"
                  aria-label="Sign out"
                >
                  <LogOut className="h-4 w-4" />
                  <span className="hidden lg:inline">Sign out</span>
                </button>
              </div>
            ) : (
              <Link
                to="/login"
                className="flex items-center gap-1 text-muted-foreground hover:text-brand"
              >
                <User className="h-4 w-4" />
                <span className="hidden sm:inline">Sign in</span>
              </Link>
            )}

            <Link
              to="/cart"
              className="relative flex items-center gap-1 hover:text-brand"
              aria-label={`Cart (${count} items)`}
            >
              <ShoppingCart className="h-5 w-5" />
              {count > 0 && (
                <span className="absolute -right-2 -top-2 flex h-4 w-4 items-center justify-center rounded-full bg-brand text-[10px] font-bold text-white">
                  {count > 99 ? "99+" : count}
                </span>
              )}
            </Link>
          </nav>
        </div>

        {/* ③ category nav row */}
        {categories.length > 0 && (
          <div className="border-t border-border bg-background">
            <div className="mx-auto flex max-w-7xl gap-1 overflow-x-auto px-4 py-1.5 text-sm md:px-8">
              <Link
                to="/products"
                className="shrink-0 rounded px-2 py-1 text-muted-foreground hover:bg-muted hover:text-foreground"
              >
                All
              </Link>
              {categories.slice(0, 10).map((cat) => (
                <Link
                  key={cat.name}
                  to={`/products?category=${encodeURIComponent(cat.name)}`}
                  className="shrink-0 rounded px-2 py-1 text-muted-foreground hover:bg-muted hover:text-foreground"
                >
                  {cat.name}
                </Link>
              ))}
            </div>
          </div>
        )}
      </header>

      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-6 md:px-8">
        <Outlet />
      </main>

      <footer className="border-t border-border bg-muted/50 px-4 py-8 text-sm text-muted-foreground md:px-8">
        <div className="mx-auto grid max-w-7xl gap-6 sm:grid-cols-3">
          <div>
            <p className="mb-2 font-display font-extrabold text-foreground">
              United<span className="text-brand">Deals</span>
            </p>
            <p className="text-xs">Demo e-commerce storefront — gs-swe-challenge.</p>
          </div>
          <div>
            <p className="mb-2 font-semibold text-foreground">Shop</p>
            <ul className="space-y-1 text-xs">
              <li><Link to="/products" className="hover:text-brand">All Products</Link></li>
              <li><Link to="/cart" className="hover:text-brand">Cart</Link></li>
            </ul>
          </div>
          <div>
            <p className="mb-2 font-semibold text-foreground">Account</p>
            <ul className="space-y-1 text-xs">
              {isAuthed ? (
                <li>
                  <button onClick={handleLogout} className="hover:text-brand">Sign out</button>
                </li>
              ) : (
                <>
                  <li><Link to="/login" className="hover:text-brand">Sign in</Link></li>
                  <li><Link to="/register" className="hover:text-brand">Create account</Link></li>
                </>
              )}
            </ul>
          </div>
        </div>
      </footer>
    </div>
  );
}
