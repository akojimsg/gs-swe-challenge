/*
 * Admin shell — left sidebar nav (Products · Import · Orders · Notifications) +
 * topbar (breadcrumb · admin menu). Sidebar collapses to a drawer on mobile
 * (layouts.md). Hosts the core admin pages. Used as a layout route under a
 * RequireAdmin guard.
 */
import { Link, NavLink, Outlet } from "react-router-dom";
import { useAuthStore } from "@/store/auth";

const NAV = [
  { to: "/admin/products", label: "Products" },
  { to: "/admin/import", label: "CSV Import" },
  { to: "/admin/orders", label: "Orders (ext)" },
  { to: "/admin/notifications", label: "Notifications (ext)" },
];

export default function AdminShell() {
  const user = useAuthStore((s) => s.user);
  return (
    <div className="flex min-h-screen">
      <aside className="w-56 shrink-0 border-r border-border p-4">
        <Link to="/" className="mb-6 block font-display text-lg font-extrabold">
          United<span className="text-brand">Deals</span>
        </Link>
        <nav className="flex flex-col gap-1 text-sm">
          {NAV.map((n) => (
            <NavLink
              key={n.to}
              to={n.to}
              className={({ isActive }) =>
                `rounded-md px-3 py-2 ${isActive ? "bg-muted font-medium" : "hover:bg-muted"}`
              }
            >
              {n.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="flex-1">
        <header className="flex items-center justify-between border-b border-border px-6 py-3 text-sm">
          <span className="text-muted-foreground">Admin</span>
          <span>{user?.email}</span>
        </header>
        <main className="p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
