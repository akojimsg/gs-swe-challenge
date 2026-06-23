import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getProducts, getCategories } from "@/api/products";
import { ProductCard, ProductCardSkeleton } from "@/components/product/ProductCard";
import { picsumUrl } from "@/components/product/ProductImage";
import { Button } from "@/components/ui/Button";
import { ShieldCheck, RefreshCw, Headphones, Truck, ChevronRight } from "lucide-react";

const VALUE_PROPS = [
  { icon: Truck, title: "Fast delivery", text: "On orders over $50" },
  { icon: RefreshCw, title: "Easy returns", text: "30-day hassle-free" },
  { icon: ShieldCheck, title: "Secure payment", text: "256-bit SSL encryption" },
  { icon: Headphones, title: "24/7 Support", text: "Always here to help" },
];

export default function Home() {
  const [featured, setFeatured] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      getProducts({ size: 8, sort: "createdAt,desc" }).catch(() => null),
      getCategories().catch(() => null),
    ]).then(([prods, cats]) => {
      setFeatured(prods?.content ?? []);
      setCategories(Array.isArray(cats) ? cats : cats?.content ?? []);
    }).finally(() => setLoading(false));
  }, []);

  return (
    <div className="-mt-6 -mx-4 md:-mx-8">
      {/* hero banner */}
      <section className="relative overflow-hidden bg-foreground px-6 py-16 text-center md:px-8 md:py-24">
        <div className="relative z-10 mx-auto max-w-2xl">
          <p className="mb-2 text-xs font-semibold uppercase tracking-widest text-brand">
            New arrivals weekly
          </p>
          <h1 className="font-display text-4xl font-extrabold leading-tight text-background sm:text-5xl">
            Everything you need,<br />
            <span className="text-brand">at the best prices.</span>
          </h1>
          <p className="mt-4 text-base text-background/70">
            Browse thousands of products across categories — from electronics to fashion.
          </p>
          <div className="mt-8 flex flex-wrap justify-center gap-3">
            <Button asChild size="lg" variant="brand" className="px-8">
              <Link to="/products">Shop now</Link>
            </Button>
            <Button asChild size="lg" variant="outline" className="border-background/30 text-background hover:bg-background/10 hover:text-background px-8">
              <Link to="/products?sort=price,asc">View deals</Link>
            </Button>
          </div>
        </div>
      </section>

      <div className="px-4 md:px-8">
        {/* value props strip */}
        <section className="mx-auto my-8 grid max-w-7xl grid-cols-2 gap-4 sm:grid-cols-4">
          {VALUE_PROPS.map(({ icon: Icon, title, text }) => (
            <div key={title} className="flex items-center gap-3 rounded-lg border border-border bg-card p-4">
              <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-brand/10">
                <Icon className="h-4 w-4 text-brand" />
              </div>
              <div>
                <p className="text-sm font-semibold">{title}</p>
                <p className="text-xs text-muted-foreground">{text}</p>
              </div>
            </div>
          ))}
        </section>

        {/* shop by top categories — circular tiles */}
        {categories.length > 0 && (
          <section className="mx-auto mb-10 max-w-7xl">
            <div className="mb-6 flex items-end justify-between">
              <h2 className="font-display text-xl font-extrabold">
                <span className="text-muted-foreground">Shop From </span>
                <span className="border-b-2 border-brand text-brand">Top Categories</span>
              </h2>
              <Link
                to="/products"
                className="flex items-center text-sm font-medium text-brand hover:underline"
              >
                View All <ChevronRight className="h-4 w-4" />
              </Link>
            </div>
            <div className="flex gap-4 overflow-x-auto pb-2">
              {categories.slice(0, 8).map((cat) => (
                <Link
                  key={cat.id ?? cat.name}
                  to={`/products?category=${encodeURIComponent(cat.name)}`}
                  className="flex w-[132px] shrink-0 flex-col items-center gap-2"
                >
                  <div className="flex h-[132px] w-[132px] items-center justify-center overflow-hidden rounded-full bg-white ring-2 ring-transparent transition-all hover:ring-brand">
                    <img
                      src={picsumUrl(`cat-${cat.id ?? cat.name}`, 120, 120)}
                      alt={cat.name}
                      loading="lazy"
                      className="h-[76px] w-[76px] object-contain"
                    />
                  </div>
                  <p className="text-center text-sm font-medium text-foreground">{cat.name}</p>
                </Link>
              ))}
            </div>
          </section>
        )}

        {/* featured products */}
        <section className="mx-auto mb-12 max-w-7xl">
          <div className="mb-4 flex items-end justify-between">
            <h2 className="font-display text-xl font-extrabold">Featured products</h2>
            <Link to="/products" className="text-sm text-brand hover:underline">See all</Link>
          </div>

          {loading ? (
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
              {Array.from({ length: 8 }).map((_, i) => <ProductCardSkeleton key={i} />)}
            </div>
          ) : featured.length > 0 ? (
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
              {featured.map((p) => (
                <ProductCard key={p.id} product={p} />
              ))}
            </div>
          ) : (
            <div className="rounded-xl border border-dashed border-border py-16 text-center text-muted-foreground">
              <p className="mb-3 text-sm">No products yet.</p>
              <Button asChild variant="outline" size="sm">
                <Link to="/admin/products/new">Add products</Link>
              </Button>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
