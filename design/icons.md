# Icons and brand

Exported from Figma "01 System" → Icons (step 04, 2026-11-02). Figma is
the source; these SVGs are its committed copy. Re-export, don't hand-edit.
Colours in the SVGs are the token defaults; code tints them.

## Sources and licences
| Set | Files | Source | Licence |
|---|---|---|---|
| Brand | `design/brand/*` | Own drawing (ECG line, Figtree wordmark outlined) | Ours. Figtree: OFL 1.1 |
| Gym glyphs | `design/icons/glyphs/*` | Own drawing | Ours |
| Tab icons | `design/icons/tabs/*` | Own drawing (from locked App frame) | Ours |
| UI icons | `design/icons/ui/*` | Material Symbols Rounded, weight 400 (`@material-symbols/svg-400` 0.27.0) | Apache 2.0 (list in the app's open-source licences screen) |

All commercial-friendly (CLAUDE.md: nothing may block monetization).

## Rules
- 24dp grid. Custom icons: 2dp round stroke (`stroke/icon`). One
  version per icon; filled only for the active tab.
- Colour from tokens: `text/text` by default, `text/muted` for
  secondary, cobalt only for "you are here". Never lime (Do/Don't).
- Tap area 48dp even when the icon is 20–24dp.

## Brand (`design/brand/`)
| File | Use |
|---|---|
| mark.svg | Logo mark: own ECG line, stroke 5 on a 48 grid. Scale, never redraw. |
| app-icon.svg | Launcher preview (lime tile, mark in ground colour). |
| app-icon-foreground.svg | Adaptive icon foreground, 108dp, mark inside the 66dp safe zone. |
| app-icon-background.svg | Adaptive icon background, lime. |
| app-icon-monochrome.svg | Android 13+ themed icon. |
| wordmark.svg | "gymbuddy." Figtree Bold outlined, lime dot. 24 on Home, 26 sign-in, 34 Welcome. |
| lockup.svg | Mark tile + wordmark. |

## Gym glyphs (`design/icons/glyphs/`)
| Name | Use |
|---|---|
| dumbbell | Exercise photo fallback, empty states |
| barbell, kettlebell, bench, cable, pull-up, jump-rope | Equipment / exercise type |
| plate | Plate maths, kg/lb |
| rest | Rest timer |
| pr | Personal record flag |
| scale | Body weight |
| cardio | Cardio, timed sets |

## Tab icons (`design/icons/tabs/`)
home, workouts, exercises, progress, you; each `-outline` (inactive) and
`-filled` (active, with cobalt pill + bold label). Exercises active uses
a 2.75 stroke (a dumbbell has nothing to fill).

## UI icons (`design/icons/ui/`)
| Name | Material Symbol | Use |
|---|---|---|
| back | arrow_back | Top bar back |
| add / remove | add / remove | Steppers, new items |
| close | close | Sheets, dialogs, clear |
| chevron-right / -down / -up | chevron_right / keyboard_arrow_down / _up | Row affordance, expand |
| check | check | Done set, selected |
| more | more_vert | Overflow menu |
| drag / drag-dots | drag_handle / drag_indicator | Reorder |
| play | play_arrow | Start template |
| search | search | Search fields |
| list | list | Lists |
| info / alert / warning | info / error / warning | Help, field errors, warnings |
| edit | edit | Edit note, name |
| hide | visibility_off | Hidden exercises |
| delete | delete | Delete (red only when destructive) |
| undo | undo | Undo snackbar |
| backspace | backspace | Keypad |
| note | sticky_note_2 | Exercise/set notes |
| clipboard / copy | content_paste / content_copy | Copy workout |
| phone | smartphone | "On this phone" |
| cloud / cloud-off | cloud / cloud_off | "In your account", needs internet |
| refresh / spinner | refresh / progress_activity | Retry, backing up |
| timer | timer | Rest, timed sets |
| trending-up | trending_up | Progress |
| swap | swap_vert | Swap exercise |
| bell | notifications | Lock-screen notification prompt |
| logout | logout | Sign out |
