import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "node:path";

// `@` → src for clean imports. Dev proxy forwards /api to the Gateway so the SPA
// and API share an origin in dev (mirrors nginx in prod). Override the target via
// VITE_PROXY_TARGET; defaults to the gateway on the frontend lane's host port.
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { "@": path.resolve(import.meta.dirname, "./src") },
  },
  server: {
    proxy: {
      "/api": {
        target: process.env.VITE_PROXY_TARGET || "http://localhost:8110",
        changeOrigin: true,
      },
    },
  },
});
