package com.example.airecipeapp.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Camera : Screen("camera")
    object Editor : Screen("editor/{scanId}") {
        fun createRoute(scanId: Long) = "editor/$scanId"
    }
    object Recipes : Screen("recipes/{scanId}") {
        fun createRoute(scanId: Long) = "recipes/$scanId"
    }
    object RecipeDetail : Screen("recipe_detail/{recipeId}") {
        fun createRoute(recipeId: String) = "recipe_detail/$recipeId"
    }
    object History : Screen("history")
}
