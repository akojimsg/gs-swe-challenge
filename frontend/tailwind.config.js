/** @type {import('tailwindcss').Config} */
// Maps the design-system tokens (src/styles/tokens.css) to Tailwind utilities.
// Colors reference CSS vars so light/dark themes work without rebuilding classes.
// See gse-requirement-docs/frontend-design/design-system.md.
export default {
  darkMode: ["class"],
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    container: {
      center: true,
      padding: "1rem",
      screens: { "2xl": "1280px" },
    },
    extend: {
      colors: {
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        card: {
          DEFAULT: "hsl(var(--card))",
          foreground: "hsl(var(--foreground))",
        },
        // Brand accent (orange) — price, ratings, active, highlights. NOT the CTA.
        brand: "hsl(var(--brand))",
        // Semantic status colors (badges, alerts) — see design-system.md.
        info: "hsl(var(--info))",
        warning: "hsl(var(--warning))",
        success: "hsl(var(--success))",
        danger: "hsl(var(--danger))",
      },
      borderRadius: {
        lg: "var(--radius)",
        md: "calc(var(--radius) - 2px)",
        sm: "calc(var(--radius) - 4px)",
      },
      fontFamily: {
        // UI / body / labels
        sans: ["Inter", "ui-sans-serif", "system-ui", "sans-serif"],
        // Display headings (hero, large section titles)
        display: ["Lato", "ui-sans-serif", "system-ui", "sans-serif"],
      },
    },
  },
  plugins: [],
};
