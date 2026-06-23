/*
 * Cart store (Zustand) — client-side only; there is NO cart API.
 *
 * Items snapshot the fields the cart + checkout need (productId, name, sku, price,
 * qty, stock). Persisted so the cart survives reloads AND the login/register detour
 * (J1 rule: never make a guest rebuild the cart after authenticating).
 *
 * Checkout sends line items as { productId, quantity } to POST /api/v1/orders.
 * Totals are computed in JS and rounded (toFixed(2)); price 0 renders "Free".
 */
import { create } from "zustand";
import { persist } from "zustand/middleware";

export const useCartStore = create(
  persist(
    (set, get) => ({
      items: [], // { productId, name, sku, price, qty, stock, imageUrl }

      addItem: (product, qty = 1) =>
        set((state) => {
          const existing = state.items.find((i) => i.productId === product.productId);
          if (existing) {
            return {
              items: state.items.map((i) =>
                i.productId === product.productId
                  ? { ...i, qty: Math.min(i.qty + qty, i.stock ?? Infinity) }
                  : i,
              ),
            };
          }
          return { items: [...state.items, { ...product, qty }] };
        }),

      setQty: (productId, qty) =>
        set((state) => ({
          items:
            qty <= 0
              ? state.items.filter((i) => i.productId !== productId)
              : state.items.map((i) =>
                  i.productId === productId
                    ? { ...i, qty: Math.min(qty, i.stock ?? Infinity) }
                    : i,
                ),
        })),

      removeItem: (productId) =>
        set((state) => ({ items: state.items.filter((i) => i.productId !== productId) })),

      clear: () => set({ items: [] }),

      // Derived selectors.
      count: () => get().items.reduce((n, i) => n + i.qty, 0),
      subtotal: () =>
        Number(get().items.reduce((sum, i) => sum + i.price * i.qty, 0).toFixed(2)),
      // Line items for the order placement contract.
      toOrderLines: () => get().items.map((i) => ({ productId: i.productId, quantity: i.qty })),
    }),
    { name: "gsswec-cart" },
  ),
);
