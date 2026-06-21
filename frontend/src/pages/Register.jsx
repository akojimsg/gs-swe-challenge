/*
 * PAGE: Register
 * ---------------------------------------------------------------------------
 * Route:  /register  ·  Access: Public · Scope: ext (auth)  ·  Shell: AuthShell
 * Figma:  1:6340  ·  Spec: gse-requirement-docs/frontend-design/specs/auth.md
 *
 * API: register({email,password,firstName,lastName}) → 201 AuthResponse (BUYER).
 *      409 → "email already in use". 429 handled. Store session, honor ?redirect=.
 * BUILD NOTE (MCP): pull 1:6340 → AuthCard (register mode). Wiring is the contract.
 * ---------------------------------------------------------------------------
 */
import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { register } from "@/api/auth";
import { ApiError } from "@/api/client";
import { useAuthStore } from "@/store/auth";
import { Button } from "@/components/ui/Button";

export default function Register() {
  const setSession = useAuthStore((s) => s.setSession);
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    const f = new FormData(e.currentTarget);
    try {
      const res = await register({
        email: f.get("email"),
        password: f.get("password"),
        firstName: f.get("firstName"),
        lastName: f.get("lastName"),
      });
      setSession({ accessToken: res.accessToken, user: res.user });
      navigate(decodeURIComponent(params.get("redirect") || "/"));
    } catch (err) {
      setError(err instanceof ApiError && err.status === 409 ? "Email already in use." : "Couldn't create the account.");
      setSubmitting(false);
    }
  };

  // Minimal proof-of-wiring render — MCP agent implements Figma 1:6340.
  return (
    <form onSubmit={onSubmit} className="space-y-3">
      <h1 className="font-display text-xl font-extrabold">Create account</h1>
      {error && <p className="text-sm text-danger">{error}</p>}
      <div className="flex gap-2">
        <input name="firstName" placeholder="First name" className="w-full rounded-md border border-input px-3 py-2" />
        <input name="lastName" placeholder="Last name" className="w-full rounded-md border border-input px-3 py-2" />
      </div>
      <input name="email" type="email" required placeholder="Email" className="w-full rounded-md border border-input px-3 py-2" />
      <input name="password" type="password" required minLength={8} placeholder="Password (min 8)" className="w-full rounded-md border border-input px-3 py-2" />
      <Button type="submit" className="w-full" disabled={submitting}>
        {submitting ? "Creating…" : "Create account"}
      </Button>
      <p className="text-sm text-muted-foreground">
        Have an account? <Link to="/login" className="text-brand">Sign in</Link>
      </p>
    </form>
  );
}
