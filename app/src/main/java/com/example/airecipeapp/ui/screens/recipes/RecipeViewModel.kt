package com.example.airecipeapp.ui.screens.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airecipeapp.data.models.Ingredient
import com.example.airecipeapp.data.models.RecipeMatch
import com.example.airecipeapp.data.repository.ScanRepository
import com.example.airecipeapp.domain.usecase.MatchRecipesUseCase
import com.example.airecipeapp.utils.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecipeUiState(
    val ingredients: List<Ingredient> = emptyList(),
    val recipeMatches: List<RecipeMatch> = emptyList(),
    val quickSuggestions: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showQuickSuggestions: Boolean = true
)

class RecipeViewModel(
    private val scanRepository: ScanRepository,
    private val matchRecipesUseCase: MatchRecipesUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(RecipeUiState())
    val uiState: StateFlow<RecipeUiState> = _uiState.asStateFlow()
    
    fun loadScanAndMatchRecipes(scanId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            when (val scanResult = scanRepository.getScanById(scanId)) {
                is Result.Success -> {
                    val scan = scanResult.data
                    if (scan != null) {
                        val ingredients = scan.ingredients
                        _uiState.value = _uiState.value.copy(ingredients = ingredients)
                        
                        // Generate quick suggestions
                        val quickSuggestions = matchRecipesUseCase.generateQuickSuggestions(ingredients)
                        _uiState.value = _uiState.value.copy(quickSuggestions = quickSuggestions)
                        
                        // Match recipes
                        matchRecipes(ingredients)
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
                        error = scanResult.message
                    )
                }
                else -> {}
            }
        }
    }
    
    private fun matchRecipes(ingredients: List<Ingredient>) {
        viewModelScope.launch {
            when (val result = matchRecipesUseCase.matchRecipes(ingredients)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        recipeMatches = result.data,
                        isLoading = false
                    )
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
    
    fun toggleSuggestionMode() {
        _uiState.value = _uiState.value.copy(
            showQuickSuggestions = !_uiState.value.showQuickSuggestions
        )
    }
}
