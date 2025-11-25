package com.example.airecipeapp.ui.screens.camera

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airecipeapp.data.models.ScanResult
import com.example.airecipeapp.domain.usecase.ProcessImageUseCase
import com.example.airecipeapp.utils.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class CameraViewModel(
    private val processImageUseCase: ProcessImageUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<CameraUiState>(CameraUiState.Idle)
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()
    
    private var processingJob: Job? = null
    
    fun processImage(bitmap: Bitmap) {
        // Cancel any ongoing processing
        processingJob?.cancel()
        
        processingJob = viewModelScope.launch {
            try {
                _uiState.value = CameraUiState.Processing
                
                when (val result = processImageUseCase.processImage(bitmap, saveToHistory = true)) {
                    is Result.Success<*> -> {
                        _uiState.value = CameraUiState.Success(result.data as ScanResult)
                    }
                    is Result.Error -> {
                        _uiState.value = CameraUiState.Error(
                            result.message ?: "Failed to process image"
                        )
                    }
                    else -> {
                        _uiState.value = CameraUiState.Error("Unexpected error occurred")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = CameraUiState.Error(
                    "Failed to process image: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }
    
    fun resetState() {
        // Cancel any ongoing processing
        processingJob?.cancel()
        processingJob = null
        _uiState.value = CameraUiState.Idle
    }
    
    override fun onCleared() {
        super.onCleared()
        processingJob?.cancel()
    }
}
