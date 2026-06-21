/*
 * PAGE: Login
 * ---------------------------------------------------------------------------
 * Route:  /login  ·  Access: Public · Scope: ext (auth)  ·  Shell: AuthShell
 * Figma:  1:6340  ·  Spec: gse-requirement-docs/frontend-design/specs/auth.md
 *
 * API: login({email,password}) → AuthResponse {accessToken,user}. Store in auth
 *      store. 401 → ONE generic message (no account enumeration). 429 handled.
 * INTERACTION: honor ?redirect= so checkout → sign-in → back works (cart preserved).
 *      Never log credentials.
 * BUILD NOTE (MCP): pull 1:6340 → AuthCard form. Session wiring below is the contract.
 * ---------------------------------------------------------------------------
 */
import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { login } from "@/api/auth";
import { useAuthStore } from "@/store/auth";
import { Button } from "@/components/ui/Button";

export default function Login() {
  const setSession = useAuthStore((s) => s.setSession);
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    const form = new FormData(e.currentTarget);
    try {
      const res = await login({ email: form.get("email"), password: form.get("password") });
      setSession({ accessToken: res.accessToken, user: res.user });
      navigate(decodeURIComponent(params.get("redirect") || "/"));
    } catch {
      // Generic message for both 401 and others — no enumeration.
      setError("Invalid email or password.");
      setSubmitting(false);
    }
  };

  // Minimal proof-of-wiring render — MCP agent implements Figma 1:6340.
  return (
    <form onSubmit={onSubmit} className="space-y-3">
      <h1 className="font-display text-xl font-extrabold">Sign in</h1>
      {error && <p className="text-sm text-danger">{error}</p>}
      <input name="email" type="email" required placeholder="Email" className="w-full rounded-md border border-input px-3 py-2" />
      <input name="password" type="password" required placeholder="Password" className="w-full rounded-md border border-input px-3 py-2" />
      <Button type="submit" className="w-full" disabled={submitting}>
        {submitting ? "Signing in…" : "Sign in"}
      </Button>
      <p className="text-sm text-muted-foreground">
        No account? <Link to="/register" className="text-brand">Create one</Link>
      </p>
    </form>
  );
}
