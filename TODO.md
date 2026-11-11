# MyStueberlApp – TODO

## 🎨 Theming (dark mode)
- [x] Add missing color roles to `Theme.kt` (onSurfaceVariant, outline, surfaceContainer*, …) and create `StueberlDefaults.kt`
- [ ] Convert existing screens to `MaterialTheme.colorScheme` + `StueberlDefaults`, one screen at a time
  - Main issue: input fields show white text on `FloralWhite` in dark mode
  - Quick first candidate: cost calculator (`CostCalculatorDialog`, `MaterialPositionRow`, price chips)
  - Home tiles: `HomeTile` / `HomeTileRow` now exist (`view/home/HomeTile.kt`); move `StatisticsTile`, `ProductsTile`, `OrdersTile`, `GalleryTile` onto them and delete the private `TileRow`
  - Home screen top bar: switch to `StueberlDefaults.topAppBarColors()`
  - `StueberlConfirmationDialog`: switch to `StueberlDefaults.primaryButtonColors()` / `destructiveButtonColors()` (also used by the event detail screen)
  - `AttachProductDialog`: theme-based colors (reused by orders and events)
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

## 🚧 In progress: Events feature (part 2: IN_PREPARATION + product linking)
Rules: link/unlink only in IN_PREPARATION; one active event per product; exclusive with orders; products in an active event are NOT stand stock; sold/removed before the event stays linked (doesn't count toward totals or the tile counter)
- [x] 1. Data access: event products (join ordered by `addedAt`, then `ep.rowid`), attachable products, guarded link/unlink, cancel event (sets `cancelledOn`), order picker + `attachProductsToOrder` exclude event products, stand count excludes event products, next-event product count (AVAILABLE only), product IDs in active events; `EventStatus.ACTIVE_NAMES` / `CANCELLABLE_NAMES`; DB version 15; `EventFormViewModel.saveEvent()` uses `base.copy(...)`
- [x] 2. IN_PREPARATION detail screen: collapsible info card (collapsed by default in IN_PREPARATION), products grouped by batch, "No longer available" section, totals (sales large, purchase small), remove with confirmation (batch / single / no longer available), tap → Edit Product, "Cancel event" in overflow (PLANNED + IN_PREPARATION), "Cancelled on" in footer; new theme-based `EventProductItem` / `EventProductBatchItem`; content is a `LazyColumn`
- [ ] 3. "Existing": reuse `AttachProductDialog` (optional `emptyText`); shared selection helper for orders + events
  - Add the "New" / "Existing" buttons to the IN_PREPARATION detail screen ("New" wired in step 4)
- [ ] 4. "New": `AddProduct(orderId, eventId)`, batches allowed, product + link in one transaction
  - `EventRepository` (or the product side) gets `db` for the transaction (moved from step 1)
- [ ] 5. Product list: "Event" badge, "Go to event" in bottom sheet, new source filter chip "From event"
  - Event membership isn't on `ProductDetail`: use `getProductIdsInActiveEventsFlow()` → `Set<Int>` in `ProductsViewModel` → `isEventProduct: Boolean = false` on `ProductDisplayItem` / `ProductBatchItem`
  - New theme-based `EventBadge` (`secondary`) next to the existing `OrderBadge`
- [ ] 6. Home: Events tile counter for AVAILABLE products of the next event (query exists: `getAvailableProductCountFlow()`); products tile unchanged
  - "Available in sales area" already excludes event products (done in step 1)

## 🗓️ Events feature (part 3+, not started)
- [ ] Detail screens for IN_PROGRESS / DONE / CANCELLED
  - Decide per status whether edit and delete stay available (currently allowed in every status)
- [ ] "Start event" (IN_PREPARATION → IN_PROGRESS) with confirmation
- [ ] Going back a status (possible but rare: overflow menu + confirmation)
  - Leaving CANCELLED must clear `cancelledOn`
- [ ] No adding/removing products during IN_PROGRESS (already true: remove icons only show in IN_PREPARATION, and link/unlink are guarded in SQL)
- [ ] Point-of-sale screen for IN_PROGRESS (basket per customer, total, lock basket, cash given → change)
- [ ] Mark sales during the event (sale date defaults to the event date)
- [ ] "Close event" flow (mark sold items, the rest go back to the stand)
- [ ] Statistics per event (revenue, stand fee, profit); decide how event sales show up in the existing sales area / orders split
- [ ] Duplicate event for recurring markets

## 🟡 Cleanup
- [ ] Status icon/tint duplication (3 files)
  - `EventStatusBadge` and `OrderStatusBadge` share the same layout; consider one shared theme-based `StatusBadge`
- [x] Extract date picker dialog (done as Events step 3)
- [ ] Version bumps: `coreKtx`, `activityCompose`, `junit`, `espresso`
- [ ] Empty-state flash (e.g. home counts start at `0` instead of `–` while loading)
- [ ] Validation: `salesPrice ≥ purchasePrice`
- [ ] Tests (good first candidates: `buildMaterialSuggestions()`, `EventRepository.getLocationSuggestions()` grouping, `splitEvents()`, `todayFlow()` with a fixed `Clock`, `EventFormViewModel` end-date shift and stand fee parsing, `eventTiming()`, `toEventProducts()` split + totals; Room instrumented tests for `linkProducts` / `unlinkProducts` guards)
- [ ] Undo on delete
- [ ] `OrderProductItem`: prices use `toPlainString()` + symbol instead of `Money.format()` (not localized: "12.50 €" instead of "12,50 €")
- [ ] `OrderDao`: `getAttachableProductsFlow()` and `attachProductToOrder()` look unused (the repository uses the `ProductDao` versions); check with Find Usages and remove
- [ ] Hard-coded `'AVAILABLE'` / `'CANCELLED'` literals in SQL: consider passing them as parameters like the event statuses
- [ ] `KEY_PRODUCT_UPDATED` is set on the event detail screen after Edit Product but never read (harmless; list updates via the database). Optionally show a snackbar there
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