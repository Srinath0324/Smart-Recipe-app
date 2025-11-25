package com.example.airecipeapp.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airecipeapp.data.models.ScanResult
import com.example.airecipeapp.data.repository.ScanRepository
import com.example.airecipeapp.utils.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryUiState(
    val scans: List<ScanResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class HistoryViewModel(
    private val scanRepository: ScanRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    init {
        loadHistory()
    }
    
    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            when (val result = scanRepository.getAllScans()) {
                is Result.Success<*> -> {
                    _uiState.value = HistoryUiState(
                        scans = result.data as List<ScanResult>,
                        isLoading = false,
                        error = null
                    )
                }
                is Result.Error -> {
                    _uiState.value = HistoryUiState(
                        scans = emptyList(),
                        isLoading = false,
                        error = result.message ?: "Failed to load history"
                    )
                }
                else -> {
                    _uiState.value = HistoryUiState(
                        scans = emptyList(),
                        isLoading = false,
                        error = "Unexpected error"
                    )
                }
            }
        }
    }
    
    fun deleteScan(scanId: Long) {
        viewModelScope.launch {
            scanRepository.deleteScan(scanId)
            loadHistory()
        }
    }
}
