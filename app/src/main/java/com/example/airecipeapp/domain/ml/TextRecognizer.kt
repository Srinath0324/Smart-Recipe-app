package com.example.airecipeapp.domain.ml

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * Enhanced OCR Manager with structured text block extraction
 */
class TextRecognizer(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val imagePreprocessor = ImagePreprocessor(context)
    
    /**
     * Process image with preprocessing for better OCR results
     * Returns structured OCR result with text blocks
     */
    suspend fun recognizeText(bitmap: Bitmap): RecognitionResult {
        return try {
            // Preprocess image for better OCR
            val preprocessedBitmap = imagePreprocessor.preprocessImage(bitmap)
            
            // Use preprocessed image
            val image = InputImage.fromBitmap(preprocessedBitmap, 0)
            val result = recognizer.process(image).await()
            
            // Extract structured text blocks
            val textBlocks = result.textBlocks.map { block ->
                TextBlock(
                    text = block.text,
                    lines = block.lines.map { line ->
                        TextLine(text = line.text)
                    }
                )
            }
            
            RecognitionResult(
                text = result.text,
                blocks = textBlocks,
                success = true,
                error = null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            RecognitionResult(
                text = "",
                blocks = emptyList(),
                success = false,
                error = e.message
            )
        }
    }
    
    /**
     * Process image from URI with preprocessing
     */
    suspend fun recognizeTextFromUri(imageUri: Uri): RecognitionResult {
        return try {
            // Preprocess image for better OCR
            val preprocessedBitmap = imagePreprocessor.preprocessImageFromUri(imageUri)
            
            if (preprocessedBitmap == null) {
                // Fallback to original image if preprocessing fails
                val image = InputImage.fromFilePath(context, imageUri)
                val result = recognizer.process(image).await()
                return RecognitionResult(
                    text = result.text,
                    blocks = result.textBlocks.map { block ->
                        TextBlock(
                            text = block.text,
                            lines = block.lines.map { TextLine(it.text) }
                        )
                    },
                    success = true,
                    error = null
                )
            }
            
            // Use preprocessed image
            recognizeText(preprocessedBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            RecognitionResult(
                text = "",
                blocks = emptyList(),
                success = false,
                error = e.message
            )
        }
    }
}

/**
 * Result of text recognition with structured blocks
 */
data class RecognitionResult(
    val text: String,
    val blocks: List<TextBlock>,
    val success: Boolean,
    val error: String?
)

/**
 * Text block from recognition
 */
data class TextBlock(
    val text: String,
    val lines: List<TextLine>
)

/**
 * Text line from recognition
 */
data class TextLine(
    val text: String
)
