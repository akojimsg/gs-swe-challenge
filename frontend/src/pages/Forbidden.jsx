/*
 * PAGE: 403 Forbidden  ·  Bare/utility shell.
 * Shown when a non-admin hits an admin route (RequireAdmin guard).
 */
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/Button";

export default function Forbidden() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-3 text-center">
      <h1 className="font-display text-3xl font-extrabold">403</h1>
      <p className="text-muted-foreground">You don't have access to this page.</p>
      <Link to="/">
        <Button variant="outline">Go home</Button>
      </Link>
    </div>
  );
}
