/*
 * Route table — the full P1 (core) + P2 (auth/home) sitemap from
 * 01-journeys-and-sitemap.md, composed from the 4 shells + guards.
 *
 * Each page is an annotated stub (see its file header for Figma node + spec +
 * API + states). The MCP build agent implements visuals against the wired pages.
 */
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import StorefrontShell from "@/components/layout/StorefrontShell";
import CheckoutShell from "@/components/layout/CheckoutShell";
import AuthShell from "@/components/layout/AuthShell";
import AdminShell from "@/components/admin/AdminShell";
import { RequireAuth, RequireAdmin } from "@/components/common/Guards";

import Home from "@/pages/Home";
import Catalogue from "@/pages/Catalogue";
import ProductDetail from "@/pages/ProductDetail";
import Cart from "@/pages/Cart";
import Checkout from "@/pages/Checkout";
import OrderConfirmation from "@/pages/OrderConfirmation";
import Login from "@/pages/Login";
import Register from "@/pages/Register";
import NotFound from "@/pages/NotFound";
import Forbidden from "@/pages/Forbidden";

import AdminProducts from "@/pages/admin/AdminProducts";
import AdminProductForm from "@/pages/admin/AdminProductForm";
import AdminImport from "@/pages/admin/AdminImport";
import AdminOrders from "@/pages/admin/AdminOrders";
import AdminNotifications from "@/pages/admin/AdminNotifications";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Storefront shell — public + buyer */}
        <Route element={<StorefrontShell />}>
          <Route index element={<Home />} />
          <Route path="/products" element={<Catalogue />} />
          <Route path="/products/:id" element={<ProductDetail />} />
          <Route path="/cart" element={<Cart />} />
          <Route element={<RequireAuth />}>
            <Route path="/order/:id/confirmation" element={<OrderConfirmation />} />
            <Route path="/orders" element={<AdminOrders />} />
          </Route>
        </Route>

        {/* Checkout shell — buyer, distraction-free */}
        <Route element={<RequireAuth />}>
          <Route element={<CheckoutShell />}>
            <Route path="/checkout" element={<Checkout />} />
          </Route>
        </Route>

        {/* Auth shell */}
        <Route element={<AuthShell />}>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
        </Route>

        {/* Admin shell — role=ADMIN */}
        <Route element={<RequireAdmin />}>
          <Route element={<AdminShell />}>
            <Route path="/admin" element={<Navigate to="/admin/products" replace />} />
            <Route path="/admin/products" element={<AdminProducts />} />
            <Route path="/admin/products/new" element={<AdminProductForm mode="create" />} />
            <Route path="/admin/products/:id/edit" element={<AdminProductForm mode="edit" />} />
            <Route path="/admin/import" element={<AdminImport />} />
            <Route path="/admin/orders" element={<AdminOrders />} />
            <Route path="/admin/notifications" element={<AdminNotifications />} />
          </Route>
        </Route>

        <Route path="/403" element={<Forbidden />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </BrowserRouter>
  );
}
