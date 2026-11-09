# MyStueberlApp – TODO

## 🎨 Theming (dark mode)
- [x] Add missing color roles to `Theme.kt` (onSurfaceVariant, outline, surfaceContainer*, …) and create `StueberlDefaults.kt`
- [ ] Convert existing screens to `MaterialTheme.colorScheme` + `StueberlDefaults`, one screen at a time
  - Main issue: input fields show white text on `FloralWhite` in dark mode
  - Quick first candidate: cost calculator (`CostCalculatorDialog`, `MaterialPositionRow`, price chips)
  - Home tiles: switch to the shared theme-based `HomeTile` / `HomeTileRow` once they exist (Events step 7)
- Rule for all new screens and components: only `MaterialTheme.colorScheme` and `StueberlDefaults`, no direct imports from `Color.kt`

## 🚧 In progress: Events feature (part 1)
- [x] 1. Data model (`EventStatus`, `EventDetail`, `EventProductDetail`, DB version 14)
- [x] 2. `EventDao`, `EventRepository`, app wiring
- [ ] 3. Shared date picker (= cleanup item "Extract date picker dialog")
- [ ] 4. Events list screen + navigation (upcoming / past by date, product count on cards)
- [ ] 5. Create / edit event screen (location autocomplete)
- [ ] 6. Event detail screen for PLANNED: info, "Start preparation", edit, delete
- [ ] 7. Home tile "Events" (nearest event not over yet, skip CANCELLED) + scrollable home screen
  - Handle "today" changing at midnight while the app stays open

## 🟡 Cleanup
- [ ] Status icon/tint duplication (3 files)
- [ ] Extract date picker dialog (planned as Events step 3)
- [ ] Version bumps: `coreKtx`, `activityCompose`, `junit`, `espresso`
- [ ] Empty-state flash (e.g. home counts start at `0` instead of `–` while loading)
- [ ] Validation: `salesPrice ≥ purchasePrice`
- [ ] Tests (good first candidates: `buildMaterialSuggestions()`, `EventRepository.getLocationSuggestions()` grouping)
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