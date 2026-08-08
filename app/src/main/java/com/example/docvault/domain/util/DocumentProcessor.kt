package com.example.docvault.domain.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for processing documents, including image compression and PDF conversion.
 *
 * It provides methods to reduce file size significantly to meet strict upload requirements 
 * (e.g., < 40KB) and to bundle images into PDFs.
 */
@Singleton
class DocumentProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Compresses an image from the provided [InputStream].
     * 
     * @param inputStream The source image stream.
     * @param quality Initial JPEG compression quality (0-100).
     * @param targetSizeKb Optional target size in KB. If provided, the method will 
     * aggressively downscale the image until it falls below this size.
     */
    suspend fun compressImage(
        inputStream: InputStream, 
        quality: Int, 
        targetSizeKb: Int? = null
    ): InputStream = withContext(Dispatchers.IO) {
        val originalBytes = inputStream.readBytes()
        var bitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size)
        
        var outputStream = ByteArrayOutputStream()
        var currentQuality = quality
        
        // Initial compression
        bitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, outputStream)
        
        // Aggressive compression loop if target size is specified
        if (targetSizeKb != null) {
            var iteration = 0
            val targetBytes = targetSizeKb * 1024
            
            while (outputStream.size() > targetBytes && iteration < 10) {
                outputStream.reset()
                
                // Downscale dimensions by 20% each time if quality reduction isn't enough
                if (currentQuality > 30) {
                    currentQuality -= 15
                } else {
                    val scale = 0.8f
                    val matrix = Matrix()
                    matrix.postScale(scale, scale)
                    val newBitmap = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                    )
                    if (newBitmap != bitmap) {
                        bitmap.recycle()
                        bitmap = newBitmap
                    }
                }
                
                bitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, outputStream)
                iteration++
            }
        }
        
        ByteArrayInputStream(outputStream.toByteArray())
    }

    /**
     * Converts a list of image [InputStream]s into a single PDF document.
     *
     * @param imageStreams List of input streams for the images to be included in the PDF.
     * @return An [InputStream] containing the generated PDF data.
     */
    suspend fun convertToPdf(imageStreams: List<InputStream>): InputStream = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        
        imageStreams.forEachIndexed { index, inputStream ->
            val bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap != null) {
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                
                val canvas: Canvas = page.canvas
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                
                pdfDocument.finishPage(page)
                bitmap.recycle()
            }
        }

        val outputStream = ByteArrayOutputStream()
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        
        ByteArrayInputStream(outputStream.toByteArray())
    }
}
