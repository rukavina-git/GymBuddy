# Components

Built in Figma "01 System" → Components (step 05, 2026-11-02). Figma is
the source; this file is the index Plan 7 builds from. Every component
uses only tokens (`design/tokens.json`) and library icons
(`design/icons.md`). Names match the planned Compose names.

## Rules
- Heights are minimums: rows and buttons grow with system font scale.
- Touch target 48dp even when the drawn part is smaller (chip 36,
  Button 40, switch 32).
- Lime = you can press this. Cobalt = you are here / selected / on.
- Focused = 2dp cobalt ring outside. Disabled = `line/disabled` (WCAG exempt).
- States in Figma are static; motion is step 07.

## Library
| Component | Base (Compose) | Variants / props | Used in |
|---|---|---|---|
| Button/Primary | M3 Button, CircleShape | Size 72 / 56 / 40 · State · Label · icon | Done (72, workout only), Start, Create, Save (40, top bar) |
| Button/Secondary | FilledTonalButton | Size 56 / 40 · State · Label · icon | All sessions, Swap, workout progress pill |
| Button/Outline | OutlinedButton (1.5) | Size 56 / 40 · State · Label · icon | I have an account, How to, Cancel, Keep going |
| Button/Text | TextButton | State · Label | Dialog actions |
| Button/Danger | TextButton, danger colour | State · Label | Discard, Delete (never default, never lime) |
| IconButton | IconButton / FilledIconButton | Style Lime/Raised/High/Plain · State · Icon | Play, ⋯ |
| Chip | FilterChip | Default / Selected (cobalt tint + line + check) / Disabled · Label · chevron | Filters, tracking type, ranges |
| Switch | Switch | Off / On (cobalt) / Disabled | Settings, Show on Progress |
| SegmentedButton | SingleChoiceSegmentedButtonRow | Selected first/second · 48 high | List / Calendar |
| TextField | TextField, custom colours | Empty / Filled / Focused / Error / Disabled · Label · Helper | Sign-in, create exercise |
| SearchField | SearchBar (docked) | Empty / Filled (clear ×) | Exercises, sessions, picker |
| SetRow | custom | Done / Upcoming / Swipe delete · Set · Value | Workout set list |
| RestLine | custom | Value | Between sets (quieter than sets/reps/kg) |
| ValuePanel | custom | Default / Editing (lime line) · Number · Unit | Weight / reps on the set card |
| StepperKey | custom | Minus / Plus · State · 64×56 | Set card, Log weight |
| SetCard | custom | Title, two ValuePanels, cobalt ring | Current set |
| KeypadKey | custom | Digit / Backspace · State · 56 high | Keypad |
| Keypad | custom sheet | Label · Value · Unit · Cancel + Next (56) | Workout, template editor, Log weight |
| LiveBar | custom | Title · Subtitle · lime progress line | Running workout, every tab |
| NavBar | NavigationBar | Selected = Home / Workouts / Exercises / Progress / You | All tabs |
| TopBar | TopAppBar / LargeTopAppBar | Back / Large title / Workout | Every screen |
| SessionCard | custom Card | Title · Subtitle · Date · PR | All sessions, Progress, history |
| TemplateRow | custom Card | Title · Subtitle · trailing icon (play) | Home |
| ListRow | ListItem in Card | Leading none / glyph · 68 | Exercises, settings |
| InfoRow | ListItem in Card | Label · Value · chevron · 52 | Up next, Edit note |
| StatTile | custom | Value · Label · 72 | Summary, session detail, run |
| WeekDay / WeekStrip | custom | Rest / Done / Today / Today done | Home week card |
| Banner | custom Card | Danger / Info · Title · Body · Action | Couldn't save, Needs internet |
| EmptyState | custom | Glyph · Title · Body · optional action | Empty lists |
| Snackbar | Snackbar | Message · Action (lime) · 56 | Undo after delete |
| Dialog | AlertDialog | Title · Body · actions right | Discard, Delete, Sign out |
| BottomSheet | ModalBottomSheet | Title · Subtitle · content · top corners 28 | Change lift, Start workout |
| Menu / MenuItem | DropdownMenu(Item) | Default / Danger · Icon · Label | ⋯ menus |

## Merges applied to the locked frames (owner + impeccable, 2026-11-02)
D1 switch on = cobalt · D2 one selected-chip style · D3 sheets: top
corners only · D4 dialog = surface-raised, 356 wide · D5 lime 72/56/40
· D6 outline 56/40 · D7 stat tile 72 · D8 row cards radius 16 (set
rows 12) · D9 Log weight − / + = StepperKey · D10 one text field
(raised fill) · D11 keypad footer 56 · D12 snackbar 56 · D13 upcoming
set row 48 · D14 nav bar uses the tab icons.

## Locked frames
627 instances on "03 Locked designs". Containers whose content differs
per screen stay drawn but token-bound (sheets, dialogs, menus, set
card, keypad sheet, week strip, top bars, text fields, list rows);
Plan 7 builds them from the components above.
