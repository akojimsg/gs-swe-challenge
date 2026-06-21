/*
 * PAGE: 404 Not Found  ·  Bare/utility shell (rendered standalone).
 * Spec: 01-journeys-and-sitemap.md (shared pages).
 */
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/Button";

export default function NotFound() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-3 text-center">
      <h1 className="font-display text-3xl font-extrabold">404</h1>
      <p className="text-muted-foreground">This page doesn't exist.</p>
      <Link to="/">
        <Button variant="outline">Go home</Button>
      </Link>
    </div>
  );
}
