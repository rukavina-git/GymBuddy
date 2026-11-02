# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

iOS client is planned (ios/, not started). Each OS follows its own
native conventions; the brand carries through colour and type only.

## Users
Broad gym-goers, from beginners to experienced lifters. They use the
app in the gym, mid-session: phone in one hand, between sets, often
sweaty, in harsh or dim gym lighting, with limited attention. Outside
the gym they plan templates and look back at history.

## Product Purpose
A workout tracker: plan workouts as templates, log them set by set,
and keep an honest history of what was lifted. Success is a user who
logs every session without friction and can see that they are
progressing. Beginners need guidance as much as tracking.

## Positioning
- Private and offline-first. Works completely without an account;
  local storage is the source of truth. An account is opt-in, only
  for backup and multi-device sync.
- No social layer. No feed, followers, likes or sharing prompts. A
  tool, not a network.
- Beyond weight × reps. Six tracking types (weight+reps, reps,
  duration, distance, and weighted variants) are first-class, so a
  plank or a farmer's walk is logged as what it is.

## Operating Context
- Core loop: pick a template (or start empty) → log sets per
  exercise → finish → session lands in history.
- Exercise library of 60 reference exercises with photos, muscle
  groups, equipment, descriptions; users add custom exercises, notes,
  and hide exercises they don't use.
- Metric storage, imperial display option.
- Sign-in: Google, email, Apple. Multi-device sync, account deletion.

## Capabilities and Constraints
- Native Android: Kotlin, Jetpack Compose, Material 3.
- v1 feature scope is frozen (architecture plan §14). Visual and UX
  design is in scope; new features are not.
- Statistics tab exists but is a placeholder; what it shows is
  undecided.
- UI is designed in Figma first (file MdD33rDNKJ3S82azTOYfEk).
- Dark only in v1 (see Brand Commitments); light theme is v2.

## Brand Commitments
- Name: GymBuddy.
- Direction: Soft Dark (locked 2026-10-31). Dark is the only mode in
  v1; light theme is v2.
- Brand colours: lime #D4FF24 = "you can press this" (only on
  actions and done), cobalt #4D86F0 = "you are here", ground #121418.
  Every other colour: design/colors.md.
- Type: Figtree.

## Evidence on Hand
- Reference exercise photos (stock, red studio backgrounds) bundled
  with reference data.
- No testimonials, user numbers or benchmarks exist. Don't invent any.

## Product Principles
1. The set is sacred: logging one must be possible mid-workout,
   one-handed, without hunting.
2. Your data is yours: an account is encouraged (backup, other
   devices) but never required; "continue without account" is always
   offered, and nothing is shared unless the user asks.
3. Show progress honestly: numbers from the user's own history, no
   fake motivation.
4. Guide without nagging: beginners get help, experienced users get
   out of the way.

## Accessibility & Inclusion
Usable in bright and dim gyms and with sweaty hands: WCAG AA contrast
minimum, large touch targets (48dp+) for in-workout controls,
respects system font scaling.
