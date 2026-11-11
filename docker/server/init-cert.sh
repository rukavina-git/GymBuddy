#!/usr/bin/env bash
# Gets the first Let's Encrypt certificate for API_HOST (ADR 0016).
# nginx can't start without one, so this runs certbot standalone on
# port 80 before nginx is up. The certbot service renews it later.
#
#   cd /opt/gymbuddy && CERT_EMAIL=you@example.com ./init-cert.sh
#
# Env:
#   API_HOST    default: API_HOST from the env file next to this script
#   CERT_EMAIL  required, for Let's Encrypt expiry notices
#   STAGING_CA  "1" uses Let's Encrypt's staging CA (test certs)
set -euo pipefail
cd "$(dirname "$0")"

die() { printf 'error: %s\n' "$*" >&2; exit 1; }

env_file=".env"
if [[ -z "${API_HOST:-}" && -f "$env_file" ]]; then
  API_HOST="$(sed -n 's/^API_HOST=//p' "$env_file" | tail -n1)"
  # Strip optional quotes, as compose does, so the cert path matches nginx's.
  API_HOST="${API_HOST%\"}"; API_HOST="${API_HOST#\"}"
  API_HOST="${API_HOST%\'}"; API_HOST="${API_HOST#\'}"
fi
[[ -n "${API_HOST:-}" ]] || die "API_HOST is required"
[[ -n "${CERT_EMAIL:-}" ]] || die "CERT_EMAIL is required"

# A restarting nginx (no cert yet) also holds port 80.
if docker compose ps --status running --status restarting --services | grep -qx nginx; then
  die "nginx holds port 80; run: docker compose stop nginx"
fi

extra=()
if [[ "${STAGING_CA:-0}" == "1" ]]; then extra=(--test-cert); fi

docker compose run --rm --no-deps -p 80:80 --entrypoint certbot certbot \
  certonly --standalone --non-interactive --agree-tos --no-eff-email \
  --keep-until-expiring --email "$CERT_EMAIL" -d "$API_HOST" "${extra[@]}"

echo "cert ready for $API_HOST. Next: docker compose up -d"
