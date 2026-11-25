# Records of processing (GDPR Art. 30)

Controller: Ivan Karlo Rukavina, Croatia, privacy@rukavina.app.
No DPO (not required). Last reviewed: 2026-11-18.
Update this file together with `legal/privacy.md`.

| Activity | Data | People | Purpose and basis | Recipients | Where | Kept |
|---|---|---|---|---|---|---|
| Account | email, Firebase uid; Google sign-in: name, picture link | account holders | run the account, 6(1)(b) | Google (Firebase Auth) | EU/USA | until account deleted |
| Profile | name, avatar id, experience, bio, height, birth date, gender, activity level, goal, target weight, training days | account holders | sync, 6(1)(b); health parts 9(2)(a) | netcup | Germany | until account deleted |
| Training and body data | workouts, sets, plans, custom exercises, notes, favourites, body weight | account holders | backup and sync, 9(2)(a) explicit consent | netcup | Germany | until account deleted |
| Consent record | consent version and time | account holders | prove consent, 6(1)(c)/7(1) | netcup | Germany | until account deleted |
| Backups | full encrypted DB dump | account holders | restore after failure, 6(1)(f) | GitHub (encrypted, can't read) | USA | 30 days |
| Crash reports | error, app version, phone model, Android version, installation id, uid if signed in | all users | fix bugs, 6(1)(f) | Google (Crashlytics) | EU/USA | 90 days |
| Feedback | message, app version, Android version, phone model, uid if signed in | anyone who sends feedback | answer reports, improve the app, 6(1)(f) | netcup (ntfy.sh gets only "New feedback", no content) | Germany | 2 years; with the account if signed in |
| Server logs | IP, request, uid | all users with an account, web visitors | security and operations, 6(1)(f) | netcup | Germany | rotated, 10 MB × 3 |
| Privacy requests | email, request content | requesters | answer rights requests, 6(1)(c) | email provider | see provider | as long as needed to prove the answer |

Transfers to the USA: EU-US Data Privacy Framework and SCCs (Google DPA).
GitHub only holds AES-256 encrypted dumps (key not on GitHub).

Security: HTTPS only; Firebase ID tokens checked on every request;
DB not exposed outside the VPS; encrypted nightly backups; secrets in
Bitwarden and GitHub environment secrets.

DPAs: netcup AVV signed (owner, 2026-11-18). Google Firebase data
processing terms accepted (owner).

Breach: if personal data leaks, notify AZOP within 72 hours
(azop.hr), and users directly if the risk to them is high. Note what
happened, what data, and what was done.
