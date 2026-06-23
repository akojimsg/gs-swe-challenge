# Runbook: Purchase saga demo

Drive the choreographed purchase saga end-to-end and observe both paths — the
happy path (payment succeeds) and the compensation path (payment fails, reserved
stock is released).

**Prerequisite:** a running, seeded stack — see [local-stack.md](local-stack.md).

## The saga, in brief

```
order.placed → Payments charges → payment.succeeded → Orders: PAID  → order.paid
                                 ↘ payment.failed   → Orders: FAILED → order.failed
                                                       + release reserved stock
```

Choreographed (no central orchestrator); each service reacts to events. Delivery
is at-least-once with idempotent consumers.

## A. Happy path (via the UI)

Follow [buyer-journey.md](buyer-journey.md): place an order and watch the
confirmation page move **Processing… → Payment confirmed!** (`PAID`). A
confirmation email lands in **Mailhog (http://localhost:8025)**.

## B. Force the failure / compensation path

The fake processor approves ~90% of the time. To see the failure path
deterministically, run Payments with a 0% success rate:

```bash
# Restart just the payments service with forced decline
docker compose -f infra/docker-compose.yml stop payments
PAYMENTS_SUCCESS_RATE=0 docker compose -f infra/docker-compose.yml up -d payments
```

Now place an order (UI or API). The order resolves to **FAILED**, an
`order.failed` event is emitted, and the saga **releases the stock** that was
reserved at placement.

Restore normal behaviour afterward:

```bash
docker compose -f infra/docker-compose.yml stop payments
docker compose -f infra/docker-compose.yml up -d payments
```

## C. Observe the events on the streams

The events flow over Redis Streams. Inspect them from the redis container:

```bash
# stream lengths per hop
for s in order.placed payment.succeeded payment.failed order.paid order.failed; do
  echo -n "$s -> "; docker exec gsswec-redis redis-cli XLEN "$s"
done

# read the messages on a stream (eventId / eventType / JSON payload)
docker exec gsswec-redis redis-cli XRANGE order.placed - + COUNT 10

# consumer-group processing state (pending = delivered-but-unacked)
docker exec gsswec-redis redis-cli XINFO GROUPS payment.succeeded
```

A healthy run shows `payment.succeeded + payment.failed` equal to `order.placed`
(every order got exactly one outcome), and `order.paid + order.failed` matching.

## D. Verify the outcome directly

```bash
# payment record for an order (ADMIN or owner token required)
curl localhost:8080/api/v1/payments/order/<orderId> -H "Authorization: Bearer <token>"

# order status (drives the confirmation page)
curl localhost:8080/api/v1/orders/<orderId> -H "Authorization: Bearer <token>"
```

## What this demonstrates

- A working choreographed saga with compensating actions (stock release on
  payment failure) — no central orchestrator.
- Event-driven flow over Redis Streams with idempotent, at-least-once consumers.
- Notifications reacting to the same events (Mailhog).
