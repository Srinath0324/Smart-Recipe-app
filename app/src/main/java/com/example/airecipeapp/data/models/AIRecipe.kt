package com.example.airecipeapp.data.models


data class AIRecipe(
    val title: String,
    val ingredients: List<String>,
    val instructions: List<String>,
    val cookingTimeMinutes: Int? = null,
    val tips: List<String> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis()
)
