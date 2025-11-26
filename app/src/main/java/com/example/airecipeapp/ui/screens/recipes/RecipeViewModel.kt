package com.example.airecipeapp.ui.screens.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airecipeapp.data.models.AIRecipe
import com.example.airecipeapp.data.models.Ingredient
import com.example.airecipeapp.data.models.RecipeMatch
import com.example.airecipeapp.data.repository.ModelRepository
import com.example.airecipeapp.data.repository.ScanRepository
import com.example.airecipeapp.domain.ml.DownloadProgress
import com.example.airecipeapp.domain.ml.ModelDownloadManager
import com.example.airecipeapp.domain.ml.RequirementsCheck
import com.example.airecipeapp.domain.usecase.GenerateAIRecipesUseCase
import com.example.airecipeapp.domain.usecase.MatchRecipesUseCase
import com.example.airecipeapp.utils.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecipeUiState(
    val ingredients: List<Ingredient> = emptyList(),
    val recipeMatches: List<RecipeMatch> = emptyList(),
    val aiRecipes: List<AIRecipe> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAIRecipes: Boolean = false,
    val isModelDownloaded: Boolean = false,
    val showModelDialog: Boolean = false,
    val downloadProgress: DownloadProgress = DownloadProgress.Idle,
    val requirementsCheck: RequirementsCheck? = null,
    val isGeneratingAI: Boolean = false
)

class RecipeViewModel(
    private val scanRepository: ScanRepository,
    private val matchRecipesUseCase: MatchRecipesUseCase,
    private val modelRepository: ModelRepository,
    private val modelDownloadManager: ModelDownloadManager,
    private val generateAIRecipesUseCase: GenerateAIRecipesUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(RecipeUiState())
    val uiState: StateFlow<RecipeUiState> = _uiState.asStateFlow()
    
    init {
        // Check if model is downloaded
        _uiState.value = _uiState.value.copy(
            isModelDownloaded = modelRepository.isModelDownloaded()
        )
        
        // Observe download progress
        viewModelScope.launch {
            modelDownloadManager.downloadProgress.collect { progress ->
                _uiState.value = _uiState.value.copy(downloadProgress = progress)
                
                // Update model downloaded status when complete
                if (progress is DownloadProgress.Complete) {
                    _uiState.value = _uiState.value.copy(
                        isModelDownloaded = true,
                        showModelDialog = false
                    )
                }
            }
        }
    }
    
    fun loadScanAndMatchRecipes(scanId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            when (val scanResult = scanRepository.getScanById(scanId)) {
                is Result.Success -> {
                    val scan = scanResult.data
                    if (scan != null) {
                        val ingredients = scan.ingredients
                        _uiState.value = _uiState.value.copy(ingredients = ingredients)
                        
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
        val newShowAI = !_uiState.value.showAIRecipes
        
        if (newShowAI) {
            // Switching to AI recipes
            if (!modelRepository.isModelDownloaded()) {
                // Show download dialog
                _uiState.value = _uiState.value.copy(
                    showModelDialog = true,
                    requirementsCheck = modelDownloadManager.checkRequirements()
                )
            } else {
                // Generate AI recipes
                _uiState.value = _uiState.value.copy(showAIRecipes = true)
                if (_uiState.value.aiRecipes.isEmpty()) {
                    generateAIRecipes()
                }
            }
        } else {
            // Switching to recipe matches
            _uiState.value = _uiState.value.copy(showAIRecipes = false)
        }
    }
    
    fun showModelDialog() {
        _uiState.value = _uiState.value.copy(
            showModelDialog = true,
            requirementsCheck = modelDownloadManager.checkRequirements()
        )
    }
    
    fun hideModelDialog() {
        _uiState.value = _uiState.value.copy(showModelDialog = false)
    }
    
    fun downloadModel() {
        viewModelScope.launch {
            modelDownloadManager.downloadModel()
        }
    }
    
    fun deleteModel() {
        viewModelScope.launch {
            modelRepository.deleteModel()
            _uiState.value = _uiState.value.copy(
                isModelDownloaded = false,
                aiRecipes = emptyList(),
                showAIRecipes = false,
                showModelDialog = false
            )
            generateAIRecipesUseCase.unloadModel()
        }
    }
    
    private fun generateAIRecipes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingAI = true, error = null)
            
            when (val result = generateAIRecipesUseCase.generateRecipes(_uiState.value.ingredients)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        aiRecipes = result.data,
                        isGeneratingAI = false
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isGeneratingAI = false,
                        error = result.message
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isGeneratingAI = false)
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        modelDownloadManager.cleanup()
        generateAIRecipesUseCase.unloadModel()
    }
}
