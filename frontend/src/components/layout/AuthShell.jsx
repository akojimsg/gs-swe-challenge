/*
 * Auth shell — centered card, brand only, no nav (layouts.md). Used by
 * /login and /register. Full-width card on mobile.
 */
import { Link, Outlet } from "react-router-dom";

export default function AuthShell() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-muted px-4">
      <Link to="/" className="mb-6 font-display text-2xl font-extrabold">
        United<span className="text-brand">Deals</span>
      </Link>
      <div className="w-full max-w-md rounded-lg border border-border bg-card p-6 shadow-sm">
        <Outlet />
      </div>
    </div>
  );
}
