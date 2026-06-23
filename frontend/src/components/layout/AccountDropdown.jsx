import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  User,
  ChevronDown,
  LogIn,
  UserPlus,
  Package,
  UserCog,
  LayoutDashboard,
  Users,
  LogOut,
} from "lucide-react";
import { useAuthStore } from "@/store/auth";
import { useToast } from "@/components/common/Toast";
import { logout as apiLogout } from "@/api/auth";

// Header account menu — guest vs. authenticated, with role-aware items. Replaces
// the flat auth cluster in StorefrontShell.
export default function AccountDropdown() {
  const navigate = useNavigate();
  const toast = useToast();
  const { accessToken, user, logout } = useAuthStore();
  const isAuthed = !!accessToken;
  const isAdmin = user?.role === "ADMIN";

  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  useEffect(() => {
    if (!open) return undefined;
    const onClick = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false);
    };
    const onKey = (e) => {
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("mousedown", onClick);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onClick);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  const go = (path) => {
    setOpen(false);
    navigate(path);
  };

  const notify = (message) => {
    setOpen(false);
    toast(message);
  };

  const handleLogout = async () => {
    setOpen(false);
    try {
      await apiLogout();
    } catch {
      /* best-effort; clear local session regardless */
    }
    logout();
    navigate("/");
  };

  const itemClass =
    "flex items-center gap-2.5 w-full px-4 py-2 text-sm hover:bg-muted transition-colors text-left";

  return (
    <div className="relative" ref={ref}>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="flex items-center gap-1.5 text-sm hover:text-brand transition-colors cursor-pointer"
        aria-haspopup="menu"
        aria-expanded={open}
      >
        <User className="h-4 w-4" />
        <span className="hidden sm:inline">
          {isAuthed ? `Hi, ${user?.firstName}` : "Account"}
        </span>
        <ChevronDown className="h-4 w-4" />
      </button>

      {open && (
        <div
          role="menu"
          className="absolute right-0 top-full mt-2 z-50 w-48 rounded-lg border border-border bg-background shadow-lg py-1"
        >
          {!isAuthed && (
            <>
              <button type="button" className={itemClass} onClick={() => go("/login")}>
                <LogIn className="h-4 w-4" /> Sign In
              </button>
              <button type="button" className={itemClass} onClick={() => go("/register")}>
                <UserPlus className="h-4 w-4" /> Register
              </button>
            </>
          )}

          {isAuthed && (
            <>
              <button
                type="button"
                className={itemClass}
                onClick={() => go("/orders")}
              >
                <Package className="h-4 w-4" /> My Orders
              </button>
              <button
                type="button"
                className={itemClass}
                onClick={() => notify("Profile page coming soon")}
              >
                <UserCog className="h-4 w-4" /> Profile
              </button>

              {isAdmin && (
                <>
                  <hr className="my-1 border-border" />
                  <button
                    type="button"
                    className={itemClass}
                    onClick={() => go("/admin/products")}
                  >
                    <LayoutDashboard className="h-4 w-4" /> Admin Panel
                  </button>
                  <button
                    type="button"
                    className={itemClass}
                    onClick={() => notify("User management coming soon")}
                  >
                    <Users className="h-4 w-4" /> Manage Users
                  </button>
                </>
              )}

              <hr className="my-1 border-border" />
              <button
                type="button"
                className={`${itemClass} text-danger`}
                onClick={handleLogout}
              >
                <LogOut className="h-4 w-4" /> Sign Out
              </button>
            </>
          )}
        </div>
      )}
    </div>
  );
}
