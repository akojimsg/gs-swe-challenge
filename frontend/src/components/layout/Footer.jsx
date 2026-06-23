import { Link } from "react-router-dom";
import { ArrowRight } from "lucide-react";

const QUICK_LINKS = [
  { label: "Shop Products", to: "/products" },
  { label: "Shopping Cart", to: "/cart" },
  { label: "Track Order", to: "/products" },
  { label: "Customer Help", to: "/products" },
  { label: "About Us", to: "/products" },
];

const POPULAR_TAGS = [
  "Game", "iPhone", "TV", "Asus Laptops",
  "Macbook", "SSD", "Graphics Card",
  "Power Bank", "Smart TV", "Speaker",
  "Tablet", "Microwave", "Samsung",
];

const FALLBACK_CATEGORIES = [
  "Computer & Laptop", "SmartPhone", "Headphone",
  "Camera & Photo", "TV & Homes", "Accessories",
];

export default function Footer({ categories = [], onLogout, isAuthed }) {
  const topCats = categories.length > 0
    ? categories.slice(0, 6).map((c) => c.name)
    : FALLBACK_CATEGORIES;

  return (
    <footer className="bg-[#191C1F]">
      {/* ── main grid ── */}
      <div className="mx-auto max-w-7xl px-4 py-16 md:px-8">
        <div className="grid gap-10 sm:grid-cols-2 lg:grid-cols-[1.4fr_1fr_1fr_1fr_1.3fr]">

          {/* 1 — Logo + Contact */}
          <div className="space-y-6">
            <Link to="/" className="flex items-center gap-2">
              <span className="flex h-12 w-12 items-center justify-center rounded-full bg-brand text-xl font-bold text-white">U</span>
              <span className="font-display text-2xl font-bold tracking-tight text-white">UNITED DEAL</span>
            </Link>
            <div className="space-y-3 text-sm">
              <div>
                <p className="text-[#77878F]">Customer Support:</p>
                <p className="text-lg font-semibold text-white">(629) 555-0129</p>
              </div>
              <p className="text-[#ADB7BC]">
                4517 Washington Ave.<br />Manchester, Kentucky 39495
              </p>
              <p className="font-medium text-white">info@uniteddeals.com</p>
            </div>
          </div>

          {/* 2 — Top Category */}
          <div className="space-y-3">
            <p className="text-sm font-medium uppercase tracking-wide text-white">Top Category</p>
            <ul>
              {topCats.map((name) => (
                <li key={name}>
                  <Link
                    to={`/products?category=${encodeURIComponent(name)}`}
                    className="block py-1.5 text-sm font-medium text-[#929FA5] transition-colors hover:text-white"
                  >
                    {name}
                  </Link>
                </li>
              ))}
            </ul>
            <Link
              to="/products"
              className="flex items-center gap-1.5 pt-1 text-sm font-medium text-warning transition-opacity hover:opacity-80"
            >
              Browse All Products <ArrowRight className="h-4 w-4" />
            </Link>
          </div>

          {/* 3 — Quick Links */}
          <div className="space-y-3">
            <p className="text-sm font-medium uppercase tracking-wide text-white">Quick Links</p>
            <ul>
              {QUICK_LINKS.map(({ label, to }) => (
                <li key={label}>
                  <Link
                    to={to}
                    className="block py-1.5 text-sm font-medium text-[#929FA5] transition-colors hover:text-white"
                  >
                    {label}
                  </Link>
                </li>
              ))}
              {isAuthed ? (
                <li>
                  <button
                    onClick={onLogout}
                    className="block py-1.5 text-left text-sm font-medium text-[#929FA5] transition-colors hover:text-white"
                  >
                    Sign Out
                  </button>
                </li>
              ) : (
                <>
                  <li>
                    <Link to="/login" className="block py-1.5 text-sm font-medium text-[#929FA5] transition-colors hover:text-white">
                      Sign In
                    </Link>
                  </li>
                  <li>
                    <Link to="/register" className="block py-1.5 text-sm font-medium text-[#929FA5] transition-colors hover:text-white">
                      Create Account
                    </Link>
                  </li>
                </>
              )}
            </ul>
          </div>

          {/* 4 — Download App */}
          <div className="space-y-4">
            <p className="text-sm font-medium uppercase tracking-wide text-white">Download App</p>
            <div className="space-y-3">
              <a
                href="#"
                aria-label="Get on Google Play"
                className="flex items-center gap-4 rounded-sm bg-[#303639] px-5 py-4 transition-colors hover:bg-[#3a4044]"
              >
                <svg viewBox="0 0 24 24" className="h-8 w-8 shrink-0" fill="none" aria-hidden>
                  <path d="M3 20.5v-17c0-.83 1-.94 1.49-.45l14 8.5a.5.5 0 010 .88l-14 8.5C4 21.44 3 21.33 3 20.5z" fill="#4CAF50"/>
                  <path d="M3.5 20.99l9.19-9.19L5.49 4.6 3.5 3.5v17.49z" fill="#1976D2"/>
                  <path d="M12.69 11.8l2.44-2.44-8.44-4.76-2.19 2.19 8.19 5.01z" fill="#FF3D00"/>
                  <path d="M12.69 11.8l-8.19 5.01 2.19 2.19 8.44-4.76-2.44-2.44z" fill="#FFD600"/>
                </svg>
                <div className="text-white">
                  <p className="text-[11px] leading-tight">Get it now</p>
                  <p className="text-sm font-semibold leading-tight">Google Play</p>
                </div>
              </a>
              <a
                href="#"
                aria-label="Get on App Store"
                className="flex items-center gap-4 rounded-sm bg-[#303639] px-5 py-4 transition-colors hover:bg-[#3a4044]"
              >
                <svg viewBox="0 0 24 24" className="h-8 w-8 shrink-0 fill-white" aria-hidden>
                  <path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.8-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M13 3.5c.73-.83 1.94-1.46 2.94-1.5.13 1.17-.34 2.35-1.04 3.19-.69.85-1.83 1.51-2.95 1.42-.15-1.15.41-2.35 1.05-3.11z"/>
                </svg>
                <div className="text-white">
                  <p className="text-[11px] leading-tight">Get it now</p>
                  <p className="text-sm font-semibold leading-tight">App Store</p>
                </div>
              </a>
            </div>
          </div>

          {/* 5 — Popular Tags */}
          <div className="space-y-4">
            <p className="text-sm font-medium uppercase tracking-wide text-white">Popular Tag</p>
            <div className="flex flex-wrap gap-2">
              {POPULAR_TAGS.map((tag) => (
                <Link
                  key={tag}
                  to={`/products?q=${encodeURIComponent(tag)}`}
                  className="rounded-sm border border-[#303639] px-3 py-1.5 text-sm font-medium text-white transition-colors hover:border-white"
                >
                  {tag}
                </Link>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* ── bottom bar ── */}
      <div className="border-t border-[#303639]">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-2 px-4 py-4 text-xs text-[#77878F] md:px-8">
          <p>© {new Date().getFullYear()} United Deals. All rights reserved.</p>
          <div className="flex gap-4">
            <a href="#" className="transition-colors hover:text-white">Privacy Policy</a>
            <a href="#" className="transition-colors hover:text-white">Terms of Use</a>
          </div>
        </div>
      </div>
    </footer>
  );
}
