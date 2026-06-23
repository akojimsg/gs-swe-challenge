/*
 * Minimal Button primitive (shadcn-style, token-driven). The MCP build agent may
 * replace/extend this with the full shadcn `button` — keep the variant API stable.
 * Default variant = near-black --primary CTA (Figma); `brand` = orange accent.
 */
import { cva } from "class-variance-authority";
import { cn } from "@/lib/cn";

const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50",
  {
    variants: {
      variant: {
        default: "bg-primary text-primary-foreground hover:opacity-90",
        brand: "bg-brand text-white hover:opacity-90",
        "brand-outline": "border border-brand text-brand bg-transparent hover:bg-brand/10",
        outline: "border border-border bg-transparent hover:bg-muted",
        ghost: "hover:bg-muted",
        danger: "bg-danger text-white hover:opacity-90",
      },
      size: {
        default: "h-10 px-4 py-2",
        sm: "h-9 px-3",
        lg: "h-11 px-6",
        icon: "h-10 w-10",
      },
    },
    defaultVariants: { variant: "default", size: "default" },
  },
);

export function Button({ className, variant, size, ...props }) {
  return <button className={cn(buttonVariants({ variant, size }), className)} {...props} />;
}

export { buttonVariants };
