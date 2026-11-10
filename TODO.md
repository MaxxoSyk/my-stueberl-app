# MyStueberlApp – TODO

## 🎨 Theming (dark mode)
- [x] Add missing color roles to `Theme.kt` (onSurfaceVariant, outline, surfaceContainer*, …) and create `StueberlDefaults.kt`
- [ ] Convert existing screens to `MaterialTheme.colorScheme` + `StueberlDefaults`, one screen at a time
  - Main issue: input fields show white text on `FloralWhite` in dark mode
  - Quick first candidate: cost calculator (`CostCalculatorDialog`, `MaterialPositionRow`, price chips)
  - Home tiles: `HomeTile` / `HomeTileRow` now exist (`view/home/HomeTile.kt`); move `StatisticsTile`, `ProductsTile`, `OrdersTile`, `GalleryTile` onto them and delete the private `TileRow`
  - Home screen top bar: switch to `StueberlDefaults.topAppBarColors()`
  - `StueberlConfirmationDialog`: switch to `StueberlDefaults.primaryButtonColors()` / `destructiveButtonColors()` (also used by the event detail screen)
  - `ProductPriceField`: theme-based version (the event form's stand fee field already shows the pattern)
- Rule for all new screens and components: only `MaterialTheme.colorScheme` and `StueberlDefaults`, no direct imports from `Color.kt`

## ✅ Done: Events feature (part 1)
- [x] 1. Data model (`EventStatus`, `EventDetail`, `EventProductDetail`, DB version 14)
- [x] 2. `EventDao`, `EventRepository`, app wiring
- [x] 3. Shared date picker (`StueberlDatePickerDialog`)
- [x] 4. Events list screen + navigation (upcoming / past by date, product count on cards, simple home tile)
- [x] 5. Create / edit event form (location suggestions, end date with `minDate`, start date shifts end date, stand fee `null` vs `0`; unsaved changes discarded without asking, like orders)
- [x] 6. Event detail screen for PLANNED: info, "Start preparation" with confirmation, edit, delete; own "Event saved" snackbar after editing
- [x] 7. Home tile "Events": next event with timing (today / tomorrow / in X days / until), "–" when none, updates at midnight; scrollable home screen

## 🗓️ Events feature (part 2+, not started)
- [ ] Detail screens for IN_PREPARATION / IN_PROGRESS / DONE / CANCELLED
  - Decide per status whether edit and delete stay available (currently allowed in every status)
- [ ] Link products (only in IN_PREPARATION; new via Add Product incl. batches, existing via picker, exclusive with orders, one active event per product)
  - Active event per product needs its own query; `@Relation` with a junction can't filter by status
- [ ] Event badge in product list (after linking)
- [ ] Going back a status (possible but rare: overflow menu + confirmation)
- [ ] No adding/removing products during IN_PROGRESS
- [ ] Point-of-sale screen for IN_PROGRESS (basket per customer, total, lock basket, cash given → change)
- [ ] Mark sales during the event (sale date defaults to the event date)
- [ ] "Close event" flow (mark sold items, the rest go back to the stand)
- [ ] Statistics per event (revenue, stand fee, profit)
- [ ] Duplicate event for recurring markets

## 🟡 Cleanup
- [ ] Status icon/tint duplication (3 files)
  - `EventStatusBadge` and `OrderStatusBadge` share the same layout; consider one shared theme-based `StatusBadge`
- [x] Extract date picker dialog (done as Events step 3)
- [ ] Version bumps: `coreKtx`, `activityCompose`, `junit`, `espresso`
- [ ] Empty-state flash (e.g. home counts start at `0` instead of `–` while loading)
- [ ] Validation: `salesPrice ≥ purchasePrice`
- [ ] Tests (good first candidates: `buildMaterialSuggestions()`, `EventRepository.getLocationSuggestions()` grouping, `splitEvents()`, `todayFlow()` with a fixed `Clock`, `EventFormViewModel` end-date shift and stand fee parsing, `eventTiming()`)
- [ ] Undo on delete
- [x] German strings: a few still said "Produkt" instead of "Werkstück" (`products_screen_search_no_results`, `attach_product_confirm`, `gallery_screen_empty`)

## 🟠 Deferred features
- [ ] Statistics breakdowns
- [ ] Bulk status actions
- [ ] Batches in orders
- [ ] Sales-price calculator
- [ ] Empty orders surviving process death

## 📦 Pre-release
- [ ] R8
- [ ] Real migrations (replace the debug-only destructive migration)
  - [x] `exportSchema = true` with `app/schemas/` configured; JSON files are generated and checked in
- [ ] Device pass on hardware
- [ ] Review backup rules
- [ ] Play Store icon