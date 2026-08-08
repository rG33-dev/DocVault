package com.example.docvault.domain.util

import android.content.Context
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for scanning barcodes and QR codes using ML Kit.
 *
 * It provides a way to extract URLs or raw data from images captured via camera 
 * or picked from the gallery.
 */
@Singleton
class BarcodeScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_AZTEC)
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    /**
     * Processes an image to find and extract data from QR codes.
     *
     * @param image The ML Kit [InputImage] to scan.
     * @return The text content of the first detected QR code, or null if none found.
     */
    suspend fun scanQrCode(image: InputImage): String? {
        return try {
            val barcodes = scanner.process(image).await()
            // In some versions of ML Kit, these are accessible via getter-like properties
            val firstBarcode = barcodes.firstOrNull()
            firstBarcode?.displayValue ?: firstBarcode?.rawValue
        } catch (e: Exception) {
            null
        }
    }
}
