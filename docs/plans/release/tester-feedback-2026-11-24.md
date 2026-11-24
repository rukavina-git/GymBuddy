# Closed-test feedback report, 2026-11-24

Paid tester service ("Testers Community"), 7-page PDF. Verdict: no
crashes or bugs on any device/SDK tested. Suggestions below, with what
we did. Council for the listing: docs/council/2026-11-24-store-listing.md.

| # | Their point | Outcome |
|---|---|---|
| 1 | Description lacks keywords and text (ASO) | Done: new listing text (council), owner pastes in Play Console |
| 2 | Screenshots don't show features | After exercise images ship (owner) |
| 3 | Walkthrough is basic, not interactive | v2 (owner likes the idea), see below |
| 4 | Logout has no confirmation | Done in 0.2.3 |
| - | Feedback mechanism | Exists (You → Send feedback, email). v2: in-app form |
| - | Regular updates | Ongoing (closed-test cadence) |
| - | Highlight offline in listing + onboarding | Listing done; welcome screen already says it |
| - | Progress reports / graphs | Already v2 (Statistics and charts) |
| - | Accessibility: larger text, voice commands | v2 discussion |

## For v2: their text, kept for the revisit

**3) Dynamic walkthrough.** "The current walkthrough provided to new
users is basic and lacks interactive elements. A more engaging and
dynamic walkthrough can help users understand app functionalities
better and lead to higher retention rates."
- Interactive tutorials: step-by-step, let users practice logging a
  workout or setting timers inside the walkthrough.
- Visual hints: animations or highlights on important features.
- Option to skip, and reopen it later from settings.

Our notes: must stay calm (no coach marks on every screen, one-time,
skippable, never blocks a workout). A first guided workout (log one
set, see the rest timer) is the likely shape.

**User feedback mechanism.** "Encourage users to provide feedback on
their experience... can help identify areas for improvement and
feature requests."
Owner: today's Send feedback is hard to find and opens an email app.
v2: an in-app text box + Send (no email app), easier to reach.
Needs a backend endpoint or a form service; no personal data beyond
what the user types (+ app version).

**Store rating prompt** (owner idea, same report context). Ask for a
Play rating at a good moment (e.g. after the Nth finished workout),
via the Play In-App Review API; never mid-workout, capped like
no-spam reminders.

**Progress tracking enhancements.** "Visual progress reports or graphs
to motivate users and make their results easier to understand at a
glance." Already v2 (Statistics and charts, personal records).

**Accessibility.** "Ensure that the app is accessible to all users,
including those with disabilities. Implement features like voice
commands for logging workouts or larger text options."
Our notes: check every screen at the largest system font size and
with TalkBack first; voice logging is a separate, bigger idea.
