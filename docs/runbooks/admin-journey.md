# Runbook: Admin user journey

Walk through the admin experience — signing in and managing the product
catalogue (create, edit, delete, and CSV import).

**Prerequisite:** a running, seeded stack — see [local-stack.md](local-stack.md)
(`make up` then `make seed`).

## 1. Sign in as admin

1. Open the storefront: **http://localhost:3000**
2. Click **Sign in** and use the seeded admin:
   ```
   admin@gsswec.com / admin123
   ```
3. Because the account has the `ADMIN` role, an **Admin** link appears in the
   header. Click it (or go to **http://localhost:3000/admin/products**).

> Buyers do not see the Admin link, and the admin routes are guarded — a buyer
> who navigates to `/admin/*` is redirected to a Forbidden page.

## 2. Browse the catalogue (admin view)

The **Admin → Products** page lists every product with SKU, price, stock badge,
and per-row actions. Pagination and category filtering work as on the storefront.

## 3. Create a product

1. Click **New product** (the **+** action).
2. Fill the form. Validation mirrors the API:
   - **Name**, **SKU** — required
   - **Price** — non-negative number
   - **Stock** — non-negative integer
   - **Description**, **Weight (kg)**, **Category**, **Active** — optional
3. **Save.** The new product appears in the list and is immediately visible on
   the storefront.

## 4. Edit / delete a product

- **Edit** (pencil) opens the same form pre-filled; save to update.
- **Delete** (trash) prompts for confirmation, then removes the product.

## 5. CSV import

1. Go to **Admin → Import** (the **Upload** action).
2. Choose a CSV. You can reuse the sample at
   `scripts/seed-data/products.csv` (the same dataset `make seed` loads).
3. Upload. The result summary reports **imported / updated / skipped / errors**,
   and any bad rows are listed in a per-row error table with the reason.
4. Import is **upsert by SKU** — re-importing updates existing products rather
   than rejecting them. The sample file intentionally contains edge cases
   (malformed prices, duplicate SKUs, missing fields, etc.) so the error
   reporting is visible.

## What this demonstrates

- Role-based access (ADMIN-only routes, guarded in the SPA and enforced by the
  API).
- Full product CRUD with server-side validation.
- Bulk CSV import with row-level validation and an upsert-by-SKU strategy.
