package de.landstueberl.mystueberlapp.data.label

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrCodes {

    /**
     * White border around the code, in modules. The QR standard asks for 4;
     * scanners need it to find the code's edges.
     */
    const val QUIET_ZONE_MODULES = 4

    /**
     * Level M survives about 15 % damage (a scratched or partly covered label).
     * Realistic ids still fit the smallest QR size (see [ProductLabelCode]).
     */
    private val HINTS = mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 0 // the quiet zone is drawn by drawQrCode
    )

    /** One entry per module, without quiet zone. Size 0 = native module size. */
    fun matrix(content: String): BitMatrix =
        QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, HINTS)

    /** For on-screen previews. The PDF draws with [drawQrCode] directly. */
    fun bitmap(content: String, sizePx: Int): Bitmap {
        val bitmap = createBitmap(sizePx, sizePx)
        Canvas(bitmap).drawQrCode(matrix(content), left = 0f, top = 0f, size = sizePx.toFloat())
        return bitmap
    }
}

/**
 * Draws [matrix] into a square at ([left], [top]) of [size], including the
 * quiet zone. Works the same on a bitmap and on a PDF page.
 *
 * The dark modules go into one path of horizontal runs: fewer shapes in the
 * PDF, and no hairline seams between neighbouring squares in PDF viewers.
 */
fun Canvas.drawQrCode(matrix: BitMatrix, left: Float, top: Float, size: Float) {
    val quiet = QrCodes.QUIET_ZONE_MODULES
    val moduleSize = size / (matrix.width + 2 * quiet)
    val paint = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.FILL
    }

    paint.color = Color.WHITE
    drawRect(left, top, left + size, top + size, paint)

    val path = Path()
    for (y in 0 until matrix.height) {
        var x = 0
        while (x < matrix.width) {
            if (!matrix[x, y]) {
                x++
                continue
            }
            val start = x
            while (x < matrix.width && matrix[x, y]) x++

            val l = left + (start + quiet) * moduleSize
            val t = top + (y + quiet) * moduleSize
            path.addRect(l, t, l + (x - start) * moduleSize, t + moduleSize, Path.Direction.CW)
        }
    }

    paint.color = Color.BLACK
    drawPath(path, paint)
}