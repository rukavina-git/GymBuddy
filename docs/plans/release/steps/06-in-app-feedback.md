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
but live on prod before 0.2.4 reaches testers. Order: 06b, 06c, 06d1,
06d2, then ship 0.2.4. Two steps to keep each small.

Shape (both steps):
- Where: `/admin/` on each API host (api-test.rukavina.app/admin/,
  api.rukavina.app/admin/). No new DNS name or cert.
- Who: env `ADMIN_UIDS` (comma list, owner's uid in that stack's
  Firebase project). No token 401, valid token not on the list 403,
  list unset = every admin call 403.
- Statuses: open (default), solved, wont_fix, spam. Optional note
  (max 500 chars), e.g. "fixed in 0.2.5".
- Privacy: no change (text stays on our server; owner reads it).
- Keep feedback.sh (works when the page is down).

### 06d1 Backend + spec
- **Spec change (api/openapi.yaml):** tag `admin`, bearer required.
  - `GET /v1/admin/feedback?status=&cursor=&limit=`: newest first;
    id, message, status, note, account ("guest" or uid), appVersion,
    osVersion, deviceModel, createdAt, receivedAt, updatedAt; opaque
    cursor like sync; plus counts per status for the filter chips.
  - `PATCH /v1/admin/feedback/{id}`: `status` and/or `note`; 200 with
    the row, 404 unknown id, 400 bad status / note too long.
- Flyway `V10__feedback_status.sql`: `status` text not null default
  'open' (check constraint on the 4 values), `note` text null,
  `updated_at`. Index on (status, received_at).
- Admin check: uid from the Firebase token (existing filter), against
  `ADMIN_UIDS`. Never treat Spring's anonymous principal as a uid
  (06a gotcha).
- Retention (privacy policy, 06c): a daily scheduled job deletes
  feedback with `received_at` older than 2 years, any status. Test it.
- ntfy: `Click` header = env `ADMIN_URL` (that stack's `/admin/`), so
  tapping the ping opens the list; unset = no click.
- 06a reviewer leftovers, fix here: bound `createdAt` (Long.MAX gave
  500; reject far past/future with 400), feedback.sh strips control
  chars before printing.
- compose.yaml (both stacks): pass `ADMIN_UIDS`, `ADMIN_URL`.
- Tests: admin controller (401, 403 non-admin, 403 empty list, list +
  status filter + cursor + counts, patch status/note, 404, 400),
  retention job, createdAt bounds, ntfy Click header; drift test
  green; backend build green.

### 06d2 Page + nginx + server
- One static page (HTML + JS + CSS, no build tool, no framework)
  served by the backend from Spring static resources at `/admin/`;
  security permits the static files, the API stays bearer-only.
- Sign-in: Firebase Auth web SDK (pinned version, from gstatic),
  Google sign-in with **popup** (redirect breaks on mobile browsers
  with storage partitioning), against that stack's Firebase project.
  Web config from `GET /admin/config.js`, built from env
  (`ADMIN_FIREBASE_API_KEY`, `ADMIN_FIREBASE_AUTH_DOMAIN`,
  `ADMIN_FIREBASE_PROJECT_ID`); unset = page shows "admin off".
  Sends the ID token as bearer; refreshes it before calls.
- Page: filter chips Open / Solved / Won't fix / Spam / All (default
  Open) with counts. Each card: message (line breaks kept, set with
  textContent, never innerHTML), date in Europe/Zagreb, app /
  Android / device, guest or account, status picker, note field.
  Saves on change, shows saved/failed. Load more at the end. Dark,
  colours from design/DESIGN.md, phone width first.
- nginx: `/admin/` and `/v1/admin/` limit_req (~30/min per IP). CSP
  for `/admin/`: script-src self + www.gstatic.com; connect-src self
  + identitytoolkit / securetoken.googleapis.com; frame-src the
  project's `*.firebaseapp.com` (popup/auth iframe); no inline
  script; frame-ancestors 'none'.
- third-party.md: Firebase JS SDK (Apache 2.0), loaded from gstatic.
- Deploy (owner, guided like 06a): Firebase console: register a web
  app in both projects, add both API hosts as authorized domains;
  5 env values per stack (`ADMIN_FIREBASE_*` x3, `ADMIN_UIDS`,
  `ADMIN_URL`); scp both compose.yaml files and the nginx template;
  `nginx -t` in a throwaway container before `restart nginx`;
  `docker compose up -d backend` after env changes (check with
  printenv). Bitwarden entry for the uids/config.
- Check: phone browser on test: sign in, see the curl and device
  messages, mark one solved, filter shows it; a second Google account
  gets 403; tapping the ntfy ping opens the page.

## Done when
Feedback from a guest and a signed-in qa build arrives on the test
backend, the owner's phone pings, the script shows it, and an offline
message arrives after reconnect. The owner reads it and marks its
status on /admin/ from the phone, on test and prod. Ships in 0.2.4.
