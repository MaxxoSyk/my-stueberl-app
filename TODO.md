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
- [x] 2. A4 sheet PDF + printing: `LabelSheetLayout` (pure, tested), `LabelSheetPdf` (`PdfDocument`), `LabelPrinter` (`PrintManager`); `drawQrCode()` draws one path (no seams); `allProducts()` helper
  - Printout and scan test still open: done together with step 3
- [ ] 3. Event detail: "Print labels" (AVAILABLE products only), selection, print dialog
  - Also test step 2 here: grid, captions, cut lines, multiple pages, 4 cm cells at 100 %, scan result
- [ ] 4. Remember printed labels: `EventProductDetail.labelPrintedAt` (per link, so relinking to a new event resets it), "only new ones" preselected; DB version bump
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
  -