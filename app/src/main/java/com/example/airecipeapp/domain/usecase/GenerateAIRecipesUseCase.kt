package com.example.airecipeapp.domain.usecase

import com.example.airecipeapp.data.models.AIRecipe
import com.example.airecipeapp.data.models.Ingredient
import com.example.airecipeapp.domain.ml.LLMInferenceManager
import com.example.airecipeapp.utils.Result

/**
 * Use case for generating AI recipe suggestions using on-device LLM
 */
class GenerateAIRecipesUseCase(
    private val llmInferenceManager: LLMInferenceManager
) {
    
    /**
     * Generate AI recipe suggestions based on available ingredients
     */
    suspend fun generateRecipes(ingredients: List<Ingredient>): Result<List<AIRecipe>> {
        return try {
            if (ingredients.isEmpty()) {
                return Result.Error(Exception("No ingredients provided"))
            }
            
            // Extract ingredient names
            val ingredientNames = ingredients.map { it.name }
            
            // Generate recipes using LLM
            val recipes = llmInferenceManager.generateRecipes(ingredientNames)
            
            if (recipes.isEmpty()) {
                Result.Error(Exception("Failed to generate recipes. Please try again."))
            } else {
                Result.Success(recipes)
            }
        } catch (e: Exception) {
            Result.Error(Exception("AI recipe generation failed: ${e.message}"))
        }
    }
    
    /**
     * Check if the LLM model is loaded and ready
     */
    fun isModelReady(): Boolean {
        return llmInferenceManager.isLoaded()
    }
    
    /**
     * Load the model into memory
     */
    suspend fun loadModel(): Result<Unit> {
        return try {
            val loaded = llmInferenceManager.loadModel()
            if (loaded) {
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Failed to load AI model"))
            }
        } catch (e: Exception) {
            Result.Error(Exception("Model loading failed: ${e.message}"))
        }
    }
    
    /**
     * Unload the model from memory
     */
    fun unloadModel() {
        llmInferenceManager.unloadModel()
    }
}
