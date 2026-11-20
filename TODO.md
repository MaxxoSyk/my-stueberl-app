# MyStueberlApp – TODO

## 🎨 Theming (dark mode)
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

## 🗓️ Events feature (part 4+, not started)
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
- [ ] Shared tappable date field: `EventDateField` (event form) and the order due date use the same press-interaction pattern; consider one theme-based `StueberlDateField` in `view/common`
- [ ] Empty-state flash (e.g. home counts start at `0` instead of `–` while loading)
- [ ] Validation: `salesPrice ≥ purchasePrice` (decide: block saving or only warn)
- [ ] Tests: unit tests exist for `ProductLabelCode`, `LabelCode`, `LabelIdentifiers`, `LabelSheetLayout`, `eventTiming()` and `eventResult()`. Next candidates: `toBasketLines()`, `PriceInput` (comma and dot, `toInputText()` round trip), `sumMoney()`, `buildMaterialSuggestions()`, `EventRepository.getLocationSuggestions()` grouping, `splitEvents()` (incl. DONE → past), `todayFlow()` with a fixed `Clock`, `EventFormViewModel` end-date shift and stand fee parsing, `toEventProducts()` split + totals + `newestFirst`, `toTillFigures()` (incl. `purchaseCost`), `groupSales()`, `EventDetail.canReopen()`, `hasLabel()`, `AttachSelection` toggling, change calculation in the payment panel, `EventTillViewModel.onScanned()` (product + group codes); Room instrumented tests for `linkProducts` / `unlinkProducts` guards (incl. identifier assignment), the `createProductsForEvent()` rollback, the source groups in `getFilteredProductsFlow` (incl. till sales), the `markLabelsPrinted` / `markBatchLabelsPrinted` no-mixing rules, the `startEvent` guard, `closeEvent` / `reopenEvent` guards, the `deleteEvent` guard (events with sales kept), the `getNextEventFlow()` ordering and skipped statuses, and the till (`addToBasket` / `addNextOfBatch` results, `completeSale` incl. `BasketChanged` and purchase snapshot, `undoSale`, `resetToAvailable` skipping till sales)
- [ ] Undo on delete
- [ ] Hard-coded `'AVAILABLE'` / `'CANCELLED'` / `'SOLD'` literals in SQL: consider passing them as parameters like the event statuses
- [ ] `KEY_PRODUCT_UPDATED` / `KEY_PRODUCT_SAVED` are set on the event detail screen after Edit / Add Product but never read (harmless; list updates via the database). Optionally show a snackbar there
- [ ] `EventDetailScreen.kt` is getting large (summary card, section state, split section helpers, label printing); consider moving shared building blocks (`EventBottomBar`, `DetailSectionHeader`, `HintText`) and the sections into their own files

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
  - Until then: every DB version bump wipes the data and restarts IDs, so printed QR labels become invalid and must be reprinted
- [ ] Review backup rules
- [ ] Play Store icon