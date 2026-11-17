#!/usr/bin/env bash
# Deletes one account for a privacy@ request from someone without the
# app (Plan 9 step 03; gymtracker.rukavina.app/delete-account). Runs the
# backend image once under the "account-delete" profile: the same
# AccountDeletionService as the in-app delete (all synced data, then
# the Firebase login), no web server, then it exits.
#
# On the server as deploy, from the stack's folder (prod or test):
#
#   cd /opt/gymbuddy && ./delete-account.sh <firebase-uid>
#
# Find the uid by the requester's email in the Firebase console
# (Authentication -> Users, "User UID" column). Only act on a request
# sent from that same email address.
#
# Exit 0: data and Firebase login deleted. Non-zero: read the output;
# if it says the Firebase user was not deleted, the data is already
# gone and only the login needs removing in the console.
set -euo pipefail
cd "$(dirname "$0")"

die() { printf 'error: %s\n' "$*" >&2; exit 1; }

[[ $# -eq 1 ]] || die "usage: $0 <firebase-uid>"
uid=$1
[[ "$uid" =~ ^[A-Za-z0-9]{1,128}$ ]] || die "not a Firebase uid: $uid"

printf 'Deleting account %s in %s. Type the uid again to confirm: ' "$uid" "$PWD"
read -r again
[[ "$again" == "$uid" ]] || die "uid did not match, nothing deleted"

# run, not exec: a separate short-lived container next to the live
# backend. No --use-aliases, so nginx can never route to it. Smaller
# heap: the box has 2 GB and both stacks are running.
if docker compose run --rm --no-deps -T \
    -e SPRING_PROFILES_ACTIVE=account-delete \
    -e ACCOUNT_DELETE_UID="$uid" \
    -e JAVA_TOOL_OPTIONS="-Xmx160m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m" \
    backend; then
  printf '\nDone: %s deleted (data + Firebase login). Reply to the request.\n' "$uid"
else
  die "deletion failed for $uid, see the output above (ACCOUNT_DELETE_*)"
fi
