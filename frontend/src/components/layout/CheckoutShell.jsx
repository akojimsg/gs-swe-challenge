/*
 * Checkout shell — distraction-free (layouts.md): minimal header (logo + secure
 * badge), NO nav/search/cart, no footer links. Reduces abandonment + accidental
 * navigation mid-payment.
 */
import { Link, Outlet } from "react-router-dom";
import { Lock } from "lucide-react";

export default function CheckoutShell() {
  return (
    <div className="flex min-h-screen flex-col">
      <header className="border-b border-border">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3 md:px-8">
          <Link to="/" className="font-display text-xl font-extrabold">
            United<span className="text-brand">Deals</span>
          </Link>
          <span className="flex items-center gap-1 text-xs text-muted-foreground">
            <Lock className="h-3.5 w-3.5" /> Secure checkout
          </span>
        </div>
      </header>
      <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-6 md:px-8">
        <Outlet />
      </main>
    </div>
  );
}
