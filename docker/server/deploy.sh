#!/usr/bin/env bash
# Switches the backend to another GHCR image and checks health (ADR 0016).
# Runs on the server as deploy, from deploy.yml or by hand:
#
#   cd /opt/gymbuddy && ./deploy.sh ghcr.io/rukavina-git/gymbuddy-backend:sha-abc1234
#
# Rewrites BACKEND_IMAGE in .env, recreates only the backend, then polls
# https://API_HOST/v1/health through the local nginx. If that fails, the
# old image goes back into .env and is started again, and the script
# exits non-zero. nginx re-resolves the backend itself; no reload needed.
#
# Env:
#   HEALTH_TIMEOUT  seconds to wait for health (default 90)
#   CURL_INSECURE   "1" skips TLS verification (local tests, self-signed)
#   ENV_FILE        env file next to compose.yaml (default .env)
set -euo pipefail
cd "$(dirname "$0")"

die() { printf 'error: %s\n' "$*" >&2; exit 1; }

[[ $# -eq 1 ]] || die "usage: $0 ghcr.io/rukavina-git/gymbuddy-backend:<tag>"
new_image=$1
[[ "$new_image" =~ ^ghcr\.io/rukavina-git/gymbuddy-backend:[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]] \
  || die "not a gymbuddy-backend GHCR tag: $new_image"

env_file="${ENV_FILE:-.env}"
[[ -f "$env_file" ]] || die "$env_file missing"

read_env() {
  local v
  v="$(sed -n "s/^$1=//p" "$env_file" | tail -n1)"
  v="${v%\"}"; v="${v#\"}"; v="${v%\'}"; v="${v#\'}"
  printf '%s' "$v"
}

old_image="$(read_env BACKEND_IMAGE)"
api_host="$(read_env API_HOST)"
[[ -n "$old_image" ]] || die "BACKEND_IMAGE not set in $env_file"
[[ -n "$api_host" ]] || die "API_HOST not set in $env_file"

set_image() {
  # Rewrite in place: keeps the file's owner and 0600 mode.
  local tmp
  tmp="$(sed "s|^BACKEND_IMAGE=.*|BACKEND_IMAGE=$1|" "$env_file")"
  printf '%s\n' "$tmp" > "$env_file"
}

curl_opts=(-fs -o /dev/null --max-time 5)
if [[ "${CURL_INSECURE:-0}" == "1" ]]; then curl_opts+=(-k); fi

healthy() {
  local deadline=$((SECONDS + ${HEALTH_TIMEOUT:-90}))
  while (( SECONDS < deadline )); do
    if curl "${curl_opts[@]}" \
        --resolve "$api_host:443:127.0.0.1" "https://$api_host/v1/health"; then
      return 0
    fi
    sleep 3
  done
  return 1
}

start() {
  docker compose --env-file "$env_file" up -d --no-deps backend
}

echo "deploy: $old_image -> $new_image"
docker pull --quiet "$new_image" >/dev/null \
  || die "cannot pull $new_image; nothing changed"

set_image "$new_image"
if start && healthy; then
  echo "deploy: healthy on $new_image"
  exit 0
fi

echo "deploy: $new_image not healthy, rolling back to $old_image" >&2
docker compose --env-file "$env_file" logs --tail 50 backend >&2 || true
set_image "$old_image"
start
if healthy; then
  echo "deploy: rolled back, healthy on $old_image" >&2
else
  echo "deploy: rollback also unhealthy; check the server" >&2
fi
exit 1
