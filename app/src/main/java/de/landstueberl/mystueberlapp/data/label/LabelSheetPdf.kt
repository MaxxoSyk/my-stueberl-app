package de.landstueberl.mystueberlapp.data.label

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import de.landstueberl.mystueberlapp.data.label.LabelSheetLayout.mmToPt
import java.io.OutputStream
import kotlin.math.roundToInt

/** Renders labels onto A4 pages, [LabelSheetLayout.labelsPerPage] per page. */
object LabelSheetPdf {

    private const val CAPTION_TEXT_PT = 6f

    fun write(labels: List<ProductLabel>, out: OutputStream) {
        require(labels.isNotEmpty()) { "No labels to print" }

        val pageWidth = mmToPt(LabelSheetLayout.PAGE_WIDTH_MM).roundToInt()
        val pageHeight = mmToPt(LabelSheetLayout.PAGE_HEIGHT_MM).roundToInt()

        val captionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = CAPTION_TEXT_PT // canvas units are points
            color = Color.rgb(0x55, 0x55, 0x55)
            textAlign = Paint.Align.CENTER
        }
        val cutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
            color = Color.rgb(0xBB, 0xBB, 0xBB)
            pathEffect = DashPathEffect(floatArrayOf(3f, 3f), 0f)
        }

        val document = PdfDocument()
        try {
            labels.chunked(LabelSheetLayout.labelsPerPage).forEachIndexed { index, pageLabels ->
                val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = document.startPage(info)
                pageLabels.forEachIndexed { i, label ->
                    page.canvas.drawLabel(
                        label = label,
                        column = i % LabelSheetLayout.columns,
                        row = i / LabelSheetLayout.columns,
                        captionPaint = captionPaint,
                        cutPaint = cutPaint
                    )
                }
                document.finishPage(page)
            }
            document.writeTo(out)
        } finally {
            document.close()
        }
    }

    private fun Canvas.drawLabel(
        label: ProductLabel,
        column: Int,
        row: Int,
        captionPaint: TextPaint,
        cutPaint: Paint
    ) {
        val left = mmToPt(LabelSheetLayout.cellLeftMm(column))
        val top = mmToPt(LabelSheetLayout.cellTopMm(row))
        val cell = mmToPt(LabelSheetLayout.CELL_MM)
        val code = mmToPt(LabelSheetLayout.CODE_MM)

        drawQrCode(
            matrix = QrCodes.matrix(label.code.encode()),
            left = left + (cell - code) / 2,
            top = top + mmToPt(LabelSheetLayout.CODE_TOP_MM),
            size = code
        )

        val caption = TextUtils.ellipsize(label.caption, captionPaint, code, TextUtils.TruncateAt.END)
        drawText(
            caption.toString(),
            left + cell / 2,
            top + mmToPt(LabelSheetLayout.CAPTION_BASELINE_MM),
            captionPaint
        )

        // Neighbouring cells share edges, so the lines coincide.
        drawRect(left, top, left + cell, top + cell, cutPaint)
    }
}