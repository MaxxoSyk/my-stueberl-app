package de.landstueberl.mystueberlapp.data.label

/**
 * Geometry of the A4 label sheet, in millimetres. Pure numbers, so the grid
 * can be unit tested without Android.
 */
object LabelSheetLayout {

    const val PAGE_WIDTH_MM = 210f
    const val PAGE_HEIGHT_MM = 297f

    /** Most home printers can't print closer to the paper edge than this. */
    const val MIN_MARGIN_MM = 5f

    const val CELL_MM = 40f

    /** The code square including its white quiet zone; centred horizontally. */
    const val CODE_MM = 34f
    const val CODE_TOP_MM = 1f

    /** Baseline of the caption, measured from the top of the cell. */
    const val CAPTION_BASELINE_MM = 37.5f

    // +0.001 so 200 mm / 40 mm counts as exactly 5 despite float rounding.
    val columns: Int = ((PAGE_WIDTH_MM - 2 * MIN_MARGIN_MM) / CELL_MM + 0.001f).toInt()
    val rows: Int = ((PAGE_HEIGHT_MM - 2 * MIN_MARGIN_MM) / CELL_MM + 0.001f).toInt()
    val labelsPerPage: Int = columns * rows

    fun pageCount(labelCount: Int): Int = (labelCount + labelsPerPage - 1) / labelsPerPage

    /** The grid is centred on the page, so leftover space is split evenly. */
    fun cellLeftMm(column: Int): Float =
        (PAGE_WIDTH_MM - columns * CELL_MM) / 2 + column * CELL_MM

    fun cellTopMm(row: Int): Float =
        (PAGE_HEIGHT_MM - rows * CELL_MM) / 2 + row * CELL_MM

    /** PDF pages are measured in points: 72 per inch. */
    fun mmToPt(mm: Float): Float = mm * 72f / 25.4f
}