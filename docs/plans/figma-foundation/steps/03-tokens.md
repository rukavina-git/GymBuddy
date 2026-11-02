# 03 Tokens as Figma variables + design/tokens.json

## Problem
Every locked frame uses raw hex, font sizes, gaps and radii typed in
by hand. Components (05) and Plan 4 code need one named set to build
from. Only colour is written down (design/colors.md); type, space,
radius and motion live only in the frames. Six colour decisions in
colors.md "Decided" are not yet applied in Figma.

## Change
1. **Read the frames.** Claude reads "03 Locked designs" (Figma
   Console MCP) and lists every unique font size/weight/line height,
   gap and padding, corner radius and motion duration/easing in use.
   Near-duplicates (e.g. 14 vs 15 px gaps) are flagged for the owner
   to merge, not merged silently. impeccable (`typeset`, `layout`)
   proposes the scales; owner picks.
2. **Figma variables on "01 System".** Collections: Colour (names and
   values from colors.md, Brand/Surfaces/Text/Tints/Danger/Overlays),
   Space, Radius, Motion (durations as numbers). Type as text styles
   (Figtree, M3 role names: display, headline, title, body, label).
   One mode, "Dark". Check first whether Starter allows a second mode;
   note the answer, don't add Light.
3. **Bind the locked frames.** Swap raw values on "03 Locked designs"
   for the variables and styles. This applies the six "Decided"
   colour merges. Not-ours colours (Google button, Android system UI,
   board chrome) stay raw. Screenshot each flow before and after; the
   only visible changes should be the six merges.
4. **`design/tokens.json`.** Exported from the Figma variables (W3C
   design-tokens format: `$value`, `$type`), committed. colors.md
   points to it; no value lives in both by hand.
5. **System page card.** A "Tokens" card on "01 System" shows each
   colour swatch, the type ramp, space and radius scales.

## Tests
None (no code). Claude checks: every colour in colors.md is a
variable and in tokens.json; no raw hex left on "03 Locked designs"
except the not-ours list; text pairs still meet WCAG AA.

## Out of scope
Light mode values, icons (04), components (05), motion spec beyond
durations and easings already in the frames (07), any Compose code.

## Done when
- Figma variables + text styles exist on "01 System", one Dark mode.
- Locked frames are bound to them; the six merges are visible.
- `design/tokens.json` is committed and matches Figma.
- Owner has okayed the type, space and radius scales.
