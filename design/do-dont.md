# Do / Don't

Rules for GymBuddy's design (Soft Dark, Android, dark only in v1).
Drafted by impeccable from what the rounds decided; impeccable has
the main say on design, the owner the final call. Every critique,
the components step and Plan 4 code check against this list.
Living file: when a round decides something new, add a line here.

Source tags: R = round in design-brief.md, owner = owner's call,
floor = impeccable craft floor, colors = design/colors.md.

## Colour
- Do use lime only on things you press that start, log, save or finish,
  and on done sets. / Don't use lime for icons, flags, headings or
  decoration. Exception: the app icon and the wordmark dot. (palette,
  R4, A2, W2)
- Do use cobalt for "you are here": current set, active tab, selected
  row, switch on. / Don't use cobalt for buttons. (palette, R15)
- Do use several lime buttons on a screen when each starts something.
  / Don't add any accent besides lime and cobalt. (owner, palette)
- Do use red only for destructive actions (Delete, Discard, Erase),
  field and keypad errors, and the "Couldn't save" card. / Don't use
  red for offline, waiting or stuck backup. (06f, R5, R16)
- Do take every colour from design/colors.md. / Don't add a hex that
  isn't there; add it to the file first. (colors)
- Do use muted #9AA1AD for secondary text. / Don't use the outline
  grey for text, or the disabled grey for anything that isn't
  disabled. (palette, colors 5-6)
- Do use the main text colour on the cobalt tint. / Don't put muted
  text on it (4.4:1 on cards). (colors)

## Type
- Do use Figtree everywhere. / Don't mix in a second family or a
  system display face. (direction, floor)
- Do make sets, reps and kg the biggest thing on a workout row. /
  Don't make rest as big as them; rest is muted. (R12, owner)
- Do follow the phone's text size; at 175%+ drop extras, keep the
  set. / Don't cut the set or the number to fit. (R7, App frame)

## Layout
- Do use M3 structure: large title that collapses, bottom bar, sheets,
  dialogs, system Back. / Don't invent navigation. (App frame)
- Do give a screen one clear main action. / Don't add a FAB next to
  a lime button. (App frame, R12)
- Do use cards for a group that belongs together. / Don't nest cards
  or build a page from same-size icon cards. (floor)
- Don't put a small label (eyebrow) above a heading. (floor, ban)
- Don't use a coloured left border on cards, rows or alerts. (floor)

## Workout screen
- Do show only what's crucial while logging; extras are one tap away.
  / Don't add anything new to the workout screen unless a set could be
  lost. (owner, 06f)
- Do put − / + on the card and a keypad on tap. / Don't use wheels or
  hidden gestures as the only way. (R2)
- Do show rest as one quiet line under the last done set. / Don't
  take over the screen or alarm when rest ends; the soft buzz is
  opt-in and off by default. (R3, R4, R15)
- Do let swipe and delete act at once with Undo. / Don't confirm
  undoable actions. (R2, H1)
- Do confirm what loses data or ends the workout (Discard, Finish,
  "Discard changes?", Delete account). / Don't confirm what Undo can
  bring back. (R2, R4, R12)

## Copy
- Do say what happened and what to do next ("Needs internet. Safe on
  this phone."). / Don't blame the user. (06f)
- Do use plain, short words ("On this phone", "In your account"). /
  Don't use jargon like sync, cache or server. (R15)
- Do show the user's own numbers. / Don't add fake motivation,
  streaks, cheers or made-up estimates. (R4, R12, PRODUCT)
- Do name the action on the button (Save, Finish, Delete). / Don't
  use OK or Yes. (floor)

## Motion
- Do keep one signature move: "the set lands" on Done. / Don't add
  bounce, confetti or count-up numbers. (R5)
- Do use M3 transitions (fade-through tabs, shared axis, sheets). /
  Don't animate when rest passes. (R5, App frame)
- Do turn movement into crossfades with Android "Remove animations";
  haptics stay. / Don't block input while animating. (R5)

## States
- Do give every list an empty state: glyph, one line, at most one
  action. / Don't show an empty box or a broken chart. (06f, R7)
- Do treat offline as normal: no global indicator, "Needs internet"
  only on network actions. / Don't show offline banners. (06f)
- Do show nothing while local data loads. / Don't use skeletons on
  local lists. (06f)
- Do hide a chart until there is enough data (3+ logs over 2+
  weeks). / Don't show a placeholder chart. (R7, 06f)

## Access
- Do meet WCAG AA: 4.5:1 text, 3:1 icons and outlines. (PRODUCT)
- Do make every tap area 48dp, even if the drawn chip is 36dp. (palette)
- Do pair colour with an icon or text for state (check, ring, empty
  ring). / Don't show state by colour alone. (palette)
- Do give charts one TalkBack stop with a summary sentence. (R7)
- Do draw icons in one outline set (2dp round stroke), with a filled
  version only for the active tab. / Don't use
  emoji or Unicode as icons. (B1, App frame, floor) Exception: profile animals
  (Fluent Emoji, R15) are pictures, not icons.
