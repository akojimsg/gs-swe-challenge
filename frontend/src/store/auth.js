/*
 * Auth store (Zustand) — access token + current user.
 *
 * Contract (docs/api/users-openapi.yaml): login/register return an AuthResponse
 * { accessToken, tokenType, user{ id,email,firstName,lastName,role } }. The
 * refresh token is an httpOnly cookie (browser-managed) — NOT stored here.
 *
 * The access token persists to localStorage so a reload keeps the session; the
 * api client reads it via getState() and refreshes on 401. role ∈ {BUYER, ADMIN}.
 */
import { create } from "zustand";
import { persist } from "zustand/middleware";

export const useAuthStore = create(
  persist(
    (set, get) => ({
      accessToken: null,
      user: null,

      // Set full session after login/register.
      setSession: ({ accessToken, user }) => set({ accessToken, user }),
      // Update only the token (used by the refresh flow).
      setAccessToken: (accessToken) => set({ accessToken }),
      logout: () => set({ accessToken: null, user: null }),

      isAuthenticated: () => !!get().accessToken,
      isAdmin: () => get().user?.role === "ADMIN",
    }),
    {
      name: "gsswec-auth",
      // Persist the token + user; refresh cookie lives in the browser.
      partialize: (s) => ({ accessToken: s.accessToken, user: s.user }),
    },
  ),
);
