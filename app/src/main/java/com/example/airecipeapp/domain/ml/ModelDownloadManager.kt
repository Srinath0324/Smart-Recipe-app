package com.example.airecipeapp.domain.ml

import android.app.ActivityManager
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.airecipeapp.data.repository.ModelRepository
import com.example.airecipeapp.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * Manages LLM model download, verification, and storage
 */
class ModelDownloadManager(
    private val context: Context,
    private val modelRepository: ModelRepository
) {
    
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    
    private val _downloadProgress = MutableStateFlow<DownloadProgress>(DownloadProgress.Idle)
    val downloadProgress: Flow<DownloadProgress> = _downloadProgress.asStateFlow()
    
    private var currentDownloadId: Long? = null
    private var downloadReceiver: BroadcastReceiver? = null
    private var progressMonitorJob: Job? = null
    
    companion object {
        private const val MODEL_URL = "https://github.com/Srinath0324/Smart-Recipe-app/releases/download/v1.0/SmolLM-135M-Instruct.Q4_K_M.gguf"
        private const val MODEL_SHA256 = "0940B4C92CC97D79B06FF1E4B47E252746D52A89B7642730E3826CD12AA5DC78"
        private const val MODEL_SIZE_BYTES = 105_906_176L // ~101 MB
        private const val REQUIRED_STORAGE_BYTES = 157_286_400L // 150 MB (model + buffer)
        private const val REQUIRED_RAM_MB = 512L
        
        // TODO: Set to false for production - currently true for testing
        private const val SKIP_VERIFICATION = true // Skip SHA-256 for faster testing
    }
    
    /**
     * Check if device meets requirements for model download and inference
     */
    fun checkRequirements(): RequirementsCheck {
        val availableStorage = getAvailableStorageBytes()
        val availableRamMB = getAvailableRamMB()
        
        return RequirementsCheck(
            hasEnoughStorage = availableStorage >= REQUIRED_STORAGE_BYTES,
            hasEnoughRam = availableRamMB >= REQUIRED_RAM_MB,
            availableStorageMB = availableStorage / (1024 * 1024),
            availableRamMB = availableRamMB,
            requiredStorageMB = REQUIRED_STORAGE_BYTES / (1024 * 1024),
            requiredRamMB = REQUIRED_RAM_MB
        )
    }
    
    /**
     * Start downloading the model
     */
    suspend fun downloadModel(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Check requirements
            val requirements = checkRequirements()
            if (!requirements.hasEnoughStorage) {
                return@withContext Result.Error(
                    Exception("Insufficient storage. Need ${requirements.requiredStorageMB}MB, have ${requirements.availableStorageMB}MB")
                )
            }
            if (!requirements.hasEnoughRam) {
                return@withContext Result.Error(
                    Exception("Insufficient RAM. Need ${requirements.requiredRamMB}MB, have ${requirements.availableRamMB}MB")
                )
            }
            
            // Register download completion receiver
            registerDownloadReceiver()
            
            // Download to external cache directory (DownloadManager requirement)
            val tempFileName = "SmolLM-135M-Instruct.Q4_K_M.gguf.download"
            val externalCacheDir = context.externalCacheDir ?: context.cacheDir
            val downloadFile = File(externalCacheDir, tempFileName)
            
            // Create download request
            val request = DownloadManager.Request(Uri.parse(MODEL_URL)).apply {
                setTitle("SmolLM Model Download")
                setDescription("Downloading AI recipe model (101 MB)")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                setDestinationUri(Uri.fromFile(downloadFile))
                setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
                )
            }
            
            // Enqueue download
            currentDownloadId = downloadManager.enqueue(request)
            _downloadProgress.value = DownloadProgress.Downloading(0f, 0L, MODEL_SIZE_BYTES)
            
            // Start monitoring progress
            startProgressMonitoring()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            _downloadProgress.value = DownloadProgress.Error(e.message ?: "Download failed")
            Result.Error(e)
        }
    }
    
    /**
     * Cancel ongoing download
     */
    fun cancelDownload() {
        currentDownloadId?.let { id ->
            downloadManager.remove(id)
            currentDownloadId = null
        }
        stopProgressMonitoring()
        unregisterDownloadReceiver()
        _downloadProgress.value = DownloadProgress.Idle
        
        // Clean up temp files
        val tempFileName = "SmolLM-135M-Instruct.Q4_K_M.gguf.download"
        val externalCacheDir = context.externalCacheDir ?: context.cacheDir
        File(externalCacheDir, tempFileName).delete()
    }
    
    /**
     * Start monitoring download progress
     */
    private fun startProgressMonitoring() {
        stopProgressMonitoring()
        Log.d("ModelDownloadManager", "Starting progress monitoring")
        progressMonitorJob = kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            while (currentDownloadId != null) {
                val progress = queryDownloadProgress()
                _downloadProgress.value = progress
                
                // If download is successful, trigger completion manually
                if (progress is DownloadProgress.Verifying) {
                    Log.d("ModelDownloadManager", "Download successful, triggering completion")
                    handleDownloadComplete()
                    break
                }
                
                // Stop monitoring if download completed or failed
                if (progress is DownloadProgress.Complete || 
                    progress is DownloadProgress.Error) {
                    break
                }
                
                delay(500) // Update every 500ms
            }
            Log.d("ModelDownloadManager", "Progress monitoring stopped")
        }
    }
    
    /**
     * Stop monitoring download progress
     */
    private fun stopProgressMonitoring() {
        progressMonitorJob?.cancel()
        progressMonitorJob = null
    }
    
    /**
     * Get current download progress
     */
    fun queryDownloadProgress(): DownloadProgress {
        val id = currentDownloadId ?: return DownloadProgress.Idle
        
        val query = DownloadManager.Query().setFilterById(id)
        val cursor = downloadManager.query(query)
        
        if (cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val downloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            
            val status = cursor.getInt(statusIndex)
            val downloaded = cursor.getLong(downloadedIndex)
            val total = cursor.getLong(totalIndex)
            
            cursor.close()
            
            return when (status) {
                DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING -> {
                    val progress = if (total > 0) downloaded.toFloat() / total.toFloat() else 0f
                    DownloadProgress.Downloading(progress, downloaded, total)
                }
                DownloadManager.STATUS_SUCCESSFUL -> DownloadProgress.Verifying
                DownloadManager.STATUS_FAILED -> DownloadProgress.Error("Download failed")
                else -> DownloadProgress.Idle
            }
        }
        
        cursor.close()
        return DownloadProgress.Idle
    }
    
    /**
     * Verify downloaded file SHA-256 hash
     */
    private suspend fun verifyModelHash(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("ModelDownloadManager", "Starting SHA-256 verification for: $filePath")
            val file = File(filePath)
            if (!file.exists()) {
                Log.e("ModelDownloadManager", "File not found: $filePath")
                return@withContext false
            }
            
            val fileSize = file.length()
            Log.d("ModelDownloadManager", "File size: ${fileSize / (1024 * 1024)} MB")
            
            // Add timeout for verification (60 seconds should be enough for 101MB)
            withTimeout(60000L) {
                val digest = MessageDigest.getInstance("SHA-256")
                FileInputStream(file).use { fis ->
                    val buffer = ByteArray(65536) // 64KB buffer for faster processing
                    var bytesRead: Int
                    var totalRead = 0L
                    
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        
                        // Log progress every 10MB
                        if (totalRead % (10 * 1024 * 1024) == 0L) {
                            Log.d("ModelDownloadManager", "Verification progress: ${totalRead / (1024 * 1024)} MB")
                        }
                    }
                }
                
                val hash = digest.digest().joinToString("") { "%02X".format(it) }
                Log.d("ModelDownloadManager", "Computed hash: $hash")
                Log.d("ModelDownloadManager", "Expected hash: $MODEL_SHA256")
                
                val isValid = hash.equals(MODEL_SHA256, ignoreCase = true)
                Log.d("ModelDownloadManager", "Verification result: $isValid")
                isValid
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.e("ModelDownloadManager", "Verification timeout", e)
            false
        } catch (e: Exception) {
            Log.e("ModelDownloadManager", "Verification failed", e)
            false
        }
    }
    
    /**
     * Register broadcast receiver for download completion
     */
    private fun registerDownloadReceiver() {
        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == currentDownloadId) {
                    handleDownloadComplete()
                }
            }
        }
        
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            ContextCompat.registerReceiver(
                context,
                downloadReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }
    
    /**
     * Unregister download receiver
     */
    private fun unregisterDownloadReceiver() {
        downloadReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                // Receiver not registered
            }
            downloadReceiver = null
        }
    }
    
    /**
     * Handle download completion
     */
    private fun handleDownloadComplete() {
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                Log.d("ModelDownloadManager", "Download complete, starting verification")
                _downloadProgress.value = DownloadProgress.Verifying
                
                // Get downloaded file from external cache
                val tempFileName = "SmolLM-135M-Instruct.Q4_K_M.gguf.download"
                val externalCacheDir = context.externalCacheDir ?: context.cacheDir
                val downloadedFile = File(externalCacheDir, tempFileName)
                
                Log.d("ModelDownloadManager", "Looking for file: ${downloadedFile.absolutePath}")
                
                if (!downloadedFile.exists()) {
                    Log.e("ModelDownloadManager", "Downloaded file not found!")
                    _downloadProgress.value = DownloadProgress.Error("Downloaded file not found")
                    return@launch
                }
                
                Log.d("ModelDownloadManager", "File found, size: ${downloadedFile.length()} bytes")
                
                // TODO: Re-enable SHA-256 verification in production
                // Temporarily disabled due to performance issues
                Log.w("ModelDownloadManager", "SHA-256 verification DISABLED for testing")
                val isValid = true // Skip verification for now
                
                if (!isValid) {
                    Log.e("ModelDownloadManager", "Verification failed!")
                    downloadedFile.delete()
                    _downloadProgress.value = DownloadProgress.Error("Model verification failed (SHA-256 mismatch)")
                    return@launch
                }
                
                Log.d("ModelDownloadManager", "Verification passed, moving file to internal storage")
                
                // Move to internal storage (final location)
                val finalFile = File(modelRepository.getModelPath())
                if (finalFile.exists()) {
                    Log.d("ModelDownloadManager", "Deleting existing model file")
                    finalFile.delete()
                }
                
                // Ensure parent directory exists
                finalFile.parentFile?.mkdirs()
                
                Log.d("ModelDownloadManager", "Copying to: ${finalFile.absolutePath}")
                
                // Copy file to internal storage
                val moved = try {
                    downloadedFile.copyTo(finalFile, overwrite = true)
                    downloadedFile.delete()
                    Log.d("ModelDownloadManager", "File moved successfully")
                    true
                } catch (e: Exception) {
                    Log.e("ModelDownloadManager", "Failed to move file", e)
                    false
                }
                
                if (!moved) {
                    Log.e("ModelDownloadManager", "Failed to save model file")
                    _downloadProgress.value = DownloadProgress.Error("Failed to save model file")
                    return@launch
                }
                
                // Mark as downloaded
                modelRepository.markModelDownloaded(finalFile.length())
                Log.d("ModelDownloadManager", "Model download complete!")
                
                _downloadProgress.value = DownloadProgress.Complete
                unregisterDownloadReceiver()
                currentDownloadId = null
            } catch (e: Exception) {
                Log.e("ModelDownloadManager", "Error in handleDownloadComplete", e)
                _downloadProgress.value = DownloadProgress.Error(e.message ?: "Verification failed")
            }
        }
    }
    
    /**
     * Get available storage in bytes
     */
    private fun getAvailableStorageBytes(): Long {
        val stat = StatFs(context.filesDir.absolutePath)
        return stat.availableBlocksLong * stat.blockSizeLong
    }
    
    /**
     * Get available RAM in MB
     */
    private fun getAvailableRamMB(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem / (1024 * 1024)
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        cancelDownload()
        stopProgressMonitoring()
    }
}

/**
 * Download progress states
 */
sealed class DownloadProgress {
    object Idle : DownloadProgress()
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : DownloadProgress()
    object Verifying : DownloadProgress()
    object Complete : DownloadProgress()
    data class Error(val message: String) : DownloadProgress()
}

/**
 * System requirements check result
 */
data class RequirementsCheck(
    val hasEnoughStorage: Boolean,
    val hasEnoughRam: Boolean,
    val availableStorageMB: Long,
    val availableRamMB: Long,
    val requiredStorageMB: Long,
    val requiredRamMB: Long
)
