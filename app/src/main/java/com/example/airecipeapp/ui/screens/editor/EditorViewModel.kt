package com.example.airecipeapp.ui.screens.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airecipeapp.data.models.Ingredient
import com.example.airecipeapp.data.repository.ScanRepository
import com.example.airecipeapp.utils.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditorUiState(
    val ingredients: List<Ingredient> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val scanId: Long = 0
)

class EditorViewModel(
    private val scanRepository: ScanRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()
    
    fun loadScan(scanId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, scanId = scanId)
            
            when (val result = scanRepository.getScanById(scanId)) {
                is Result.Success -> {
                    val scan = result.data
                    if (scan != null) {
                        _uiState.value = _uiState.value.copy(
                            ingredients = scan.ingredients,
                            isLoading = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Scan not found"
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                else -> {}
            }
        }
    }
    
    fun updateIngredient(index: Int, ingredient: Ingredient) {
        val updatedList = _uiState.value.ingredients.toMutableList()
        if (index in updatedList.indices) {
            updatedList[index] = ingredient
            _uiState.value = _uiState.value.copy(ingredients = updatedList)
        }
    }
    
    fun removeIngredient(index: Int) {
        val updatedList = _uiState.value.ingredients.toMutableList()
        if (index in updatedList.indices) {
            updatedList.removeAt(index)
            _uiState.value = _uiState.value.copy(ingredients = updatedList)
        }
    }
    
    fun addIngredient(ingredient: Ingredient) {
        val updatedList = _uiState.value.ingredients.toMutableList()
        updatedList.add(ingredient)
        _uiState.value = _uiState.value.copy(ingredients = updatedList)
    }
    
    fun saveIngredients(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val scanId = _uiState.value.scanId
            val ingredients = _uiState.value.ingredients
            
            when (scanRepository.updateIngredients(scanId, ingredients)) {
                is Result.Success -> {
                    onSuccess()
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to save ingredients"
                    )
                }
                else -> {}
            }
        }
    }
}
