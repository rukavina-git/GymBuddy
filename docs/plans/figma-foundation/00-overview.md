# Plan 3: Figma foundation

## Why
From here on, UI changes follow a Figma frame (CLAUDE.md). Today the
Figma file (MdD33rDNKJ3S82azTOYfEk) is empty, so no UI step can link
one. Plan 4 already needs frames: 4.2a adds a "session expired" path
to Login. This plan mirrors the current v1 UI in Figma so code and
design start from the same place (roadmap-details.md, Plan 3).

## Scope
1. (done, roadmap 3.1) Figma file, MCP connected.
2. Design tokens: colours, type scale, spacing, shapes from
   `ui/theme/` as Figma variables, light and dark. Same values in
   `design/tokens.json`.
3. Component library: one Figma component per file in
   `ui/components/`, built on the tokens.
4. Screens: inventory of every v1 screen and its states (empty,
   loading, error), then one frame per screen/state from the
   components. Includes the Login "session expired" state for 4.2a.
5. Code follows Figma: Compose theme reads the token values; step
   files for UI work link a frame from here on.

Out of scope: the v2 UI overhaul, new features, any behaviour change,
iOS, Code Connect.

## Invariants
- v1 scope stays frozen (architecture plan §14). Figma mirrors and
  tidies; it does not redesign.
- Steps 02–04 change no app code. Step 05 changes only `ui/theme/`
  and how it's read; the app looks the same before and after.
- `design/tokens.json` and the Figma variables hold the same values.
  Figma is the source; the JSON is its committed copy.
- Figma calls stay lean (Starter plan): read only the nodes a step
  needs.
- No Room schema change, no `api/openapi.yaml` change.
- Each step builds and passes its tests on its own.
