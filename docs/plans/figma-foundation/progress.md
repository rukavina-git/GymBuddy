# Progress

- [x] 01 Figma file + MCP connected (roadmap 3.1, done before this plan)
- [ ] 02 Design tokens as Figma variables + design/tokens.json (steps/02-design-tokens.md)
- [ ] 03 Component library (steps/03-components.md)
- [ ] 04 Screen inventory and frames (steps/04-screens.md)
- [ ] 05 Compose theme follows the tokens (steps/05-code-follows-figma.md)

## Deviations
None

## Gotchas
- From phase5-data-safety: debug builds use the hosted backend; use
  `-Pgymbuddy.apiBaseUrl=http://10.0.2.2:8080/` for a local one.
- From phase5-data-safety: Plan 4 has two new steps, 4.2a (auth gate,
  needs a Login "session expired" frame from step 04) and 4.2b
  (durable push).
