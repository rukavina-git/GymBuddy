#!/usr/bin/env bash
# Prints the newest in-app feedback (Plan 10 step 06). The phone gets
# an ntfy push saying only "New feedback"; the text is read here.
#
# On the server as deploy, from the stack's folder (prod or test):
#
#   cd /opt/gymbuddy && ./feedback.sh        # newest 20
#   cd /opt/gymbuddy && ./feedback.sh 50     # newest 50
#
# "account" is the Firebase uid, or "guest". Read-only.
set -euo pipefail
cd "$(dirname "$0")"

die() { printf 'error: %s\n' "$*" >&2; exit 1; }

n=${1:-20}
[[ "$n" =~ ^[0-9]{1,4}$ ]] || die "usage: $0 [count]"

docker compose exec -T db psql -U gymbuddy -d gymbuddy -P pager=off -x -c "
  SELECT to_char(received_at AT TIME ZONE 'Europe/Zagreb', 'YYYY-MM-DD HH24:MI') AS received,
         coalesce(user_id, 'guest') AS account,
         coalesce(app_version, '?') || ' / ' || coalesce(os_version, '?') || ' / ' || coalesce(device_model, '?') AS app_os_device,
         message
  FROM feedback
  ORDER BY received_at DESC
  LIMIT $n;"
