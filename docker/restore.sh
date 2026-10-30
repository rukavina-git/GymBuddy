#!/usr/bin/env bash
# Restore an encrypted backup from the DB Backup workflow.
#
#   BACKUP_PASSPHRASE=... docker/restore.sh <dump.gpg> <target-database-url>
#
# <target-database-url> is a libpq URL with the password included, e.g.
# postgresql://gymbuddy:local@localhost:5432/gymbuddy. Existing objects
# in the target are dropped and replaced (--clean). Needs gpg and
# pg_restore (same or newer major version than the dump) on PATH.
# Prompts for the passphrase when BACKUP_PASSPHRASE is unset.
set -euo pipefail

if [ $# -ne 2 ]; then
  echo "usage: $0 <dump.gpg> <target-database-url>" >&2
  exit 2
fi

dump=$1
target=$2

if [ ! -f "$dump" ]; then
  echo "no such file: $dump" >&2
  exit 1
fi

if [ -z "${BACKUP_PASSPHRASE:-}" ]; then
  read -rsp "Backup passphrase: " BACKUP_PASSPHRASE
  echo
fi

tmp=$(mktemp)
trap 'rm -f "$tmp"' EXIT

gpg --batch --yes --quiet --decrypt --pinentry-mode loopback \
  --passphrase-fd 3 --output "$tmp" "$dump" 3<<<"$BACKUP_PASSPHRASE"

pg_restore --clean --if-exists --no-owner --no-acl --exit-on-error \
  --dbname "$target" "$tmp"

echo "Restored $dump"
