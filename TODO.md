# MyStueberlApp – TODO

## 🎨 Theming (dark mode)
- [x] Add missing color roles to `Theme.kt` (onSurfaceVariant, outline, surfaceContainer*, …) and create `StueberlDefaults.kt`, if not done yet
- [ ] Convert existing screens to `MaterialTheme.colorScheme` + `StueberlDefaults`, one screen at a time
    - Main issue: input fields show white text on `FloralWhite` in dark mode
    - Quick first candidate: cost calculator (`CostCalculatorDialog`, `MaterialPositionRow`, price chips)
- Rule for all new screens and components: only `MaterialTheme.colorScheme` and `StueberlDefaults`, no direct imports from `Color.kt`

## 🟡 Cleanup
- [ ] Status icon/tint duplication (3 files)
- [ ] Extract date picker dialog
- [ ] Version bumps: `coreKtx`, `activityCompose`, `junit`, `espresso`
- [ ] Empty-state flash
- [ ] Validation: `salesPrice ≥ purchasePrice`
- [ ] Tests (good first candidate: `buildMaterialSuggestions()`, a pure function)
- [ ] Undo on delete
- [X] German strings: a few still say "Produkt" instead of "Werkstück" (`products_screen_search_no_results`, `attach_product_confirm`, `gallery_screen_empty`)

## 🟠 Deferred features
- [ ] Statistics breakdowns
- [ ] Bulk status actions
- [ ] Batches in orders
- [ ] Sales-price calculator
- [ ] Empty orders surviving process death

## 📦 Pre-release
- [ ] R8
- [ ] Real migrations (replace the debug-only destructive migration)
    - Enable `exportSchema = true` with a schema folder early, so you have every schema version to write migrations from
- [ ] Device pass on hardware
- [ ] Review backup rules
- [ ] Play Store icon