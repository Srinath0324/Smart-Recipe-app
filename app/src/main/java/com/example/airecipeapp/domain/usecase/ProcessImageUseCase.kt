package com.example.airecipeapp.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import com.example.airecipeapp.data.models.ScanResult
import com.example.airecipeapp.data.repository.ScanRepository
import com.example.airecipeapp.domain.ml.ImagePreprocessor
import com.example.airecipeapp.domain.ml.TextRecognizer
import com.example.airecipeapp.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Process image and extract ingredients using optimized OCR pipeline
 */
class ProcessImageUseCase(
    private val context: Context,
    private val scanRepository: ScanRepository
) {
    
    private val imagePreprocessor = ImagePreprocessor(context)
    private val textRecognizer = TextRecognizer(context)
    private val parseIngredientsUseCase = ParseIngredientsUseCase()
    
    /**
     * Process image: preprocess -> OCR -> parse -> save
     * Optimized for speed and accuracy
     */
    suspend fun processImage(
        originalBitmap: Bitmap,
        saveToHistory: Boolean = true
    ): Result<ScanResult> = withContext(Dispatchers.Default) {
        try {
            // Step 1: Preprocess image (fast ColorMatrix operations)
            val preprocessedBitmap = imagePreprocessor.preprocessImage(originalBitmap)
            
            // Step 2: Perform OCR with structured text blocks
            val recognitionResult = textRecognizer.recognizeText(preprocessedBitmap)
            
            if (!recognitionResult.success) {
                return@withContext Result.Error(
                    Exception(recognitionResult.error ?: "OCR failed"),
                    "Failed to recognize text from image"
                )
            }
            
            // Step 3: Parse ingredients from structured text blocks
            val ingredients = parseIngredientsUseCase.parse(recognitionResult)
            
            if (ingredients.isEmpty()) {
                return@withContext Result.Error(
                    Exception("No ingredients found"),
                    "Could not extract any ingredients from the image. Please try again with a clearer image."
                )
            }
            
            // Step 4: Save images
            val originalUri = saveBitmapToFile(originalBitmap, "original")
            val processedUri = saveBitmapToFile(preprocessedBitmap, "processed")
            
            // Step 5: Create scan result
            val scanResult = ScanResult(
                timestamp = System.currentTimeMillis(),
                rawText = recognitionResult.text,
                ingredients = ingredients,
                imageUri = originalUri,
                processedImageUri = processedUri
            )
            
            // Step 6: Save to database if requested
            if (saveToHistory) {
                when (val saveResult = scanRepository.saveScan(scanResult)) {
                    is Result.Success<*> -> {
                        Result.Success(scanResult.copy(id = saveResult.data as Long))
                    }
                    is Result.Error -> saveResult
                    else -> Result.Error(Exception("Failed to save scan"))
                }
            } else {
                Result.Success(scanResult)
            }
        } catch (e: Exception) {
            Result.Error(e, "Failed to process image: ${e.message}")
        }
    }
    
    /**
     * Save bitmap to internal storage
     */
    private suspend fun saveBitmapToFile(bitmap: Bitmap, prefix: String): String = withContext(Dispatchers.IO) {
        val imagesDir = File(context.filesDir, "images")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        
        val filename = "${prefix}_${System.currentTimeMillis()}.jpg"
        val file = File(imagesDir, filename)
        
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        
        file.absolutePath
    }
}
