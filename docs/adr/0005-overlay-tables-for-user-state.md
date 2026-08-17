# 5. User state about reference data lives in overlay tables

Date: 2026-08-09
Status: Accepted

## Context

`isHidden` and `note` were columns on the exercise row itself, and
hiding was implemented as `UPDATE exercises SET isHidden = 1 WHERE id`.

That works while every row belongs to the device. It breaks once default
exercises are server-owned reference data:

1. The server owns the row for a default exercise, shared by every user.
2. The user hides it; the client writes `isHidden = 1` into that row.
3. The library is updated and the client pulls fresh reference data,
   replacing those rows.
4. The exercise is visible again. The preference is gone.

There is a second, independent failure: the flag cannot sync, because
doing so would require the server to modify a row shared with every
other user. There is nowhere to put it.

The underlying error is one of category. `isHidden` is not a property of
the exercise — it is the user's opinion *about* the exercise.

## Decision

User opinions about reference entities live in separate user-owned
tables: `user_exercise_state` and `user_template_state`, keyed by the
referenced entity id.

Every list query left-joins the overlay and coalesces missing rows to
defaults. Reference data is read-only on the client, enforced in the
repository layer rather than by hiding UI controls.

## Consequences

- A reference-data refresh cannot destroy user preferences.
- The flags sync as ordinary user data.
- The tables are sparse — a row exists only where the user has expressed
  an opinion. Sixty exercises with four hidden produces four rows.
- The tables are upsert-only with no tombstones, deviating from ADR 10's
  soft-delete rule. "Cleared" means all flags false and note null. The
  rows are tiny and never garbage-collected, and the asymmetry removes a
  delete-versus-update race from the sync path.
- Custom exercises use the overlay too, even though their `isHidden`
  could live on the row they own. One mechanism is worth a negligible
  redundancy; two mechanisms means every query handles both.
- Orphaned overlay rows, where the referenced exercise no longer exists,
  are left in place. Harmless, and the preference returns if the
  exercise does.
- Every future feature of this shape — favourites, per-exercise rest
  preferences, personal notes — uses the same table rather than a new
  mechanism.
