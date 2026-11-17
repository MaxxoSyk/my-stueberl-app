# MyStueberlApp – TODO

## 🎨 Theming (dark mode)
- [x] Add missing color roles to `Theme.kt` (onSurfaceVariant, outline, surfaceContainer*, …) and create `StueberlDefaults.kt`
- [ ] Convert existing screens to `MaterialTheme.colorScheme` + `StueberlDefaults`, one screen at a time
  - Main issue: input fields show white text on `FloralWhite` in dark mode
  - Quick first candidate: cost calculator (`CostCalculatorDialog`, `MaterialPositionRow`, price chips)
  - Home tiles: `HomeTile` / `HomeTileRow` now exist (`view/home/HomeTile.kt`); move `StatisticsTile`, `ProductsTile`, `OrdersTile`, `GalleryTile` onto them and delete the private `TileRow`
  - Home screen top bar: switch to `StueberlDefaults.topAppBarColors()`
  - `StueberlConfirmationDialog`: switch to `StueberlDefaults.primaryButtonColors()` / `destructiveButtonColors()` (also used by the event detail and till screens)
  - `AttachProductDialog`: theme-based colors (reused by orders and events)
  - `FilterBottomSheet`: theme-based chip and button colors
  - `ProductPriceField`: theme-based version (the event form's stand fee field already shows the pattern)
  - `OrderBadge`: theme-based, like the new `EventBadge`
- Rule for all new screens and components: only `MaterialTheme.colorScheme` and `StueberlDefaults`, no direct imports from `Color.kt`

## ✅ Done: Events feature (part 1)

## ✅ Done: Events feature (part 2: IN_PREPARATION + product linking)

## ✅ Done: Events feature (part 3a: QR labels)

## ✅ Done: Events feature (part 3b: point of sale)

## 🚧 In progress: Events feature (part 3c: close event)
Decisions: open basket blocks closing; no recording of sales made without the till; profit = revenue − purchase price of sold products − stand fee (purchase total stored as a snapshot per sale; without stand fee shown as "without stand fee"); DONE events editable; no till for DONE events; "Reopen" while the last day is today or later, refused if a product was assigned elsewhere meanwhile; DONE skipped on the home tile and moved to "Past" immediately
- [x] 1. Data: `EventDetail.closedOn` + `canReopen()`; `EventSale.purchaseTotal`; `TillFigures.purchaseCost`; `EventDao.closeEvent()` (empty basket) / `reopenEvent()` (last day, no product assigned elsewhere); `getNextEventFlow()` skips CANCELLED + DONE; `splitEvents()` moves DONE to past; `getBasketCountFlow()`, `hasSalesFlow()`; DB version 18
- [x] 2. Close + reopen: outlined "Close event" below "Open till", disabled with basket hint while products are in the basket; confirmation with summary (revenue, items, customers, products returning, note before the last day, sales final + labels invalid); "Reopen" in ⋮ while `canReopen()`, with confirmation; `EventNotice` snackbars for a blocked close / refused reopen
- [ ] 3. DONE detail screen: summary card (revenue, items, customers, purchase cost, stand fee, profit), section "Back to the stand", read-only, no till, footer "Closed on …"
- [ ] 4. Delete guard: events with sales can't be deleted (message instead of the constraint error)

## 🔧 Events: improvements
- [x] IN_PREPARATION detail screen: "New" / "Existing" fixed in the bottom bar (`EventBottomBar`, shared with PLANNED), always visible with long product lists
- [x] Home screen: running event clearly visible — animated frame on the Events tile (`Modifier.runningBorder()`, 5 s per lap, highlight 20 %); `getNextEventFlow()` lists IN_PROGRESS events first, even past their last day; "Still running" / "Läuft noch" via `EventTiming.StillRunning`
- [ ] Optional polish for the running frame
  - Static frame (no moving highlight) when system animations are turned off
  - Constant speed along the frame (currently slightly faster on the short edges)
- [ ] Optional privacy during an event
  - Hide sale totals in the till's sales sheet the same way ("••••", tap to reveal)
  - Protect the statistics screen while an event runs

## 🗓️ Events feature (part 3+, not started)
- [x] "Start event" (IN_PREPARATION → IN_PROGRESS): second row in the bottom bar with confirmation (notes for a future start date and unprinted labels); blocked without available products (guarded in SQL via `EventDao.startEvent()`); afterwards products read-only, info card collapsed, in-progress hint, "Print labels" stays
- [ ] Statistics: stand sales = `orderId IS NULL AND eventSaleId IS NULL`; event revenue as its own channel next to sales area / orders
- [ ] Detail screen for CANCELLED
  - Decide whether edit stays available (currently allowed in every status)
- [ ] Going back a status (possible but rare): other transitions than DONE → IN_PROGRESS (covered by "Reopen")
  - Leaving CANCELLED must clear `cancelledOn`
- [ ] Statistics per event (revenue, stand fee, profit, customers, average basket)
- [ ] Duplicate event for recurring markets
- [ ] Optional: continuous scanning (CameraX + ML Kit)

## 🟡 Cleanup
- [ ] Status icon/tint duplication (3 files)
  - `EventStatusBadge` and `OrderStatusBadge` share the same layout; consider one shared theme-based `StatusBadge` (events already use `eventStatusIconAndTint()`)
- [x] Extract date picker dialog (done as Events step 3)
- [ ] Version bumps: `coreKtx`, `activityCompose`, `junit`, `espresso`
- [ ] Empty-state flash (e.g. home counts start at `0` instead of `–` while loading)
- [ ] Validation: `salesPrice ≥ purchasePrice`
- [ ] Tests: unit tests exist for `ProductLabelCode`, `LabelSheetLayout` and `eventTiming()`. Next candidates: `buildMaterialSuggestions()`, `EventRepository.getLocationSuggestions()` grouping, `splitEvents()` (incl. DONE → past), `todayFlow()` with a fixed `Clock`, `EventFormViewModel` end-date shift and stand fee parsing, `toEventProducts()` split + totals, `toTillFigures()` (incl. `purchaseCost`), `groupSales()`, `EventDetail.canReopen()`, `AttachSelection` toggling, change calculation in the payment panel, `EventTillViewModel.onScanned()`; Room instrumented tests for `linkProducts` / `unlinkProducts` guards, the `createProductsForEvent()` rollback, the source groups in `getFilteredProductsFlow` (incl. till sales), the `markLabelsPrinted` guard, the `startEvent` guard, `closeEvent` / `reopenEvent` guards, the `getNextEventFlow()` ordering and skipped statuses, and the till (`addToBasket` results, `completeSale` incl. `BasketChanged` and purchase snapshot, `undoSale`, `resetToAvailable` skipping till sales)
- [ ] Undo on delete
- [ ] `OrderProductItem`: prices use `toPlainString()` + symbol instead of `Money.format()` (not localized: "12.50 €" instead of "12,50 €")
- [ ] `OrderDao`: `getAttachableProductsFlow()` and `attachProductToOrder()` look unused (the repository uses the `ProductDao` versions); check with Find Usages and remove
- [ ] Hard-coded `'AVAILABLE'` / `'CANCELLED'` / `'SOLD'` literals in SQL: consider passing them as parameters like the event statuses
- [ ] Two private `sumMoney` helpers (`EventProducts.kt`, `TillFigures.kt`); consider one shared `Money` sum helper
- [ ] `KEY_PRODUCT_UPDATED` / `KEY_PRODUCT_SAVED` are set on the event detail screen after Edit / Add Product but never read (harmless; list updates via the database). Optionally show a snackbar there
- [ ] Accessibility (optional): `HomeTileRow` icons have no content description, so the next event's status isn't announced by screen readers
- [ ] Label sheets in `cacheDir/labels` are overwritten on each print, never deleted (one small file; optionally clear on app start like the camera temp)
- [ ] `EventDetailScreen.kt` is getting large; consider moving shared building blocks (`EventBottomBar`, `DetailSectionHeader`, `HintText`) into their own file (now `internal` for the till screen)
- [x] German strings: a few still said "Produkt" instead of "Werkstück" (`products_screen_search_no_results`, `attach_product_confirm`, `gallery_screen_empty`)

## 🟠 Deferred features
- [ ] Statistics breakdowns
- [ ] Bulk status actions
- [ ] Batches in orders
- [ ] Sales-price calculator
- [ ] Empty orders surviving process death

## 📦 Pre-release
- [ ] R8
  - Check that the code scanner and ZXing still work in a minified release build
- [ ] Real migrations (replace the debug-only destructive migration)
  - [x] `exportSchema = true` with `app/schemas/` configured; JSON files are generated and checked in
  - Until then: every DB version bump wipes the data and restarts IDs, so printed QR labels become invalid and must be reprinted
- [ ] Device pass on hardware
  - Scanning needs Google Play Services; test offline scanning on the phone you'll use at the market
  - Emulator: set the time zone to Berlin, otherwise sale times, "today" and the midnight switch follow UTC
- [ ] Review backup rules
- [ ] Play Store icon