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
- [x] 1. Data model (`EventStatus`, `EventDetail`, `EventProductDetail`, DB version 14)
- [x] 2. `EventDao`, `EventRepository`, app wiring
- [x] 3. Shared date picker (`StueberlDatePickerDialog`)
- [x] 4. Events list screen + navigation (upcoming / past by date, product count on cards, simple home tile)
- [x] 5. Create / edit event form (location suggestions, end date with `minDate`, start date shifts end date, stand fee `null` vs `0`; unsaved changes discarded without asking, like orders)
- [x] 6. Event detail screen for PLANNED: info, "Start preparation" with confirmation, edit, delete; own "Event saved" snackbar after editing
- [x] 7. Home tile "Events": next event with timing (today / tomorrow / in X days / until), "–" when none, updates at midnight; scrollable home screen

## ✅ Done: Events feature (part 2: IN_PREPARATION + product linking)
Rules: link/unlink only in IN_PREPARATION; one active event per product; exclusive with orders; products in an active event are NOT stand stock; sold/removed before the event stays linked (doesn't count toward totals or the tile counter)
- [x] 1. Data access: event products (join ordered by `addedAt`, then `ep.rowid`), attachable products, guarded link/unlink, cancel event (sets `cancelledOn`), order picker + `attachProductsToOrder` exclude event products, stand count excludes event products, next-event product count (AVAILABLE only); `EventStatus.ACTIVE_NAMES` / `CANCELLABLE_NAMES`; DB version 15; `EventFormViewModel.saveEvent()` uses `base.copy(...)`
- [x] 2. IN_PREPARATION detail screen: collapsible info card, products grouped by batch, "No longer available" section, totals, remove with confirmation, tap → Edit Product, "Cancel event", "Cancelled on" in footer; theme-based `EventProductItem` / `EventProductBatchItem`
- [x] 3. "Existing": `AttachProductDialog` reused (optional `emptyText`); shared `AttachSelection` helper; "Existing" button in IN_PREPARATION
- [x] 4. "New": `AddProduct(orderId, eventId)`, batches allowed, products + links in one transaction (`createProductsForEvent()`); event rows match the product list
- [x] 5. Product list + home tile: "Event" badge, "Go to event" in the bottom sheet, "From event" filter chip (available + in an active event); exhaustive `when` on `ProductSourceFilter`; Events tile counter for AVAILABLE products of the next event; next event's status icon on the tile (`eventStatusIconAndTint()`)

## 🚧 In progress: Events feature (part 3a: QR labels, before "Start event")
Decisions: code = `STUEBERL:1:<eventId>:<productId>` (label only valid for its event); plain A4 paper, cut by hand; QR only, plus a description caption to match code ↔ product; 4 × 4 cm cells (35 per page) with dashed cut lines; one label per product (batch members too); remember printed labels; printing in IN_PREPARATION and IN_PROGRESS
- [x] 1. QR format + generator: ZXing core 3.5.3, `ProductLabelCode.encode()` / `decode()`, `QrCodes.matrix()` / `bitmap()`, `Canvas.drawQrCode()`; first unit test `ProductLabelCodeTest`
  - Smallest QR size (21×21) while both ids together have ≤ 8 digits; beyond that 25×25
- [x] 2. A4 sheet PDF + printing: `LabelSheetLayout` (pure, tested), `LabelSheetPdf` (`PdfDocument`), `LabelPrinter` (`PrintManager`); `drawQrCode()` draws one path (no seams); `allProducts()` helper; printout and scan tested
- [x] 3. "Print labels" in the overflow menu (IN_PREPARATION / IN_PROGRESS, only with available products); theme-based `LabelSelectionDialog` (everything preselected, tri-state batch checkbox, select all / none, page count); `EventStatus.canPrintLabels`; `AttachSelection.replaceWith()`
- [ ] 4. Remember printed labels: `EventProductDetail.labelPrintedAt` (per link, so relinking to a new event resets it), "only new ones" preselected; DB version bump
  - `selectAllForLabels()` is the hook: preselect only unprinted products
  - Decide: mark as printed when the print dialog opens, or ask after printing ("Did the labels print?")
- Note: labels printed before a destructive migration become invalid (IDs restart); reprint after schema changes during development
- Note: print at 100 % / actual size; printers with a large unprintable edge may scale the page slightly
- POS later: scan via `GmsBarcodeScanning`; `ProductLabelCode.decode()`, then check event ID = running event and product linked + AVAILABLE

## 🗓️ Events feature (part 3+, not started)
- [ ] Sale channel: `ProductDetail.soldAtEventId` (decided: option A)
  - Set by POS / "Close event"; stand sales (incl. before the event and after DONE/CANCELLED) stay `null`
  - "Reset to available" clears it
  - Foreign key `RESTRICT` as a safety net against deleting an event with sales
  - Statistics: stand sales = `orderId IS NULL AND soldAtEventId IS NULL`; event revenue as its own channel
  - Product list: "From event" should then also include products sold at an event (`soldAtEventId IS NOT NULL`); today it only covers available products in an active event
- [ ] Deleting events (decided): blocked for IN_PROGRESS / DONE events that have products sold at the event; PLANNED / IN_PREPARATION / CANCELLED and events without sales stay deletable
- [ ] Detail screens for IN_PROGRESS / DONE / CANCELLED
  - Decide per status whether edit stays available (currently allowed in every status)
- [ ] "Start event" (IN_PREPARATION → IN_PROGRESS) with confirmation
- [ ] Going back a status (possible but rare: overflow menu + confirmation)
  - Leaving CANCELLED must clear `cancelledOn`
- [ ] No adding/removing products during IN_PROGRESS (already true: remove icons and "New" / "Existing" only show in IN_PREPARATION, and link/unlink are guarded in SQL)
- [ ] Point-of-sale screen for IN_PROGRESS (basket per customer, total, lock basket, cash given → change; scan product labels)
- [ ] Mark sales during the event (sale date defaults to the event date)
- [ ] "Close event" flow (mark sold items, the rest go back to the stand)
- [ ] Statistics per event (revenue, stand fee, profit); event sales as their own channel next to sales area / orders
- [ ] Duplicate event for recurring markets

## 🟡 Cleanup
- [ ] Status icon/tint duplication (3 files)
  - `EventStatusBadge` and `OrderStatusBadge` share the same layout; consider one shared theme-based `StatusBadge` (events already use `eventStatusIconAndTint()`)
- [x] Extract date picker dialog (done as Events step 3)
- [ ] Version bumps: `coreKtx`, `activityCompose`, `junit`, `espresso`
- [ ] Empty-state flash (e.g. home counts start at `0` instead of `–` while loading)
- [ ] Validation: `salesPrice ≥ purchasePrice`
- [ ] Tests: first unit tests exist (`ProductLabelCodeTest`, `LabelSheetLayoutTest`). Next candidates: `buildMaterialSuggestions()`, `EventRepository.getLocationSuggestions()` grouping, `splitEvents()`, `todayFlow()` with a fixed `Clock`, `EventFormViewModel` end-date shift and stand fee parsing, `eventTiming()`, `toEventProducts()` split + totals, `AttachSelection` toggling; Room instrumented tests for `linkProducts` / `unlinkProducts` guards, the `createProductsForEvent()` rollback and the source groups in `getFilteredProductsFlow`
- [ ] Undo on delete
- [ ] `OrderProductItem`: prices use `toPlainString()` + symbol instead of `Money.format()` (not localized: "12.50 €" instead of "12,50 €")
- [ ] `OrderDao`: `getAttachableProductsFlow()` and `attachProductToOrder()` look unused (the repository uses the `ProductDao` versions); check with Find Usages and remove
- [ ] Hard-coded `'AVAILABLE'` / `'CANCELLED'` literals in SQL: consider passing them as parameters like the event statuses
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
- [ ] Device pass on hardware
- [ ] Review backup rules
- [ ] Play Store icon