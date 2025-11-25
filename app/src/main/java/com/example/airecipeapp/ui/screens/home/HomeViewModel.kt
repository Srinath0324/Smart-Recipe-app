package com.example.airecipeapp.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airecipeapp.data.models.ScanResult
import com.example.airecipeapp.data.repository.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val recentScans: List<ScanResult> = emptyList(),
    val totalScans: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel(
    private val scanRepository: ScanRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadRecentScans()
        loadScanCount()
    }
    
    private fun loadRecentScans() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            scanRepository.getRecentScans(5).collect { scans ->
                _uiState.value = _uiState.value.copy(
                    recentScans = scans,
                    isLoading = false
                )
            }
        }
    }
    
    private fun loadScanCount() {
        viewModelScope.launch {
            scanRepository.getScanCount().collect { count ->
                _uiState.value = _uiState.value.copy(totalScans = count)
            }
        }
    }
    
    fun deleteScan(scanId: Long) {
        viewModelScope.launch {
            scanRepository.deleteScan(scanId)
        }
    }
}
