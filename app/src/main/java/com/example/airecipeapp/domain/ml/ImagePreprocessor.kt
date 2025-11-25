package com.example.airecipeapp.domain.ml

import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.max

/**
 * Optimized image preprocessing for fast OCR processing
 * Uses ColorMatrix for efficient transformations instead of pixel-by-pixel operations
 */
class ImagePreprocessor(private val context: Context) {
    
    companion object {
        private const val MAX_IMAGE_DIMENSION = 1024 // Optimized for speed
    }
    
    /**
     * Preprocess image for optimal OCR results
     * Significantly faster than pixel-by-pixel operations
     */
    suspend fun preprocessImage(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        try {
            // Correct rotation if needed
            val rotatedBitmap = bitmap
            
            // Convert to grayscale using ColorMatrix (fast)
            val grayBitmap = toGrayscale(rotatedBitmap)
            
            // Enhance contrast using ColorMatrix (fast)
            val enhancedBitmap = enhanceContrast(grayBitmap)
            
            enhancedBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }
    
    /**
     * Preprocess image from URI with rotation correction
     */
    suspend fun preprocessImageFromUri(imageUri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // Load and scale bitmap
            val originalBitmap = loadAndScaleBitmap(imageUri) ?: return@withContext null
            
            // Correct rotation based on EXIF
            val rotatedBitmap = correctRotation(imageUri, originalBitmap)
            
            // Apply preprocessing
            preprocessImage(rotatedBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun loadAndScaleBitmap(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                
                // Calculate scale factor
                val scaleFactor = max(
                    options.outWidth / MAX_IMAGE_DIMENSION,
                    options.outHeight / MAX_IMAGE_DIMENSION
                )
                
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = max(1, scaleFactor)
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    
    private fun correctRotation(uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                
                val rotation = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
                
                if (rotation != 0f) {
                    val matrix = Matrix()
                    matrix.postRotate(rotation)
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                } else {
                    bitmap
                }
            } ?: bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }
    
    /**
     * Fast grayscale conversion using ColorMatrix
     */
    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val grayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayBitmap)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        val filter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = filter
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return grayBitmap
    }
    
    /**
     * Fast contrast enhancement using ColorMatrix
     */
    private fun enhanceContrast(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val enhancedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(enhancedBitmap)
        val paint = Paint()
        
        // Increase contrast using ColorMatrix
        val cm = ColorMatrix(floatArrayOf(
            1.5f, 0f, 0f, 0f, -40f,
            0f, 1.5f, 0f, 0f, -40f,
            0f, 0f, 1.5f, 0f, -40f,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return enhancedBitmap
    }
}
