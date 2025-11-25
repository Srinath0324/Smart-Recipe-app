package com.example.airecipeapp.data.models

import kotlinx.serialization.Serializable

/**
 * Domain model representing a recipe
 */
@Serializable
data class Recipe(
    val id: String,
    val name: String,
    val description: String = "",
    val ingredients: List<String>,
    val instructions: List<String>,
    val prepTimeMinutes: Int,
    val servings: Int = 4,
    val difficulty: String, // "Easy", "Medium", "Hard"
    val category: String, // "Breakfast", "Lunch", "Dinner", "Snack", "Dessert"
    val tags: List<String> = emptyList(),
    val imageUrl: String? = null
)

/**
 * Recipe match result with score
 */
data class RecipeMatch(
    val recipe: Recipe,
    val matchScore: Float, // 0.0 to 1.0
    val matchedIngredients: List<String>,
    val missingIngredients: List<String>
)
