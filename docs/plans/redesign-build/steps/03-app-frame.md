# 03 App frame

## Problem
The app still uses the v1 frame: M3 `NavigationBar` with Material
icons and tabs Home, Workouts, Exercises, Stats, Settings
(`BottomNavItem`), tab switch that slides, no live bar, and the nav bar
stays on screen during a workout. 02a/02b built `AppNavBar`,
`LargeTitleTopBar` (static) and `LiveBar`; nothing uses them yet.
Roadmap 7.3. **No spec change, no Room schema change.**
Figma: App frame https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=85-9890
(Bottom bar 85:9892, Live bar 85:10033, Home while a workout runs
85:10122, Workouts scrolled 85:10257, Motion 85:10756).
Specs: DESIGN.md §8 nav, §9; motion.md rows 14–16, 23.

## Owner decisions: 1A, 2A, 3A (2026-11-03)
1. **What Progress and You show until 7.8 / 7.10**
   - **A (recommended): reuse the old screens.** Progress tab →
     `StatisticsScreen`, You tab → `SettingsScreen` (Profile, About,
     Hidden*, AppPreferences stay pushed from it). Nothing lost.
   - **B: new empty placeholders.** Cleaner tab roots, but settings
     and profile become unreachable until 7.10.
2. **Live bar**
   - **A (recommended): in this step.** The workout route hides the
     nav bar (full screen). Leaving it while a workout runs shows
     `LiveBar` above the tabs; tap goes back. Text from
     `ActiveWorkoutUiState` via a pure mapper (title, "Set x of y",
     done/total progress). Enter grows up, leave slides down + fades,
     text crossfades (Figma Motion frame). Resting/timed variants
     come with 7.6 (rest state doesn't exist yet).
   - **B: defer to 7.6.** Smaller step; the frame stays without it.
3. **Routes**
   - **A (recommended): keep string routes,** add `progress` / `you`,
     delete `BottomNavItem`. Tabs keep their own back stack
     (`saveState` / `restoreState`).
   - **B: type-safe `@Serializable` routes.** Adds the
     kotlinx-serialization plugin (Apache 2.0) and rewrites ~30
     routes that 7.4–7.10 mostly delete.

## Change
- `ui/MainScreen.kt`: `AppNavBar` replaces the M3 bar; `NavTab` →
  route map; selected tab from the back stack hierarchy. Nav bar
  hidden on the active workout route.
- Transitions: tab switch fade-through `medium2` (no slide, Figma:
  "tabs have no order"); push/pop shared axis X `medium2` `emph`.
  Predictive back from Navigation 2.9 (no `PredictiveBackHandler`);
  check `enableOnBackInvokedCallback` in the manifest.
- `ui/design/Bars.kt`: `LargeTitleTopBar` collapses into a small bar
  (title-medium + same actions) as content scrolls, finger-linked,
  via a scroll state / nestedScroll. Used by tab roots from 7.5 on.
- `AppNavBar`: icons only when labels don't fit (large font scale);
  TalkBack still says the name, long-press shows it (Figma note).
- Live bar per decision 2 (`ui/frame/LiveBarState.kt` mapper).
- Delete `navigation/BottomNavItem.kt`. Old screens keep their own
  top bars until their step replaces them.

## Tests
- Unit: live bar mapper (no workout → hidden; title, "Set x of y",
  progress 0..1, empty workout).
- Compose UI (Robolectric): tab tap selects tab and shows its root;
  reselect keeps one entry; nav bar hidden on workout route; live bar
  shown when a workout runs off the workout route, tap navigates
  back; large title collapses on scroll (title semantics stay one
  heading); nav bar icons-only at fontScale 2 still exposes names.
- Existing tests stay green.

## Out of scope
New tab content (7.5–7.10). Onboarding/auth screens and the session
expired banner (7.4). Rest / timed live bar states (7.6). Light mode.

## Done when
android `testDebugUnitTest` and `assembleDebug` green. No manual checks.
