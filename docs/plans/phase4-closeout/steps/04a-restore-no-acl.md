# 04a restore.sh skips Neon GRANTs (--no-acl)

## Problem
Step 04's restore test failed: a Neon dump carries default privileges
and GRANTs to Neon-only roles, and `pg_restore --exit-on-error` stops at
the first one:

    ERROR:  role "neon_superuser" does not exist
    Command was: ALTER DEFAULT PRIVILEGES FOR ROLE cloud_admin IN SCHEMA
    public GRANT ALL ON SEQUENCES TO neon_superuser WITH GRANT OPTION;

Any non-Neon target (compose, a VPS, a fresh Neon project's other
roles) hits this. Flagged as a reviewer SHOULD in step 03.

## Change
- `docker/restore.sh`: add `--no-acl` to `pg_restore`. The app connects
  as the database owner, so no GRANTs are needed after a restore.
- `docker/README.md` Restore section: one line saying privileges are
  not restored (Neon roles don't exist elsewhere).

## Tests
Same as step 04's local test (manual, needs the Neon URL and
passphrase): dump with `postgres:17` + gpg as in `db-backup.yml`, run
`restore.sh` into a throwaway `postgres:17`, `\dt` lists the app tables
and `flyway_schema_history`. Already passed in step 04 with a sed'ed
copy of `restore.sh`; rerun against the committed script.

Without secrets: `bash -n docker/restore.sh`.

## Out of scope
Workflow changes, compose Postgres version bump, other step 03
reviewer items.

## Done when
`restore.sh` passes `--no-acl`, README says so, and the local restore
test ends with the table list.
