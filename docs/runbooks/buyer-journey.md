# Runbook: Buyer user journey

Walk through the shopper experience — browsing the catalogue, building a cart,
checking out, and watching the order complete through the purchase saga.

**Prerequisite:** a running, seeded stack — see [local-stack.md](local-stack.md)
(`make up` then `make seed`).

## 1. Sign in (or register)

Open **http://localhost:3000** and either:

- **Sign in** with the seeded buyer:
  ```
  buyer@gsswec.com / buyer123
  ```
- or **Register** a new account — registration creates a `BUYER` and signs you
  in. (A welcome email is captured in Mailhog — see step 6.)

## 2. Browse the catalogue

- **Products** lists the catalogue with search, category filter, price range,
  and an in-stock toggle; results are paginated.
- Click a product to open its **detail** page (description, price, stock).

## 3. Build a cart

- From the catalogue or a product page, **Add to cart**.
- The **Cart** shows line items and a running subtotal; adjust quantities or
  remove items. The cart persists client-side.

## 4. Check out

1. From the cart, **Checkout** (requires being signed in).
2. Review the order summary and **Place order**.
3. The request carries an **Idempotency-Key**, so an accidental double-submit
   returns the same order rather than creating a duplicate. (Insufficient stock
   returns a clear error and keeps you on checkout.)

## 5. Watch the order complete

On **Order confirmation** the page polls the order status and updates live as
the saga runs:

- **Processing your payment…** → the order is `AWAITING_PAYMENT` while Payments
  processes it.
- **Payment confirmed!** → `PAID` (the fake processor approves ~90% of the time).
- **Payment failed** → `FAILED` (the ~10% decline path; reserved stock is
  released automatically by the saga's compensation).

This status comes from `GET /api/v1/orders/{id}` — the same lifecycle endpoint a
buyer can use to view their orders.

## 6. See the notification

Open **Mailhog → http://localhost:8025**. The Notifications service consumes the
order/payment events and sends a confirmation (or failure) email, captured here.

## What this demonstrates

- The full purchase flow: browse → cart → idempotent checkout → confirmation.
- The choreographed saga end-to-end, with live order-status updates and
  automatic stock release on payment failure.
- Event-driven notifications landing in Mailhog.
