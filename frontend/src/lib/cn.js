import { clsx } from "clsx";
import { twMerge } from "tailwind-merge";

// Standard shadcn/ui class-merge helper: conditional classes + Tailwind conflict
// resolution. Used by every UI primitive and domain component.
export function cn(...inputs) {
  return twMerge(clsx(inputs));
}
