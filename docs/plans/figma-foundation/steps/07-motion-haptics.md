# 07 Motion and haptics spec

## Problem
Motion lives in scattered places: design-brief.md round logs (Round 5
"the set lands", tab fade-through, live bar), motion tokens in
`design/tokens.json` (durations, easings, press-scale), and static
pressed states on the 05 components. Haptics are only mentioned
("press 0.97 + haptic", "rest buzz off"). Plan 4 has no single list
of what moves, how, and which haptic fires, so each screen would be
guessed in Compose.

## Change
1. **Inventory.** Claude lists every transition and feedback moment
   across "03 Locked designs" and design-brief.md: trigger, what
   moves, token (duration + easing), haptic, reduced-motion fallback.
   Start list: press (all buttons), set lands on Done, digit odometer
   on − / +, keypad open/close, swipe a set (done/delete) + Undo
   snackbar, shared axis X between exercises, ⌄ container transform
   into live bar and back, rest countdown and rest over (no motion,
   no buzz), tab fade-through, title collapse on scroll, sheets and
   dialogs, sync/offline banners, onboarding pages, sign-in errors.
   Each maps to an M3 / Compose API or is marked custom.
2. **impeccable `animate`.** Reviews the inventory against the
   brief's rules (no bounce, no confetti, no count-up numbers, never
   blocks input, calm workout screen). Fills gaps; drops anything
   that adds noise. Owner picks where it offers options.
3. **Haptics map.** One table: moment → Android
   `HapticFeedbackConstants` (e.g. CONFIRM on Done, REJECT at a limit,
   CLOCK_TICK on odometer only if impeccable keeps it), and the rest
   timer end (vibration pattern, follows the "rest buzz" setting).
   Rule: haptics stay when "Remove animations" is on; none fire
   without a user action except the rest buzz.
4. **Figma.** One "Motion" card on "01 System" (Section under
   Tokens): the motion tokens, the signature "set lands" as a short
   frame strip (start, mid, end), and the reduced-motion rule. No
   prototype animations on locked frames. Missing tokens (if
   impeccable needs any) are added as variables and re-exported to
   `design/tokens.json` (generated; never hand-edited).
5. **Write-up.** `design/motion.md`: the inventory table (moment,
   trigger, tokens, Compose API, haptic, reduced-motion), haptics
   map, and the rules. Plan 4 builds from it.

## Tests
None (no code). Claude checks: every row uses tokens only (no raw
ms/curves); every moment has a reduced-motion fallback; every
haptic names an Android constant; nothing contradicts the locked
round logs.

## Out of scope
Compose code (Plan 4), new screens or features, iOS haptics (Phase
6), sound, light mode, DESIGN.md (08), Lottie or other
third-party animation assets.

## Done when
- Owner okayed the inventory and any options from impeccable.
- "Motion" card on "01 System".
- `design/motion.md` committed; tokens.json re-exported if tokens
  changed.
- No impeccable P0/P1 open.
