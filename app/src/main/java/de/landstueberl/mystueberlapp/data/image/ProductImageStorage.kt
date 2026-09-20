package de.landstueberl.mystueberlapp.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Owns product image files in internal storage. The database stores only the
 * generated file name; absolute paths are resolved here so they stay valid
 * across reinstalls, restores and secondary user profiles.
 */
class ProductImageStorage(private val context: Context) {

    private val imagesDir: File
        get() = File(context.filesDir, IMAGES_DIR).apply { mkdirs() }

    private val cameraTempDir: File
        get() = File(context.cacheDir, CAMERA_DIR).apply { mkdirs() }

    /** Resolves a stored file name to a readable [File]. */
    fun fileFor(fileName: String): File = File(imagesDir, fileName)

    /**
     * Creates an empty file in the cache and returns a content URI the camera
     * app may write into. The file is temporary — [importImage] copies it into
     * permanent storage afterwards.
     */
    fun createCameraTarget(): CameraTarget {
        val file = File(cameraTempDir, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return CameraTarget(file = file, uri = uri)
    }

    /**
     * Copies the image at [source] into internal storage, downscaled to at most
     * [MAX_DIMENSION] on its longest edge and rotation-corrected from EXIF.
     * Returns the generated file name.
     */
    suspend fun importImage(source: Uri): String = withContext(Dispatchers.IO) {
        val decoded = decodeDownscaled(source)
            ?: error("Could not decode image at $source — file may be empty or not an image")

        val oriented = applyExifRotation(source, decoded)
        val fileName = "prod_${UUID.randomUUID()}.jpg"

        File(imagesDir, fileName).outputStream().use { out ->
            oriented.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }

        if (oriented !== decoded) decoded.recycle()
        oriented.recycle()

        fileName
    }

    suspend fun delete(fileName: String): Unit = withContext(Dispatchers.IO) {
        File(imagesDir, fileName).delete()
    }

    /** Deletes stored files whose names are not in [keep]. */
    suspend fun deleteOrphans(keep: Set<String>): Unit = withContext(Dispatchers.IO) {
        imagesDir.listFiles()?.forEach { file ->
            if (file.name !in keep) file.delete()
        }
    }

    suspend fun clearCameraTemp(): Unit = withContext(Dispatchers.IO) {
        cameraTempDir.listFiles()?.forEach { it.delete() }
    }

    // ── Internals ──────────────────────────────

    /** Two-pass decode: read bounds first, then load at a reduced sample size. */
    private fun decodeDownscaled(source: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }

        // Note: decodeStream returns null when inJustDecodeBounds is set, so the
        // result is ignored — the bounds are read from `bounds` instead.
        val opened = context.contentResolver.openInputStream(source)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
            true
        } ?: false

        if (!opened) return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
        }
        return context.contentResolver.openInputStream(source)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        var longest = maxOf(width, height)
        while (longest / 2 >= MAX_DIMENSION) {
            longest /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun applyExifRotation(source: Uri, bitmap: Bitmap): Bitmap {
        val orientation = context.contentResolver.openInputStream(source)?.use { input ->
            ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: return bitmap

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    data class CameraTarget(val file: File, val uri: Uri)

    companion object {
        private const val IMAGES_DIR = "images"
        private const val CAMERA_DIR = "camera"
        private const val MAX_DIMENSION = 1600
        private const val JPEG_QUALITY = 85
    }
}