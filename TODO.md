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
  - `FilterBottomSheet`: theme-based chip and button colors
  - `ProductPriceField`: theme-based version (the event form's stand fee field already shows the pattern)
  - `OrderBadge`: theme-based, like the new `EventBadge`
- Rule for all new screens and components: only `MaterialTheme.colorScheme` and `StueberlDefaults`, no direct imports from `Color.kt`

## ✅ Done: Events feature (part 1)

## ✅ Done: Events feature (part 2: IN_PREPARATION + product linking)

## ✅ Done: Events feature (part 3a: QR labels)

## 🔧 Events: improvements
- [x] IN_PREPARATION detail screen: "New" / "Existing" fixed in the bottom bar (`EventBottomBar`, shared with PLANNED), always visible with long product lists
- [x] Home screen: running event clearly visible — animated frame on the Events tile (`Modifier.runningBorder()`, 5 s per lap, highlight 20 %); `getNextEventFlow()` lists IN_PROGRESS events first, even past their last day; "Still running" / "Läuft noch" via `EventTiming.StillRunning`
- [ ] Optional polish for the running frame
  - Static frame (no moving highlight) when system animations are turned off
  - Constant speed along the frame (currently slightly faster on the short edges)

## 🚧 In progress: Events feature (part 3b: point of sale)
Decisions: one basket at a time, saved in the DB; no "lock": "Pay" opens a closable payment panel (nothing can be added while it's open); cash only, sales price is always the price; undo any sale of the event, only while IN_PROGRESS; one tap per scan; header figures: revenue, items sold, customers
- [x] 1. Data model: `EventSale` (snapshot of total + item count, `RESTRICT` on event), `EventBasketItem` (keyed by product), `ProductDetail.eventSaleId`, `Instant` converter; `EventSaleDao` + `EventSaleRepository` (`addToBasket` → `BasketAddResult`, `completeSale` compares with the shown basket → `BasketChanged`, `undoSale` only while IN_PROGRESS); stand actions clear `eventSaleId`; DB version 17
- [ ] 2. Till screen + basket: "Open till" on the IN_PROGRESS detail screen, add by hand (list + search), remove, total, header figures (calculated from `getSalesFlow()`)
  - Event detail: products sold at the till currently land in "No longer available"; show them as "Sold at this event" instead
- [ ] 3. Payment panel + complete: cash given, quick buttons (exact / 5 / 10 / 20 / 50 €), change; handle `BasketChanged`
- [ ] 4. Scanning: `GmsBarcodeScanning`, validation + error messages (foreign code, other event, plus the `BasketAddResult` cases), vibration, preload the module when the event starts (offline at the market)
- [ ] 5. Recent sales + undo, keep screen on, "Open till" shortcut on the Events tile

## 🗓️ Events feature (part 3+, not started)
- [x] "Start event" (IN_PREPARATION → IN_PROGRESS): second row in the bottom bar with confirmation (notes for a future start date and unprinted labels); blocked without available products (guarded in SQL via `EventDao.startEvent()`); afterwards products read-only, info card collapsed, in-progress hint, "Print labels" stays
- [ ] Sale channel follow-ups (`eventSaleId`, part 3b)
  - Bottom sheet: block "Reset" / "Removed" for products sold at the till (undo the sale instead), so sale totals stay correct
  - Deleting an event with sales currently fails with a constraint error (safety net); block it in the UI with a message
  - Statistics: stand sales = `orderId IS NULL AND eventSaleId IS NULL`; event revenue as its own channel
  - Product list: "From event" should also include products sold at an event (`eventSaleId IS NOT NULL`)
- [ ] Deleting events (decided): blocked for IN_PROGRESS / DONE events that have sales; PLANNED / IN_PREPARATION / CANCELLED and events without sales stay deletable
- [ ] Detail screens for DONE / CANCELLED
  - Decide per status whether edit stays available (currently allowed in every status)
- [ ] Going back a status (possible but rare: overflow menu + confirmation)
  - Leaving CANCELLED must clear `cancelledOn`
- [ ] "Close event" flow (IN_PROGRESS → DONE; remaining products go back to the stand; open basket must be empty)
- [ ] Statistics per event (revenue, stand fee, profit, customers, average basket); event sales as their own channel next to sales area / orders
- [ ] Duplicate event for recurring markets
- [ ] Optional: continuous scanning (CameraX + ML Kit)

## 🟡 Cleanup
- [ ] Status icon/tint duplication (3 files)
  - `EventStatusBadge` and `OrderStatusBadge` share the same layout; consider one shared theme-based `StatusBadge` (events already use `eventStatusIconAndTint()`)
- [x] Extract date picker dialog (done as Events step 3)
- [ ] Version bumps: `coreKtx`, `activityCompose`, `junit`, `espresso`
- [ ] Empty-state flash (e.g. home counts start at `0` instead of `–` while loading)
- [ ] Validation: `salesPrice ≥ purchasePrice`
- [ ] Tests: unit tests exist for `ProductLabelCode`, `LabelSheetLayout` and `eventTiming()`. Next candidates: `buildMaterialSuggestions()`, `EventRepository.getLocationSuggestions()` grouping, `splitEvents()`, `todayFlow()` with a fixed `Clock`, `EventFormViewModel` end-date shift and stand fee parsing, `toEventProducts()` split + totals, `AttachSelection` toggling; Room instrumented tests for `linkProducts` / `unlinkProducts` guards, the `createProductsForEvent()` rollback, the source groups in `getFilteredProductsFlow`, the `markLabelsPrinted` guard, the `startEvent` guard, the `getNextEventFlow()` ordering, and the till (`addToBasket` results, `completeSale` incl. `BasketChanged`, `undoSale`)
- [ ] Undo on delete
- [ ] `OrderProductItem`: prices use `toPlainString()` + symbol instead of `Money.format()` (not localized: "12.50 €" instead of "12,50 €")
- [ ] `OrderDao`: `getAttachableProductsFlow()` and `attachProductToOrder()` look unused (the repository uses the `ProductDao` versions); check with Find Usages and remove
- [ ] Hard-coded `'AVAILABLE'` / `'CANCELLED'` / `'SOLD'` literals in SQL: consider passing them as parameters like the event statuses
- [ ] `KEY_PRODUCT_UPDATED` / `KEY_PRODUCT_SAVED` are set on the event detail screen after Edit / Add Product but never read (harmless; list updates via the database). Optionally show a snackbar there
- [ ] Accessibility (optional): `HomeTileRow` icons have no content description, so the next event's status isn't announced by screen readers
- [ ] Label sheets in `cacheDir/labels` are overwritten on each print, never deleted (one small file; optionally clear on app start like the camera temp)
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
  - Until then: every DB version bump wipes the data and restarts IDs, so printed QR labels become invalid and must be reprinted
- [ ] Device pass on hardware
- [ ] Review backup rules
- [ ] Play Store icon