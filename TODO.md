# MyStueberlApp – TODO

## 🎨 Theming (dark mode)
- [x] Add missing color roles to `Theme.kt` (onSurfaceVariant, outline, surfaceContainer*, …) and create `StueberlDefaults.kt`
- [ ] Convert existing screens to `MaterialTheme.colorScheme` + `StueberlDefaults`, one screen at a time
  - Main issue: input fields show white text on `FloralWhite` in dark mode
  - Quick first candidate: cost calculator (`CostCalculatorDialog`, `MaterialPositionRow`, price chips)
  - Home tiles: switch to the shared theme-based `HomeTile` / `HomeTileRow` once they exist (Events step 7)
  - `StueberlConfirmationDialog`: switch to `StueberlDefaults.primaryButtonColors()` / `destructiveButtonColors()`
- Rule for all new screens and components: only `MaterialTheme.colorScheme` and `StueberlDefaults`, no direct imports from `Color.kt`

## 🚧 In progress: Events feature (part 1)
- [x] 1. Data model (`EventStatus`, `EventDetail`, `EventProductDetail`, DB version 14)
- [x] 2. `EventDao`, `EventRepository`, app wiring
- [x] 3. Shared date picker (`StueberlDatePickerDialog`)
- [x] 4. Events list screen + navigation (upcoming / past by date, product count on cards, simple home tile)
- [ ] 5. Create / edit event screen (location autocomplete; end date with `minDate = eventDate`, clear icon for one-day events)
  - After saving a new event: back to the list, set `KEY_EVENT_SAVED` → snackbar
  - Wire the FAB in `NavGraph` (`Screen.EventForm()`)
- [ ] 6. Event detail screen for PLANNED: info, "Start preparation", edit, delete
  - Wire the cards in `NavGraph` (`Screen.EventDetail(eventId)`)
  - Decide: does the detail screen show its own "Event saved" snackbar after editing?
- [ ] 7. Home tile "Events": add next-event row (nearest event not over yet, skip CANCELLED, "today" / "in 5 days") + scrollable home screen
  - Reuse `todayFlow()` so the tile updates at midnight

## 🟡 Cleanup
- [ ] Status icon/tint duplication (3 files)
  - `EventStatusBadge` and `OrderStatusBadge` share the same layout; consider one shared theme-based `StatusBadge`
- [x] Extract date picker dialog (done as Events step 3)
- [ ] Version bumps: `coreKtx`, `activityCompose`, `junit`, `espresso`
- [ ] Empty-state flash (e.g. home counts start at `0` instead of `–` while loading)
- [ ] Validation: `salesPrice ≥ purchasePrice`
- [ ] Tests (good first candidates: `buildMaterialSuggestions()`, `EventRepository.getLocationSuggestions()` grouping, `splitEvents()`, `todayFlow()` with a fixed `Clock`)
- [ ] Undo on delete
- [x] German strings: a few still said "Produkt" instead of "Werkstück" (`products_screen_search_no_results`, `attach_product_confirm`, `gallery_screen_empty`)

## 🟠 Deferred features
- [ ] Statistics breakdowns
- [ ] Bulk status actions
- [ ] Batches in orders
- [ ] Sales-price calculator
- [ ] Empty orders surviving process death
- [ ] Events: detail screens for IN_PREPARATION / IN_PROGRESS / DONE / CANCELLED
- [ ] Events: link products (only in IN_PREPARATION; new via Add Product incl. batches, existing via picker, exclusive with orders, one active event per product)
  - Active event per product needs its own query; `@Relation` with a junction can't filter by status
- [ ] Events: event badge in product list (after linking)
- [ ] Events: going back a status (possible but rare: overflow menu + confirmation)
- [ ] Events: no adding/removing products during IN_PROGRESS
- [ ] Events: point-of-sale screen for IN_PROGRESS (basket per customer, total, lock basket, cash given → change)
- [ ] Events: mark sales during the event (sale date defaults to the event date)
- [ ] Events: "Close event" flow (mark sold items, the rest go back to the stand)
- [ ] Events: statistics per event (revenue, stand fee, profit)
- [ ] Events: duplicate event for recurring markets

## 📦 Pre-release
- [ ] R8
- [ ] Real migrations (replace the debug-only destructive migration)
  - [x] `exportSchema = true` with `app/schemas/` configured; JSON files are generated and checked in
- [ ] Device pass on hardware
- [ ] Review backup rules
- [ ] Play Store icon