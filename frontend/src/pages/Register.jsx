import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { register } from "@/api/auth";
import { ApiError } from "@/api/client";
import { useAuthStore } from "@/store/auth";
import { Button } from "@/components/ui/Button";

export default function Register() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const redirect = searchParams.get("redirect") || "/";
  const setSession = useAuthStore((s) => s.setSession);

  const [form, setForm] = useState({ firstName: "", lastName: "", email: "", password: "", confirm: "" });
  const [fieldErrors, setFieldErrors] = useState({});
  const [globalError, setGlobalError] = useState(null);
  const [loading, setLoading] = useState(false);

  const onChange = (e) => {
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }));
    setFieldErrors((fe) => ({ ...fe, [e.target.name]: undefined }));
  };

  const validate = () => {
    const errs = {};
    if (!form.firstName.trim()) errs.firstName = "Required";
    if (!form.lastName.trim()) errs.lastName = "Required";
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) errs.email = "Enter a valid email";
    if (form.password.length < 8) errs.password = "At least 8 characters";
    if (form.password !== form.confirm) errs.confirm = "Passwords don't match";
    return errs;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setGlobalError(null);
    const errs = validate();
    if (Object.keys(errs).length) { setFieldErrors(errs); return; }
    setLoading(true);
    try {
      const data = await register({
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email: form.email.toLowerCase(),
        password: form.password,
      });
      setSession({ accessToken: data.accessToken, user: data.user });
      navigate(redirect, { replace: true });
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 409) setFieldErrors((fe) => ({ ...fe, email: "Email already in use" }));
        else if (err.status === 429) setGlobalError("Too many attempts — try again shortly.");
        else setGlobalError("Something went wrong. Please try again.");
      } else {
        setGlobalError("Something went wrong. Please try again.");
      }
    } finally {
      setLoading(false);
    }
  };

  const field = (name, label, type = "text", extra = {}) => (
    <div>
      <label htmlFor={name} className="mb-1 block text-sm font-medium">{label}</label>
      <input
        id={name}
        name={name}
        type={type}
        value={form[name]}
        onChange={onChange}
        className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
        {...extra}
      />
      {fieldErrors[name] && (
        <p className="mt-1 text-xs text-danger">{fieldErrors[name]}</p>
      )}
    </div>
  );

  return (
    <div className="w-full max-w-sm">
      <div className="mb-6 text-center">
        <h1 className="text-xl font-semibold">Create account</h1>
        <p className="mt-1 text-sm text-muted-foreground">Start shopping today</p>
      </div>

      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          {field("firstName", "First name", "text", { placeholder: "Jane", autoComplete: "given-name" })}
          {field("lastName", "Last name", "text", { placeholder: "Doe", autoComplete: "family-name" })}
        </div>
        {field("email", "Email", "email", { placeholder: "you@example.com", autoComplete: "email" })}
        {field("password", "Password", "password", { placeholder: "Min 8 characters", autoComplete: "new-password" })}
        {field("confirm", "Confirm password", "password", { placeholder: "••••••••", autoComplete: "new-password" })}

        {globalError && (
          <p className="rounded-md border border-danger/30 bg-danger/10 px-3 py-2 text-sm text-danger">
            {globalError}
          </p>
        )}

        <Button type="submit" disabled={loading} className="w-full" size="lg">
          {loading ? "Creating account…" : "Create account"}
        </Button>
      </form>

      <p className="mt-4 text-center text-sm text-muted-foreground">
        Already have an account?{" "}
        <Link
          to={`/login${redirect !== "/" ? `?redirect=${encodeURIComponent(redirect)}` : ""}`}
          className="font-medium text-foreground hover:text-brand"
        >
          Sign in
        </Link>
      </p>
    </div>
  );
}
