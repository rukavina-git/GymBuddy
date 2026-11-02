# 04 Brand and icons

## Problem
The locked frames use a placeholder brand: A2 app icon (lime plate
with G), W2 "gymbuddy." wordmark, and the Google-style G on the
Welcome/Login logo. Owner wants a new animal logo (e.g. a duck)
before v1 ends (2026-11-02). Icons on the locked frames are drawn ad
hoc: mixed strokes (1.33/1.67/2.5/2.75, step 03 Gotchas), mixed
radii, no shared set. Plan 4 needs one named icon set to export.

## Change
1. **Icon inventory.** Claude reads "03 Locked designs" (Figma
   Console MCP) and lists every icon in use: name, where, outline or
   filled, size. Splits it into Material Symbols (Rounded, use as is)
   and custom gym glyphs (B1: 12, outline, 2dp round stroke). Owner
   okays the list of 12 before any drawing.
2. **Logo round (round loop, 00-overview).** Brief from PRODUCT.md.
   impeccable `concept-seed` deals 3+ animal marks (owner asks for
   duck, meerkat and raccoon) plus one category standard, each shown as app icon on lime, small
   (24dp, notification), monochrome, and next to the wordmark. Claude
   builds them on "02 Rounds". Owner picks; `critique` → refine →
   owner picks.
3. **Wordmark.** Keep W2 "gymbuddy." (lime dot) unless the logo round
   says otherwise; outline it to vector, token colours.
4. **Glyphs.** Draw the 12 on a 24dp grid, 2dp round stroke
   (stroke.icon), outline + filled where the nav or state needs it.
   impeccable `critique` + `audit` (legible at 20/24dp, 200% scale).
5. **Library on "01 System".** Logo, app icon (adaptive: foreground,
   background, monochrome layers; 108dp safe zone), wordmark and all
   icons as components, named for Plan 4 export. Swap them into
   "03 Locked designs"; snap stray strokes/radii to tokens.
6. **Export.** SVGs to `design/icons/` and `design/brand/`
   (committed), plus a short `design/icons.md`: name → use.

## Tests
None (no code). Claude checks: every icon on "03 Locked designs" is
a library instance; strokes are stroke.icon; app icon layers fit the
adaptive safe zone; logo reads at 24dp and in monochrome.

## Out of scope
Components (05), motion (07), muscle-map art (rejected/C1 is its own
asset), Android resources or any Compose code (Plan 4), store
listing art.

## Done when
- Owner picked the logo; wordmark confirmed.
- 12 glyphs + used Material icons are components on "01 System".
- Locked frames use the library; before/after screenshots match
  except the new logo.
- SVGs and `design/icons.md` committed.
