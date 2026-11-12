# 08 (me) Test stack live, CI green, PR develop → main

## Problem
Steps 06 and 07 are built and tested locally, but the live box still
runs the old prod-only stack, `api-test` has no DNS record, Android CI
is red without `GOOGLE_SERVICES_JSON_QA`, and the Deploy workflow has
never run (`workflow_dispatch` needs `deploy.yml` on `main`). The
owner runs this; Claude helps live.

## Checklist
Firebase (gymbuddy-test, made in 07):
1. Android app `com.rukavina.gymbuddy.qa` registered, debug SHA-1
   added (Google sign-in), Auth providers match prod. Its
   `google-services.json` is in `android/app/src/qa/`.
2. Service account key for gymbuddy-test → base64, one line
   (`wc -l` = 1). Bitwarden entry.

GitHub:
3. Repo secret `GOOGLE_SERVICES_JSON_QA` (qa json contents).
4. Push develop: Android CI green (test, build-apk with both APKs),
   backend CI green.

Box, in this order (wrong order breaks prod, see 06 gotcha):
5. `/opt/gymbuddy` env file: add `TEST_API_HOST=api-test.rukavina.app`
   FIRST.
6. DNS: A record `api-test` → 62.83.85.218, DNS-only, no AAAA. Wait
   until it resolves.
7. Copy the new `compose.yaml`, nginx template, `init-cert.sh` to
   `/opt/gymbuddy` together; `docker compose up -d` (~30 s downtime).
   Prod health 200.
8. `WEBROOT=1 CERT_EMAIL=… ./init-cert.sh`, then
   `docker compose exec nginx nginx -s reload`. Check the cert covers
   both names and the issuer is not the LE staging CA.
9. As root: `mkdir /opt/gymbuddy-test && chown deploy:deploy` it.
   Copy the test stack files + `deploy.sh`; env file (0600) with a
   new `DB_PASSWORD` (Bitwarden), gymbuddy-test
   `FIREBASE_CREDENTIALS_B64`, a `sha-` image tag. `up -d`.
   `https://api-test.rukavina.app/v1/health` 200.
10. GitHub environment `test`.

PR:
11. PR develop → main (no "Generated with Claude Code" footer).
    Merge once all checks are green.

After merge:
12. Deploy workflow, target `prod`, a `sha-` tag: green, health 200.
13. Deploy workflow, target `test`, same tag: green.
14. Backup workflow by hand: green (prod still dumps; test not backed
    up unless the workflow says so).
15. Phone: `installQaDebug`, sign in, sync a workout; it lands in the
    test DB, not prod (row count on both). Prod app still syncs.
16. Uptime check for `api-test` (optional, free plan).

## Out of scope
Auto-deploy (8.4). Moving `DEPLOY_*` to environment secrets (06
reviewer note; owner may do it here if wanted). Release signing.

## Done when
Both hosts serve 200 with valid certs, CI green on main, both Deploy
targets ran green, qa app syncs to test only, progress and roadmap
ticked.
