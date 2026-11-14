package de.landstueberl.mystueberlapp.data.label

import android.content.Context
import android.util.Log
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/** Google's code scanner, limited to QR codes (our labels). */
object ProductScanner {

    private const val TAG = "ProductScanner"

    private val OPTIONS = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        // Zooms in by itself on small or distant codes.
        .enableAutoZoom()
        .build()

    /** [context] should be an Activity context: the scanner opens its own screen. */
    fun client(context: Context): GmsBarcodeScanner = GmsBarcodeScanning.getClient(context, OPTIONS)

    /**
     * Downloads the scanner module ahead of time, so scanning works offline at
     * the market. Returns quickly if it's already installed; failures only log.
     */
    fun preload(context: Context) {
        val request = ModuleInstallRequest.newBuilder()
            .addApi(GmsBarcodeScanning.getClient(context))
            .build()
        ModuleInstall.getClient(context)
            .installModules(request)
            .addOnFailureListener { Log.w(TAG, "Scanner module could not be preloaded", it) }
    }
}