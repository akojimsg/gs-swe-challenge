/*
 * Route guards. Client-side gating only — the server re-checks every request
 * (the gateway enforces JWT + role). RequireAuth preserves the intended
 * destination via ?redirect= so checkout → sign-in → back works (cart preserved).
 */
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuthStore } from "@/store/auth";

export function RequireAuth() {
  const isAuthed = useAuthStore((s) => !!s.accessToken);
  const location = useLocation();
  if (!isAuthed) {
    const redirect = encodeURIComponent(location.pathname + location.search);
    return <Navigate to={`/login?redirect=${redirect}`} replace />;
  }
  return <Outlet />;
}

export function RequireAdmin() {
  const isAuthed = useAuthStore((s) => !!s.accessToken);
  const isAdmin = useAuthStore((s) => s.user?.role === "ADMIN");
  if (!isAuthed) return <Navigate to="/login?redirect=/admin/products" replace />;
  if (!isAdmin) return <Navigate to="/403" replace />;
  return <Outlet />;
}
