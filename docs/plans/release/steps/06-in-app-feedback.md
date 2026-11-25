# 06 In-app feedback (0.2.4)

Runs before 05 (owner, 2026-11-25). Roadmap 10.4b.

## Why
Tester report 2026-11-24 + owner: You → About → "Send feedback" is
hard to find and opens an email app, so almost nobody uses it. v1
gets a text box + Send inside the app.

## Decisions (owner, 2026-11-25)
- Backend endpoint, stored in Postgres. No form service.
- Works for guests too (ADR 0014: no anonymous auth), so the endpoint
  accepts posts without a token. Abuse guard: nginx per-IP limit,
  2000-char cap, server-side rate limit.
- Owner alert: ntfy push "New feedback" with no user text (ntfy.sh
  never sees content). Read it with a script on the VPS.
- Email option removed from the app.
- Offline-first: an unsent message is kept on the phone and sent
  later; the UI says so. Never lost, never a failed Send.
- New data collection (allowed by owner): message text, app version,
  Android version, device model, user id if signed in. Nothing else.

## 06a API + backend
- **Spec change (api/openapi.yaml):** `POST /v1/feedback`, security
  optional (bearer or none). Body: `id` (UUIDv7, client-generated, so
  retries are idempotent), `message` (1-2000 chars), `appVersion`,
  `osVersion`, `deviceModel`, `createdAt` (epoch ms). 204 on insert or
  duplicate id; 400 on bad input; 429 with Retry-After.
- Flyway `V9__feedback.sql`: table `feedback` (id, user_id nullable,
  message, app_version, os_version, device_model, created_at,
  received_at). Deleted with the account (AccountDataDeleter).
- SecurityConfig: permit the path; if a valid token is sent, attach
  the user id. Rate limit by uid, or by client IP for guests
  (X-Real-IP from nginx).
- ntfy ping after insert, async, best-effort; topic from env
  `NTFY_FEEDBACK_TOPIC` (unset = off, tests unset). Separate topic from
  the SSH-login one. Give the Bitwarden entry for it.
- nginx: `limit_req` zone for /v1/feedback (e.g. 5/min per IP, burst 5),
  `client_max_body_size` small.
- `docker/server/feedback.sh [env] [n]`: prints the newest n rows.
- Tests: controller (guest, signed-in, duplicate, too long, 429),
  drift test green.

## 06b Android
- Screen You → Send feedback: one multiline field, char counter near
  the cap, Send. Short line under it: what is sent (text, app and
  phone version, account if signed in). No Figma frame (owner,
  2026-11-25): small screen, built from existing components and
  design/DESIGN.md.
- Room table `pending_feedback` (schema bump + migration + test).
  Send = insert row + enqueue WorkManager job (network constraint,
  backoff). Job posts each row and deletes it on 204/400. Token
  attached if signed in, none for guests.
- After Send: "Thanks, sent" or "Saved, sends when you're online".
- Remove the mailto row and FEEDBACK_EMAIL.
- Tests: ViewModel, worker (offline, 429, success). Device check on
  qa: guest + signed-in, airplane mode then online, row lands on test.

## 06c Legal + Play
- legal/privacy.md: feedback section (what, why, retention, deleted
  with account; guests can't be matched, so ask by email to delete).
- (me) Play Console Data safety: add "Other user-generated content"
  (+ device ids if the model counts), collected, not shared, optional.
  Update docs/plans/legal-privacy/data-safety.md.
- docs/third-party.md: ntfy for feedback pings.

## 06d Feedback admin page
Owner, 2026-11-25: reading feedback over SSH is not acceptable; wants
a UI to read it and mark its status. Backend-only (no app release),
but live on prod before 0.2.4 reaches testers. Order: 06b, 06c, 06d,
then ship.

- Where: `/admin/` on each API host (api-test.rukavina.app/admin/,
  api.rukavina.app/admin/). No new DNS name or cert. One static HTML
  page + JS + CSS served by the backend (Spring static resources), no
  build tool, no framework. Works in a phone browser.
- Sign-in: Firebase Auth web SDK, Google sign-in, against that
  stack's Firebase project. The page sends the ID token as bearer.
  Firebase web config comes from `GET /admin/config.js`, built from
  env (`ADMIN_FIREBASE_API_KEY`, `ADMIN_FIREBASE_AUTH_DOMAIN`,
  `ADMIN_FIREBASE_PROJECT_ID`); unset = page shows "admin off".
- Who: env `ADMIN_UIDS` (comma list, owner's uid per project). Any
  other valid token gets 403; no token 401. Unset = every admin call
  403.
- **Spec change (api/openapi.yaml):** tag `admin`, bearer required.
  - `GET /v1/admin/feedback?status=&cursor=&limit=`: newest first;
    id, message, status, note, account ("guest" or uid), appVersion,
    osVersion, deviceModel, createdAt, receivedAt, updatedAt; opaque
    cursor like sync.
  - `PATCH /v1/admin/feedback/{id}`: `status` and/or `note`; 200 with
    the row, 404 unknown id.
- Flyway `V10__feedback_status.sql`: `status` text not null default
  'open' (check: open, solved, wont_fix, spam), `note` text null (max
  500), `updated_at`. Index on (status, received_at).
- Page: filter chips All / Open / Solved / Won't fix / Spam (default
  Open), count per status. Each card: message (line breaks kept,
  rendered as text, never HTML), date, app / Android / device, guest
  or account, status picker, note field. Saves on change. Plain
  design from design/DESIGN.md colours, dark.
- ntfy: `Click` header = that stack's `/admin/` URL, so tapping the
  ping opens the list (env `ADMIN_URL`; unset = no click).
- nginx: `/admin/` and `/v1/admin/` limit_req (e.g. 30/min per IP).
  CSP for `/admin/`: scripts from self + Firebase's gstatic/
  googleapis hosts only, `frame-ancestors 'none'`, no inline script.
- Security: admin endpoints only on bearer + uid allowlist; message
  rendered with textContent; no feedback text in logs. Firebase
  authorized domains: add both API hosts (owner, console).
- Privacy: no change (text stays on our server; owner reads it).
- third-party.md: Firebase JS SDK (Apache 2.0), loaded from gstatic.
- Keep feedback.sh (works when the page is down).
- Tests: admin controller (401, 403 non-admin, 403 empty allowlist,
  list + filter + cursor, patch status/note, 404, bad status 400),
  drift test green. Device check: sign in on the phone browser on
  test, mark a curl message solved, filter shows it.
- (me) Firebase console: register a web app in both projects, add
  authorized domains; put the 3 config values + `ADMIN_UIDS` +
  `ADMIN_URL` in both `.env` files, `docker compose up -d backend`.
  Bitwarden entry for the uids/config (not secret, but kept together).

## Done when
Feedback from a guest and a signed-in qa build arrives on the test
backend, the owner's phone pings, the script shows it, and an offline
message arrives after reconnect. The owner reads it and marks its
status on /admin/ from the phone, on test and prod. Ships in 0.2.4.
