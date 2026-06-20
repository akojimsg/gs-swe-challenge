#!/usr/bin/env bash
# Idempotent demo seed (ADR-016). Drives the real service APIs — never raw SQL.
# Safe to run anytime: creates what is missing, skips what exists, repairs partial
# state, and reports when there is nothing to do.
#
# Layered by service availability:
#   Layer 1 (Users, Products) — admin + buyers + catalogue        [implemented]
#   Layer 2 (Orders/Payments/Notifications) — orders via the saga  [added when those lanes land]
set -euo pipefail

USERS_URL="${USERS_URL:-http://localhost:8081}"
PRODUCTS_URL="${PRODUCTS_URL:-http://localhost:8082}"
PG_CONTAINER="${PG_CONTAINER:-gsswec-postgres}"
PG_USER="${POSTGRES_USER:-gsswec}"
PG_DB="${POSTGRES_DB:-gsswec}"
CSV="$(dirname "$0")/seed-data/products.csv"

ADMIN_EMAIL="${SEED_ADMIN_EMAIL:-admin@gsswec.com}"
ADMIN_PASSWORD="${SEED_ADMIN_PASSWORD:-admin123}"
BUYER_EMAIL="${SEED_BUYER_EMAIL:-buyer@gsswec.com}"
BUYER_PASSWORD="${SEED_BUYER_PASSWORD:-buyer123}"

say()  { printf '  %s\n' "$1"; }
ok()   { printf '  \033[32m✓\033[0m %s\n' "$1"; }
skip() { printf '  \033[33m•\033[0m %s (already present, skipped)\n' "$1"; }

# --- preconditions -----------------------------------------------------------

wait_healthy() {
  local name="$1" url="$2" i
  for i in $(seq 1 30); do
    if curl -sf "$url/actuator/health" >/dev/null 2>&1; then return 0; fi
    sleep 2
  done
  echo "✖ $name not healthy at $url — run 'make up' and 'make run-$name' first." >&2
  exit 1
}

# --- helpers (idempotent) ----------------------------------------------------

# Register a user; ignore 409 (already exists). Returns nothing.
register_user() {
  local email="$1" pw="$2" first="$3" last="$4" code
  code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$USERS_URL/api/v1/auth/register" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$email\",\"password\":\"$pw\",\"firstName\":\"$first\",\"lastName\":\"$last\"}")
  case "$code" in
    201) ok "user $email created" ;;
    409) skip "user $email" ;;
    *)   echo "✖ register $email failed (HTTP $code)" >&2; return 1 ;;
  esac
}

# Promote a user to ADMIN if not already (interim until a first-class admin bootstrap).
ensure_admin() {
  local email="$1" current
  current=$(docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d "$PG_DB" -tAc \
    "SELECT role FROM users_schema.users WHERE email='$email';" 2>/dev/null | tr -d '[:space:]')
  if [ "$current" = "ADMIN" ]; then
    skip "admin role for $email"
  else
    docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d "$PG_DB" -q \
      -c "UPDATE users_schema.users SET role='ADMIN', updated_at=now() WHERE email='$email';" >/dev/null
    ok "promoted $email to ADMIN"
  fi
}

login_token() {
  local email="$1" pw="$2"
  curl -s -X POST "$USERS_URL/api/v1/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$email\",\"password\":\"$pw\"}" \
    | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p'
}

product_count() {
  curl -s "$PRODUCTS_URL/api/v1/products?size=1" \
    | sed -n 's/.*"totalElements":\([0-9]*\).*/\1/p'
}

# --- seed --------------------------------------------------------------------

echo "==> Seeding demo data (idempotent)"

wait_healthy users "$USERS_URL"
wait_healthy products "$PRODUCTS_URL"

echo "Layer 1 — users"
register_user "$ADMIN_EMAIL" "$ADMIN_PASSWORD" "Admin" "User"
ensure_admin "$ADMIN_EMAIL"
register_user "$BUYER_EMAIL" "$BUYER_PASSWORD" "Buyer" "One"
register_user "buyer2@gsswec.com" "buyer123" "Buyer" "Two"

echo "Layer 1 — catalogue"
existing=$(product_count)
existing="${existing:-0}"
if [ "$existing" -gt 0 ]; then
  skip "catalogue ($existing products already loaded)"
else
  token=$(login_token "$ADMIN_EMAIL" "$ADMIN_PASSWORD")
  if [ -z "$token" ]; then echo "✖ could not obtain admin token" >&2; exit 1; fi
  import_id=$(curl -s -X POST "$PRODUCTS_URL/api/v1/products/import" \
    -H "Authorization: Bearer $token" -F "file=@$CSV" \
    | sed -n 's/.*"importId":"\([^"]*\)".*/\1/p')
  ok "catalogue import started (importId=$import_id)"
  # Poll the async job to completion.
  for i in $(seq 1 30); do
    summary=$(curl -s -H "Authorization: Bearer $token" "$PRODUCTS_URL/api/v1/products/import/$import_id")
    case "$summary" in
      *'"status":"COMPLETED"'*) ok "catalogue loaded: $(echo "$summary" | sed -n 's/.*\("imported":[0-9]*,"updated":[0-9]*,"skipped":[0-9]*\).*/\1/p')"; break ;;
      *'"status":"FAILED"'*)    echo "✖ catalogue import failed" >&2; exit 1 ;;
    esac
    sleep 2
  done
fi

# Layer 2 (orders via the saga) is added here once Orders/Payments/Notifications land.

echo "==> Seed complete."
say "admin: $ADMIN_EMAIL / $ADMIN_PASSWORD   buyer: $BUYER_EMAIL / $BUYER_PASSWORD"
