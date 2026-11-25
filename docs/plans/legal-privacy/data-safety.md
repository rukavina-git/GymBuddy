# Play Console answers (for 9.5)

Written in Plan 9 step 04 from the code and `legal/privacy.md`. If
the app starts collecting something new, update this file and the
privacy policy first.

## Data safety

**Data collection and security**
- Does your app collect or share any of the required user data types? **Yes**
- Is all of the user data collected by your app encrypted in transit? **Yes** (HTTPS only: our API, Firebase, the legal pages)
- Which account creation methods does your app support? **Username and password**, **OAuth** (Google)
- Delete account URL: **https://gymtracker.rukavina.app/delete-account**
- Can users request that some data be deleted without deleting the account? **No** (they can delete single workouts or entries in the app, but there is no data-only request)
- Independent security review: **No**

**Sharing.** Nothing is "shared": Firebase (Google) and netcup are
service providers acting for us, which Play does not count as
sharing. Answer "Shared: No" for every type.

**As submitted (2026-11-17, owner):** Email, User IDs, Health info and
Fitness info were marked **Required** (anyone with an account sends
them); Name and Other info Optional; crash logs and device IDs
Required. Over-declaring is safe; the table below is the stricter view.

**Collected types.** Ephemeral: No for all. Optional: see the column.
Guests send only crash data (row 7 and 8); everything else needs an
account, and guest mode makes an account optional, so account data is
"Optional: Yes". Rows 2 and 2b are one Play type (fill once).

| # | Play category → type | What it is | Purpose | Optional |
|---|---|---|---|---|
| 1 | Personal info → Email address | account email (Firebase Auth, profile) | App functionality, Account management | Yes (only with an account) |
| 2 | Personal info → Name | profile name; Google gives it on Google sign-in | App functionality, Account management | Yes |
| 2b | Personal info → Other info | Google profile picture link (stored, from Google sign-in) | Account management | Yes |
| 3 | Personal info → User IDs | Firebase uid | App functionality, Account management | Yes (only with an account) |
| 4 | Personal info → Other info | birth date, gender, bio (About you) | App functionality | Yes |
| 5 | Health and fitness → Health info | body weight, height, target weight | App functionality | Yes |
| 6 | Health and fitness → Fitness info | workouts, sets, plans, exercises, notes; experience, activity level, goal, training days | App functionality | Yes |
| 7 | App info and performance → Crash logs | Crashlytics reports | Analytics (fixing bugs) | No |
| 8 | Device or other IDs | Crashlytics installation id | Analytics (fixing bugs) | No |
| 9 | App activity → Other user-generated content | in-app feedback message (Plan 10 step 06, 0.2.4) | App functionality | Yes |

Feedback also carries app version, Android version and phone model:
the same fields as crash logs (row 7), so no new type. Guests can
send feedback too, so row 9 is collected from guests as well.

Not collected: location, contacts, photos/videos (avatars are
presets), audio, files, calendar, messages (feedback is "other
user-generated content", not messages between users), web history,
app interactions/analytics, financial info, installed apps. No ads SDK.

## Foreground service (Android 14+ declaration)

`WorkoutService`, type **specialUse**, subtype "workout timer".

Paste as the justification:

> While a workout runs, Gym Tracker shows an ongoing notification with
> the current exercise and set and the elapsed/rest timer, and buzzes
> when a rest period ends, also with the screen off. The user starts
> it by starting a workout and it stops when they finish or discard
> it. No other foreground service type fits: the app uses no sensors,
> location, media or health APIs, and a rest buzz must fire on time,
> so WorkManager or an exact alarm can't replace it.

Video: start a workout, lock the phone, show the notification, wait
for the rest buzz, finish the workout and show the notification gone.

## Target audience and content

- Target age groups: **18 and over** and **16–17**. Not 13–15 or
  younger (terms and policy: 16+). No age gate in the app (step 04
  decision).
- Appeals to children: **No**.
- Ads: **No**.
- Health apps declaration: fitness tracking, **not** a medical
  device, no health connect/sensor data.
