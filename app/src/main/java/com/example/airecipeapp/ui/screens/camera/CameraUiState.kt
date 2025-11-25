package com.example.airecipeapp.ui.screens.camera

import com.example.airecipeapp.data.models.ScanResult

sealed class CameraUiState {
    object Idle : CameraUiState()
    object Processing : CameraUiState()
    data class Success(val scanResult: ScanResult) : CameraUiState()
    data class Error(val message: String) : CameraUiState()
}
