# Barname Studio v2 | staged UI/UX iteration

Stage 1 implementation of the 47-issue Linear roadmap. Existing 7x7 planner, calendar, archive, PNG/PDF and JSON export preserved. Features: Midnight Blue UI, responsive day/week views, selected-day strip, mobile settings drawer, Undo/Redo, copy a day, clone next week, focus display, offline density heatmap, template chooser, library filter/sort and pins, IndexedDB draft mirror (localStorage migration). IndexedDB migration is guarded to prevent silent archive overwrites. 

Checks: Chromium simulated mobile 390x844 + desktop 1365x900, editor 49 cells, day view 7 cards, autosave reloading, undo/redo, library archive after PNG export, search, templates and theme modal. HTTPS/PWA offline/device-install testing and production smoke tests remain release gates. Updates shall preserve the existing origin and not delete stored data.
