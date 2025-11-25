package com.example.airecipeapp.data.repository

import android.content.Context
import com.example.airecipeapp.data.models.Recipe
import com.example.airecipeapp.utils.Result
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import java.io.IOException

class RecipeRepository(private val context: Context) {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    private var cachedRecipes: List<Recipe>? = null
    
    /**
     * Load recipes from JSON file in assets
     */
    suspend fun loadRecipes(): Result<List<Recipe>> {
        return try {
            // Return cached recipes if available
            cachedRecipes?.let {
                return Result.Success(it)
            }
            
            // Load from assets
            val jsonString = context.assets.open("recipes.json")
                .bufferedReader()
                .use { it.readText() }
            
            val recipes = json.decodeFromString(
                ListSerializer(Recipe.serializer()),
                jsonString
            )
            
            cachedRecipes = recipes
            Result.Success(recipes)
        } catch (e: IOException) {
            Result.Error(e, "Failed to load recipes from assets")
        } catch (e: Exception) {
            Result.Error(e, "Failed to parse recipes")
        }
    }
    
    /**
     * Get recipe by ID
     */
    suspend fun getRecipeById(recipeId: String): Result<Recipe?> {
        return when (val result = loadRecipes()) {
            is Result.Success -> {
                val recipe = result.data.find { it.id == recipeId }
                Result.Success(recipe)
            }
            is Result.Error -> result
            else -> Result.Error(Exception("Unexpected state"))
        }
    }
    
    /**
     * Search recipes by category
     */
    suspend fun getRecipesByCategory(category: String): Result<List<Recipe>> {
        return when (val result = loadRecipes()) {
            is Result.Success -> {
                val filtered = result.data.filter { 
                    it.category.equals(category, ignoreCase = true) 
                }
                Result.Success(filtered)
            }
            is Result.Error -> result
            else -> Result.Error(Exception("Unexpected state"))
        }
    }
    
    /**
     * Search recipes by tags
     */
    suspend fun getRecipesByTag(tag: String): Result<List<Recipe>> {
        return when (val result = loadRecipes()) {
            is Result.Success -> {
                val filtered = result.data.filter { recipe ->
                    recipe.tags.any { it.equals(tag, ignoreCase = true) }
                }
                Result.Success(filtered)
            }
            is Result.Error -> result
            else -> Result.Error(Exception("Unexpected state"))
        }
    }
}
