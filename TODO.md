# MyStueberlApp – TODO

## 🎨 Theming (dark mode)
- [x] Add missing color roles to `Theme.kt` (onSurfaceVariant, outline, surfaceContainer*, …) and create `StueberlDefaults.kt`
- [ ] Convert existing screens to `MaterialTheme.colorScheme` + `StueberlDefaults`, one screen at a time
  - Main issue: input fields show white text on `FloralWhite` in dark mode
  - Quick first candidate: cost calculator (`CostCalculatorDialog`, `MaterialPositionRow`, price chips)
  - Home tiles: `HomeTile` / `HomeTileRow` now exist (`view/home/HomeTile.kt`); move `StatisticsTile`, `ProductsTile`, `OrdersTile`, `GalleryTile` onto them and delete the private `TileRow`
  - `AttachProductDialog`: theme-based colors (reused by orders and events)
  - `FilterBottomSheet`: theme-based chip and button colors
  - `ProductPriceField`: theme-based version (the event form's stand fee field already shows the pattern)
  - `ProductDisplayItem` / `ProductBatchItem`: remaining fixed colors (`AccentGold`, `SageGreen`, `TextSecondary`, …)
  - `OrderProductItem`: fixed colors (`TextSecondary`, `AccentGold`, `SageGreen`)
  - `OrderDetailScreen`: due date and other fields still use fixed colors
- Rule for all new screens and components: only `MaterialTheme.colorScheme` and `StueberlDefaults`, no direct imports from `Color.kt`

## ✅ Done: Events feature (part 1)

## ✅ Done: Events feature (part 2: IN_PREPARATION + product linking)

## ✅ Done: Events feature (part 3a: QR labels)

## ✅ Done: Events feature (part 3b: point of sale)

## ✅ Done: Events feature (part 3c: close event)

## 🔧 Events: improvements
- [x] IN_PREPARATION detail screen: "New" / "Existing" fixed in the bottom bar (`EventBottomBar`, shared with PLANNED), always visible with long product lists
- [x] Home screen: running event clearly visible — animated frame on the Events tile (`Modifier.runningBorder()`); `getNextEventFlow()` lists IN_PROGRESS events first, even past their last day; "Still running" / "Läuft noch" via `EventTiming.StillRunning`
- [ ] Optional: protect the statistics screen while an event runs

## 🗓️ Events feature (part 4+, not started)
- [x] "Start event" (IN_PREPARATION → IN_PROGRESS): second row in the bottom bar with confirmation (notes for a future start date and unprinted labels); blocked without available products (guarded in SQL via `EventDao.startEvent()`); afterwards products read-only, info card collapsed, in-progress hint, "Print labels" stays
- [ ] Statistics: stand sales = `orderId IS NULL AND eventSaleId IS NULL`; event revenue as its own channel next to sales area / orders
- [ ] Detail screen for CANCELLED
  - Decide whether edit stays available (currently allowed in every status)
- [ ] Going back a status (possible but rare): other transitions than DONE → IN_PROGRESS (covered by "Reopen")
  - Leaving CANCELLED must clear `cancelledOn`
- [ ] Statistics per event (revenue, stand fee, profit, customers, average basket); `eventResult()` already covers profit
- [ ] Duplicate event for recurring markets
- [ ] Optional: continuous scanning (CameraX + ML Kit)

## 🟡 Cleanup
- [ ] Status icon/tint duplication (3 files)
  - `EventStatusBadge` and `OrderStatusBadge` share the same layout; consider one shared theme-based `StatusBadge` (events already use `eventStatusIconAndTint()`)
- [x] Extract date picker dialog (done as Events step 3)
- [ ] Shared tappable date field: `EventDateField` (event form) and the order due date use the same press-interaction pattern; consider one theme-based `StueberlDateField` in `view/common`
- [ ] Version bumps (by risk):
  - Patch bumps: `room`, `navigation`, `kotlinxSerialization`, `coil`, `zxing`, `codeScanner`
  - Larger jumps (own PR, test camera / gallery / printing): `coreKtx`, `activityCompose`
- [ ] Empty-state flash (e.g. home counts start at `0` instead of `–` while loading)
- [ ] Validation: `salesPrice ≥ purchasePrice` (decide: block saving or only warn)
- [ ] Tests: unit tests exist for `ProductLabelCode`, `LabelSheetLayout`, `eventTiming()` and `eventResult()`. Next candidates: `PriceInput` (comma and dot, `toInputText()` round trip), `sumMoney()`, `buildMaterialSuggestions()`, `EventRepository.getLocationSuggestions()` grouping, `splitEvents()` (incl. DONE → past), `todayFlow()` with a fixed `Clock`, `EventFormViewModel` end-date shift and stand fee parsing, `toEventProducts()` split + totals + `newestFirst`, `toTillFigures()` (incl. `purchaseCost`), `groupSales()`, `EventDetail.canReopen()`, `AttachSelection` toggling, change calculation in the payment panel, `EventTillViewModel.onScanned()`; Room instrumented tests for `linkProducts` / `unlinkProducts` guards, the `createProductsForEvent()` rollback, the source groups in `getFilteredProductsFlow` (incl. till sales), the `markLabelsPrinted` guard, the `startEvent` guard, `closeEvent` / `reopenEvent` guards, the `deleteEvent` guard (events with sales kept), the `getNextEventFlow()` ordering and skipped statuses, and the till (`addToBasket` results, `completeSale` incl. `BasketChanged` and purchase snapshot, `undoSale`, `resetToAvailable` skipping till sales)
- [ ] Undo on delete
- [ ] Hard-coded `'AVAILABLE'` / `'CANCELLED'` / `'SOLD'` literals in SQL: consider passing them as parameters like the event statuses
- [ ] `KEY_PRODUCT_UPDATED` / `KEY_PRODUCT_SAVED` are set on the event detail screen after Edit / Add Product but never read (harmless; list updates via the database). Optionally show a snackbar there
- [ ] `EventDetailScreen.kt` is getting large (summary card, section state, split section helpers); consider moving shared building blocks (`EventBottomBar`, `DetailSectionHeader`, `HintText`) and the sections into their own files
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