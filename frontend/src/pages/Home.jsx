/*
 * PAGE: Home / Landing
 * ---------------------------------------------------------------------------
 * Route:  /  ·  Access: Public · Scope: ext (recommended P2)  ·  Shell: StorefrontShell
 * Figma:  node 1:21021  ·  Spec: gse-requirement-docs/frontend-design/specs/home.md
 *
 * Storefront landing: hero · category tiles · featured products · value props.
 * API: getProducts({ size, sort }) for featured; getCategories() for tiles.
 * BUILD NOTE (MCP): pull 1:21021; drop sections we have no data for. Routes into
 *      the catalogue. Keep /products as the full PLP.
 * ---------------------------------------------------------------------------
 */
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/Button";

export default function Home() {
  // Minimal proof-of-wiring render — MCP agent implements Figma 1:21021.
  return (
    <section className="py-12 text-center">
      <h1 className="font-display text-4xl font-extrabold">Flash deals are here</h1>
      <p className="mx-auto mt-2 max-w-md text-muted-foreground">
        United Deals — gs-swe-challenge storefront. (STUB landing — implement Figma 1:21021.)
      </p>
      <Link to="/products" className="mt-6 inline-block">
        <Button variant="brand" size="lg">Shop all products</Button>
      </Link>
    </section>
  );
}
