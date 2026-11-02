# GymBuddy colours

Every colour used in the locked designs, in one place. Pulled from
Figma page "03 Locked designs" on 2026-11-02 (all fills, strokes and
text). Dark is the only mode in v1. Values live in Figma variables
(collection "Colour"); their committed copy is `design/tokens.json`.
Change a value in Figma and re-export. This file explains meaning and
use. Type, space, radius, stroke, size and motion tokens are in the
same JSON (step 03).

Contrast is WCAG 2 on the ground (#121418) unless noted. Text needs
4.5:1, icons, outlines and large text need 3:1.

## Brand
| Name | Hex | Meaning | Origin | On ground |
|---|---|---|---|---|
| lime | #D4FF24 | "You can press this." Start, log, done, save. Nothing else is lime. | Owner's acid lime, unchanged. | 15.9 |
| cobalt | #4D86F0 | "You are here." Current set ring, active tab, selected rows, switch on. | Owner's #1558C9, lightened so it reads on dark grey (#1558C9 is 2.9:1 on the ground). | 5.3 (4.7 on surface) |
| ground | #121418 | The app background. | Soft Dark direction (2026-10-31). Replaces the owner's midnight #030407. | — |

## Surfaces (darkest to lightest)
| Name | Hex | Use |
|---|---|---|
| ground | #121418 | Screen background. |
| surface | #1C1F26 | Cards, rows, bottom bar. |
| surface-raised | #262A33 | Keypad keys, input fields, chips. Sheets keep `surface` (step 05 D3). Also at 60% in places. |
| surface-high | #323743 | Calendar days, progress tracks, fills inside workout cards. |

## Text and lines
| Name | Hex | Use | Contrast (ground / surface / raised) |
|---|---|---|---|
| text | #F3F4F6 | Main text and icons. | 16.8 / 15.0 / 13.1 |
| text-soft | #C9CDD4 | Long reading text: How-to steps, option labels. | 11.6 / 10.3 / 9.0 |
| muted | #9AA1AD | Secondary text: subtitles, rest, dates. | 7.1 / 6.3 / 5.5 |
| outline | #717886 | Control outlines, empty rings. | 4.2 / 3.7 / 3.2 |
| disabled | #5E6571 | Disabled controls only (WCAG exempt). | 3.1 / 2.8 / 2.4 |
| on-lime | #121418 | Text and icons on lime. | 15.9 on lime |

## Tints
| Name | Value | Use | Contrast |
|---|---|---|---|
| lime-pressed | #B8DE1F | Lime button while pressed. | on-lime on it 11.9 |
| lime-tint | #2E3A1A | Circle behind a done set's check. | lime check on it 10.4 |
| cobalt-tint | #4D86F0 at 25% | Active tab pill, selected row, picked exercise. | text on it 10.5; muted only 4.4 on cards, so use text |

## Danger
| Name | Hex | Use | Contrast |
|---|---|---|---|
| danger | #FF7A7A | Delete, Discard, Sign out anyway, Erase, Delete account, field and keypad errors. | 7.3 on ground |
| danger-container | #2B1D20 | "Couldn't save" card. | danger on it 6.4 |
| danger-swipe | #5C1F1F | Background behind a set swiped to delete. | text on it 11.4 |

There is no success or warning colour. Done is lime plus a check, and
"waiting" or "stuck" states use muted text plus an icon (06f decision:
offline is normal, not an error).

## Overlays
| Name | Value | Use |
|---|---|---|
| scrim | #000000 at 60% | Behind sheets and dialogs. |
| shadow | #000000 at 50% | Drop shadow on sheets. |
| ripple | #FFFFFF | Press ripple (in the motion frames). |

## Not ours (do not turn into tokens)
- Google sign-in button: #131314, #8E918F, #E3E3E3 and the G logo
  colours (#4285F4, #34A853, #FBBC05, #EA4335). Google's brand rules.
- Android system UI drawn for context: account picker (#E9ECF1,
  #1B1C1F), lock screen (#0A0B0E).
- Figma board chrome: section fills #444444 and #0B0C0F, label text
  #F2F5F5, section strokes #FFFFFF at 10%, avatar placeholder #D9D9D9.
- One-off fades in motion frames (#F3F4F6 at 50/60/70%, #121418 at
  30%): animation steps, not colours.

## Decided 2026-11-02 (owner, impeccable agrees)
Applied in step 03 (2026-11-02) when the frames were bound to variables.
1. **One red.** #FF7A7A only; #FF8A80 (keypad error) becomes danger.
2. **One cobalt tint.** 25% only; the 16% and 30% uses become 25%.
3. **One scrim.** 60% only; the 55% and 35% uses become 60%.
4. **#3A3F4B is dropped.** The "New template" frame uses surface-high
   #323743.
5. **No outline grey as text.** #717886 failed AA as text (4.2:1 on
   the ground, 3.7:1 on cards; 63 uses). Those texts become muted
   #9AA1AD. Outline stays for lines and rings.
6. **Disabled grey only when disabled.** Of 27 text uses, these stay:
   keypad keys that can't be pressed ("." and Save), "Back up and sign
   out" while offline, lifts without enough logs in Change lift ("Lat
   pulldown · 2 of 3"). These become muted: template row numbers,
   "9 of 12", the "e.g. Push day" placeholder (placeholders need
   4.5:1). The rest are Android's account picker or board notes.

## Retired
- #030407 midnight, #1558C9 cobalt: owner's first proposal, never used
  in the redesign.
- #5B8DEB cobalt text tint: from the first proposal; not used.
