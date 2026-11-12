package de.landstueberl.mystueberlapp.data.label

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/** Writes a label sheet and hands it to Android's print dialog. */
object LabelPrinter {

    /** Rendered off the main thread. Overwrites the previous sheet in the cache. */
    suspend fun createPdf(context: Context, labels: List<ProductLabel>): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "labels").apply { mkdirs() }
            val file = File(dir, "labels.pdf")
            FileOutputStream(file).use { LabelSheetPdf.write(labels, it) }
            file
        }

    /** [context] must be an Activity context; PrintManager needs a window. */
    fun print(context: Context, pdf: File, pageCount: Int, jobName: String) {
        val printManager = context.getSystemService(PrintManager::class.java)
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .build()
        printManager.print(jobName, PdfFileAdapter(pdf, pageCount, jobName), attributes)
    }
}

/** Serves an already rendered PDF; layout never changes with print settings. */
private class PdfFileAdapter(
    private val pdf: File,
    private val pageCount: Int,
    private val jobName: String
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder("$jobName.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(pageCount)
            .build()
        callback.onLayoutFinished(info, oldAttributes != newAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback
    ) {
        try {
            FileInputStream(pdf).use { input ->
                FileOutputStream(destination.fileDescriptor).use { output -> input.copyTo(output) }
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.message)
        }
    }
}