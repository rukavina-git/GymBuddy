# Council 2026-11-01: Exercises list leading visual

## Brief
```
COUNCIL BRIEF: what (if anything) leads each row in the Exercises list (GymBuddy, native Android, Soft Dark redesign)
Context: Exercises tab = big title, pinned search, muscle-group filter chips (All, Chest, Back, Legs, Shoulders, Arms, Core), then rows: [44dp leading visual] Name (16sp) / "Chest · Barbell" (14sp muted). ~60 built-in exercises + user's own. Same row appears in "add exercise to workout/template" pickers and search results. Exercise detail page exists (how-to, photos, history/progress chart).
Tried and REJECTED by owner: blocky mini muscle figures ("look like boobs"), crops of two open-source anatomy maps (polygon one "skeleton/robot", smooth one still disliked). Owner wants the visual to carry meaning (not 10 identical icons) but doesn't like body-part icons. Open to "nothing".
Q1. Leading visual for list rows:
 (a) none: text only, maybe denser rows (category standard in many trackers)
 (b) equipment glyph in our 2dp outline style (barbell, dumbbell, cable, machine, bodyweight, kettlebell, band) ~8 icons
 (c) movement-pattern glyph (push, pull, squat, hinge, lunge, carry, core, cardio) ~8 custom icons
 (d) exercise thumbnail: photo/illustration of the movement (e.g. public-domain free-exercise-db photos, or a commissioned illustration set) - consistent toned treatment
 (e) monogram tile (first letter) with neutral surface
 (f) replace the visual with useful data on the right: "Last: 80 kg × 5" or "3 days ago" (needs history; empty for new users)
 (g) other (propose)
Q2. Exercise detail page hero: (a) none, title + text (b) movement photo/illustration (c) muscle diagram full body (d) last/best numbers as hero (e) other.
Constraints: Operate mode, Material 3, WCAG AA (no colour-only meaning), 48dp rows, font 200%, TalkBack. Lime = pressable only, cobalt = selected only. Licences must allow commercial use; assets must be consistent in style. Design only (Plan 3); built in Plan 4. Keep it fast to scan with sweaty hands mid-workout (picker use).
Read: PRODUCT.md, docs/plans/figma-foundation/design-brief.md. Repo /Users/karlo/Dev/GymBuddy. Do not edit files.
```

## Answers (condensed)
- **Design director (impeccable):** Q1 (d), Q2 (b). Movement shape is what people recognise fastest (mode-operate scanability); (b) repeats the subtitle, (c) is jargon. 60 photos already in reference data; tone them neutral grey, crop to end position; custom exercises get an equipment glyph; (f) as trailing text in the picker. Numbers as a plain row under the detail photo, not a hero metric (craft floor). Veto (e) letter tiles (slop).
- **Product owner:** Q1 (a), Q2 (b). Text-only works for custom exercises, no assets to maintain, denser rows. Photos belong on the detail page. (f) depends on History (future plan) and is empty for new users. Veto (c): a data-model change disguised as an icon. Idea: "Recent" group at top of the picker.
- **Android architect:** Q1 (b), Q2 (b). Exercise.equipmentNeeded already exists (14 values → ~8 glyphs), ~10 KB vectors, no decode per row. Veto (c) (new field, migration, openapi change) and (d) in lists: thumbnailUrl is a raw.githubusercontent.com link (free-exercise-db), breaks offline. Idea: bundle 60 toned WebP in assets (~1–2 MB) for the detail hero.
- **Accessibility:** Q1 (a)+(f), Q2 (b) with last/best strip under the photo. A 44dp tile steals ~60dp the name needs at 200%; glyph duplicates text; 44dp photo too small in dim light. Trailing "Last: 80 kg × 5 · 3 days ago" is text, scales, TalkBack reads it as one item. Veto (e). Idea: Recent / in-this-template section in the picker.
- **Beginner:** Q1 (d) + (f) later, Q2 (b). I don't know what "Romanian deadlift" looks like; a picture finds it; icons don't help. Veto (c) jargon. Idea: custom exercises borrow the closest built-in photo or let me snap my own.
- **Veteran:** Q1 (a)+(f), Q2 (d) with photo below. Never looked at thumbnails in Strong/Hevy; I search 3 letters; the one thing I want is my last set. Veto (e). Idea: "Recent" (last 8) at the top of the picker.

## Synthesis
| Member | Q1 | Q2 |
|---|---|---|
| impeccable | d photo | b photo |
| PO | a none | b photo |
| Architect | b equipment | b photo |
| Accessibility | a+f none + last numbers | b photo + numbers strip |
| Beginner | d photo (+f) | b photo |
| Veteran | a+f none + last numbers | d numbers (photo below) |

Q1 split: no visual (3, two with last numbers) vs movement photo (2) vs equipment glyph (1). Vetoes: letter tiles ×3, movement-pattern glyphs ×3. Nobody wants body-part icons.
Q2: near-unanimous movement photo on the detail page (toned, bundled offline), last/best numbers as a plain row under it.
Shared idea (3 independent): a "Recent" section at the top of the add-exercise picker.
Fact found: photos come from free-exercise-db via GitHub raw URLs today (not offline); bundle them as toned WebP.
Recommendation: list rows text-only, denser, with trailing "80 kg × 5 · 3d" once history exists (nothing when empty); toned movement photo as the detail hero; Recent section in the picker. Option to mock: small toned photo thumbnails (impeccable/beginner) for comparison.

Owner (2026-11-01): Q1 (a) text only for now (photos get imported anyway for the exercise page; list photos can come later). Q2 (b) photo on top of the exercise page, Last / Best under it. Recent group: questioned by owner (useless across split templates), see chat.
