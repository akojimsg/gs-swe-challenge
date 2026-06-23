import { useState } from "react";
import { Package } from "lucide-react";
import { cn } from "@/lib/cn";

const SIZES = {
  thumb: { w: 80, h: 80 },
  card: { w: 300, h: 300 },
  hero: { w: 600, h: 480 },
};

export function picsumUrl(seed, w, h) {
  return `https://picsum.photos/seed/${encodeURIComponent(seed)}/${w}/${h}`;
}

export default function ProductImage({ product, size = "card", alt, className }) {
  const [failed, setFailed] = useState(false);
  const { w, h } = SIZES[size] ?? SIZES.card;

  const isHero = size === "hero";
  const rounded = size === "thumb" ? "rounded-md" : "rounded-lg";
  const aspect = isHero ? "aspect-[5/4]" : "aspect-square";

  const wrapper = cn("overflow-hidden bg-muted", aspect, rounded, className);

  if (failed) {
    return (
      <div className={cn(wrapper, "flex items-center justify-center")}>
        <Package className="h-1/3 w-1/3 text-muted-foreground/40" />
      </div>
    );
  }

  const src = product?.imageUrl ?? picsumUrl(product?.id ?? "unknown", w, h);

  return (
    <div className={wrapper}>
      <img
        src={src}
        alt={alt ?? product?.name ?? "Product image"}
        loading="lazy"
        onError={() => setFailed(true)}
        className={cn("h-full w-full", isHero ? "object-contain" : "object-cover")}
      />
    </div>
  );
}
