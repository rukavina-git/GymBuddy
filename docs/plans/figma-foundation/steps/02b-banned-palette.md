# 02b Colours file + Do/Don't list

## Problem
The colours live in two places that disagree. PRODUCT.md still shows
the owner's first proposal (midnight #030407, cobalt #1558C9).
design-brief.md names only 8 hexes. The rest, such as error red, were
added straight in Figma and are written down nowhere. There is also
no written list of design rules, so components (05) and Plan 4 code
can drift from what the rounds decided.

## Change
1. **`design/colors.md`, one file with every colour.** Claude reads
   all fills and strokes on Figma "03 Locked designs" (Figma Console
   MCP) and lists each unique colour once: name, hex, role ("lime =
   you can press this"), where it is used, contrast on the ground.
   Near-duplicates (e.g. #4D86F0 vs #5B8DEB) are flagged for the owner
   to merge, not merged silently. The file notes where each brand
   colour came from: lime is the owner's, cobalt was lightened from
   #1558C9 for contrast, ground #121418 comes from Soft Dark.
2. **`design/do-dont.md`, living Do/Don't list.** impeccable drafts
   it (it has the main say) from the round-log decisions in
   design-brief.md, the owner's kills, and its craft floor. Each line
   says "Do X / Don't Y" and where it came from. Claude checks that no
   line contradicts a locked frame. It gets updated whenever a round
   decides something new.
3. **PRODUCT.md.** Brand Commitments lists the 3 brand colours, Figtree
   and dark-only v1, and points to `design/colors.md` for the rest.
   Fix the "theme modes are undecided" line. Nothing else changes.

## Tests
None (no code). Claude checks that every hex in PRODUCT.md and
design-brief.md is in colors.md, and every pair it lists for text
meets WCAG AA.

## Out of scope
Figma edits, Figma variables and tokens.json (03 builds them from
colors.md), type and spacing tokens, new design options.

## Done when
- `design/colors.md` covers every colour on "03 Locked designs".
- `design/do-dont.md` exists.
- PRODUCT.md matches colors.md.
- Owner has okayed both files and any colour merges.
