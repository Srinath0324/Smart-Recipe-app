package com.example.airecipeapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import java.io.File

/**
 * Repository for managing LLM model state and metadata
 */
class ModelRepository(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "model_prefs", 
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val MODEL_FILENAME = "SmolLM-135M-Instruct.Q4_K_M.gguf"
        private const val PREF_MODEL_DOWNLOADED = "model_downloaded"
        private const val PREF_MODEL_VERSION = "model_version"
        private const val PREF_MODEL_SIZE = "model_size"
        private const val PREF_DOWNLOAD_DATE = "download_date"
        private const val CURRENT_MODEL_VERSION = "1.0"
    }
    
    /**
     * Get the file path where the model should be stored
     */
    fun getModelPath(): String {
        return File(context.filesDir, MODEL_FILENAME).absolutePath
    }
    
    /**
     * Check if the model file exists locally
     */
    fun isModelDownloaded(): Boolean {
        val modelFile = File(getModelPath())
        val exists = modelFile.exists() && modelFile.length() > 0
        
        // Sync with preferences
        if (exists != prefs.getBoolean(PREF_MODEL_DOWNLOADED, false)) {
            prefs.edit().putBoolean(PREF_MODEL_DOWNLOADED, exists).apply()
        }
        
        return exists
    }
    
    /**
     * Get the model file size in bytes
     */
    fun getModelSize(): Long {
        val modelFile = File(getModelPath())
        return if (modelFile.exists()) modelFile.length() else 0L
    }
    
    /**
     * Mark model as downloaded and save metadata
     */
    fun markModelDownloaded(sizeBytes: Long) {
        prefs.edit().apply {
            putBoolean(PREF_MODEL_DOWNLOADED, true)
            putString(PREF_MODEL_VERSION, CURRENT_MODEL_VERSION)
            putLong(PREF_MODEL_SIZE, sizeBytes)
            putLong(PREF_DOWNLOAD_DATE, System.currentTimeMillis())
            apply()
        }
    }
    
    /**
     * Delete the model file and clear metadata
     */
    fun deleteModel(): Boolean {
        val modelFile = File(getModelPath())
        val deleted = if (modelFile.exists()) {
            modelFile.delete()
        } else {
            true
        }
        
        if (deleted) {
            prefs.edit().apply {
                remove(PREF_MODEL_DOWNLOADED)
                remove(PREF_MODEL_VERSION)
                remove(PREF_MODEL_SIZE)
                remove(PREF_DOWNLOAD_DATE)
                apply()
            }
        }
        
        return deleted
    }
    
    /**
     * Get model metadata
     */
    fun getModelMetadata(): ModelMetadata? {
        return if (isModelDownloaded()) {
            ModelMetadata(
                version = prefs.getString(PREF_MODEL_VERSION, CURRENT_MODEL_VERSION) ?: CURRENT_MODEL_VERSION,
                sizeBytes = prefs.getLong(PREF_MODEL_SIZE, 0L),
                downloadDate = prefs.getLong(PREF_DOWNLOAD_DATE, 0L)
            )
        } else {
            null
        }
    }
}

/**
 * Model metadata information
 */
data class ModelMetadata(
    val version: String,
    val sizeBytes: Long,
    val downloadDate: Long
)
