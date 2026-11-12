#!/usr/bin/env bash
# Gets the Let's Encrypt certificate for API_HOST + TEST_API_HOST
# (ADR 0016). One cert for both names, lineage named after API_HOST,
# so nginx's test block never points at a missing cert. The certbot
# service renews it later.
#
# Fresh box (nginx can't start without a cert): certbot standalone on
# port 80, nginx stopped.
#
#   cd /opt/gymbuddy && CERT_EMAIL=you@example.com ./init-cert.sh
#
# nginx already running with this repo's api.conf.template (it answers
# ACME challenges for any name on port 80), e.g. to add the test
# name: WEBROOT=1 goes through nginx, no downtime. nginx starts fine
# on the old cert meanwhile; reload it afterwards.
#
#   cd /opt/gymbuddy && WEBROOT=1 CERT_EMAIL=you@example.com ./init-cert.sh
#   docker compose exec nginx nginx -s reload
#
# Env:
#   API_HOST, TEST_API_HOST  default: from the env file next to this script
#   CERT_EMAIL  required, for Let's Encrypt expiry notices
#   STAGING_CA  "1" uses Let's Encrypt's staging CA (test certs)
#   WEBROOT     "1" goes through the running nginx instead of standalone
set -euo pipefail
cd "$(dirname "$0")"

die() { printf 'error: %s\n' "$*" >&2; exit 1; }

env_file=".env"
read_env() {
  local v=""
  if [[ -f "$env_file" ]]; then v="$(sed -n "s/^$1=//p" "$env_file" | tail -n1)"; fi
  # Strip optional quotes, as compose does, so the cert path matches nginx's.
  v="${v%\"}"; v="${v#\"}"; v="${v%\'}"; v="${v#\'}"
  printf '%s' "$v"
}
API_HOST="${API_HOST:-$(read_env API_HOST)}"
TEST_API_HOST="${TEST_API_HOST:-$(read_env TEST_API_HOST)}"
[[ -n "$API_HOST" ]] || die "API_HOST is required"
[[ -n "$TEST_API_HOST" ]] || die "TEST_API_HOST is required"
[[ -n "${CERT_EMAIL:-}" ]] || die "CERT_EMAIL is required"

extra=()
if [[ "${STAGING_CA:-0}" == "1" ]]; then extra=(--test-cert); fi
# --cert-name keeps the lineage at live/$API_HOST (nginx's path) when
# the name list changes.
certonly=(certonly --non-interactive --agree-tos --no-eff-email
  --keep-until-expiring --email "$CERT_EMAIL"
  --cert-name "$API_HOST" -d "$API_HOST" -d "$TEST_API_HOST" "${extra[@]}")

if [[ "${WEBROOT:-0}" == "1" ]]; then
  docker compose ps --status running --services | grep -qx nginx \
    || die "WEBROOT=1 needs nginx running; on a fresh box run without it"
  docker compose run --rm --no-deps --entrypoint certbot certbot \
    "${certonly[@]}" --webroot -w /var/www/certbot
  echo "cert ready for $API_HOST + $TEST_API_HOST. Next: docker compose exec nginx nginx -s reload"
  exit 0
fi

# A restarting nginx (no cert yet) also holds port 80.
if docker compose ps --status running --status restarting --services | grep -qx nginx; then
  die "nginx holds port 80; run: docker compose stop nginx (or use WEBROOT=1)"
fi
docker compose run --rm --no-deps -p 80:80 --entrypoint certbot certbot \
  "${certonly[@]}" --standalone

echo "cert ready for $API_HOST + $TEST_API_HOST. Next: docker compose up -d"
