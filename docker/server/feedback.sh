#!/usr/bin/env bash
# Prints the newest in-app feedback (Plan 10 step 06). The phone gets
# an ntfy push saying only "New feedback"; the text is read here.
#
# On the server as deploy, from the stack's folder (prod or test):
#
#   cd /opt/gymbuddy && ./feedback.sh        # newest 20
#   cd /opt/gymbuddy && ./feedback.sh 50     # newest 50
#
# "account" is the Firebase uid, or "guest". Read-only. Control
# characters (terminal escapes) are stripped; line breaks are kept.
set -euo pipefail
cd "$(dirname "$0")"

die() { printf 'error: %s\n' "$*" >&2; exit 1; }

n=${1:-20}
[[ "$n" =~ ^[0-9]{1,4}$ ]] || die "usage: $0 [count]"

# Every client-sent text goes through clean(): drops C0 controls except
# tab and newline, DEL and C1 controls (ESC sequences would run in the
# terminal).
docker compose exec -T db psql -U gymbuddy -d gymbuddy -P pager=off -x -c "
  CREATE FUNCTION pg_temp.clean(t text) RETURNS text LANGUAGE sql IMMUTABLE AS
    \$\$ SELECT regexp_replace(t, '[\u0001-\u0008\u000b-\u001f\u007f-\u009f]', '', 'g') \$\$;
  SELECT to_char(received_at AT TIME ZONE 'Europe/Zagreb', 'YYYY-MM-DD HH24:MI') AS received,
         status,
         pg_temp.clean(coalesce(user_id, 'guest')) AS account,
         pg_temp.clean(coalesce(app_version, '?') || ' / ' || coalesce(os_version, '?') || ' / ' || coalesce(device_model, '?')) AS app_os_device,
         pg_temp.clean(message) AS message,
         pg_temp.clean(note) AS note
  FROM feedback
  ORDER BY received_at DESC
  LIMIT $n;"
