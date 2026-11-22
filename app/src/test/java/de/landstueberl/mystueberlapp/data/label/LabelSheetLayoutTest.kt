package de.landstueberl.mystueberlapp.data.label

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LabelSheetLayoutTest {

    @Test
    fun `A4 holds 5 x 7 labels of 4 cm`() {
        assertEquals(5, LabelSheetLayout.columns)
        assertEquals(7, LabelSheetLayout.rows)
        assertEquals(35, LabelSheetLayout.labelsPerPage)
    }

    @Test
    fun `page count rounds up`() {
        assertEquals(0, LabelSheetLayout.pageCount(0))
        assertEquals(1, LabelSheetLayout.pageCount(1))
        assertEquals(1, LabelSheetLayout.pageCount(35))
        assertEquals(2, LabelSheetLayout.pageCount(36))
    }

    @Test
    fun `grid stays inside the printable margins`() {
        val last = LabelSheetLayout.columns - 1
        val bottom = LabelSheetLayout.rows - 1
        assertTrue(LabelSheetLayout.cellLeftMm(0) >= LabelSheetLayout.MIN_MARGIN_MM - 0.01f)
        assertTrue(
            LabelSheetLayout.cellLeftMm(last) + LabelSheetLayout.CELL_MM <=
                    LabelSheetLayout.PAGE_WIDTH_MM - LabelSheetLayout.MIN_MARGIN_MM + 0.01f
        )
        assertTrue(LabelSheetLayout.cellTopMm(0) >= LabelSheetLayout.MIN_MARGIN_MM)
        assertTrue(
            LabelSheetLayout.cellTopMm(bottom) + LabelSheetLayout.CELL_MM <=
                    LabelSheetLayout.PAGE_HEIGHT_MM - LabelSheetLayout.MIN_MARGIN_MM
        )
    }
}