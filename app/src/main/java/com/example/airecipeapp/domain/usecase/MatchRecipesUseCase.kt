package com.example.airecipeapp.domain.usecase

import com.example.airecipeapp.data.models.Ingredient
import com.example.airecipeapp.data.models.Recipe
import com.example.airecipeapp.data.models.RecipeMatch
import com.example.airecipeapp.data.repository.RecipeRepository
import com.example.airecipeapp.utils.Result
import java.util.Locale

/**
 * Match recipes based on available ingredients
 */
class MatchRecipesUseCase(private val recipeRepository: RecipeRepository) {
    
    /**
     * Find matching recipes for given ingredients
     */
    suspend fun matchRecipes(ingredients: List<Ingredient>): Result<List<RecipeMatch>> {
        return when (val recipesResult = recipeRepository.loadRecipes()) {
            is Result.Success -> {
                val recipes = recipesResult.data
                val matches = findMatches(ingredients, recipes)
                Result.Success(matches)
            }
            is Result.Error -> recipesResult
            else -> Result.Error(Exception("Unexpected state"))
        }
    }
    
    /**
     * Find recipe matches with scoring
     */
    private fun findMatches(
        availableIngredients: List<Ingredient>,
        allRecipes: List<Recipe>
    ): List<RecipeMatch> {
        val ingredientNames = availableIngredients.map { 
            it.name.lowercase(Locale.getDefault()) 
        }.toSet()
        
        val matches = allRecipes.mapNotNull { recipe ->
            val matchResult = calculateMatch(ingredientNames, recipe)
            if (matchResult.matchScore > 0.2f) { // Minimum threshold
                matchResult
            } else {
                null
            }
        }
        
        // Sort by match score descending
        return matches.sortedByDescending { it.matchScore }
    }
    
    /**
     * Calculate match score for a recipe
     */
    private fun calculateMatch(
        availableIngredients: Set<String>,
        recipe: Recipe
    ): RecipeMatch {
        val recipeIngredients = recipe.ingredients.map { 
            extractIngredientName(it).lowercase(Locale.getDefault()) 
        }
        
        val matched = mutableListOf<String>()
        val missing = mutableListOf<String>()
        
        for (recipeIngredient in recipeIngredients) {
            var found = false
            
            // Exact match
            if (availableIngredients.contains(recipeIngredient)) {
                matched.add(recipeIngredient)
                found = true
            } else {
                // Fuzzy match (check if ingredient name contains or is contained in available)
                for (available in availableIngredients) {
                    if (isSimilar(available, recipeIngredient)) {
                        matched.add(recipeIngredient)
                        found = true
                        break
                    }
                }
            }
            
            if (!found) {
                missing.add(recipeIngredient)
            }
        }
        
        // Calculate score
        val matchScore = if (recipeIngredients.isEmpty()) {
            0f
        } else {
            matched.size.toFloat() / recipeIngredients.size.toFloat()
        }
        
        return RecipeMatch(
            recipe = recipe,
            matchScore = matchScore,
            matchedIngredients = matched,
            missingIngredients = missing
        )
    }
    
    /**
     * Extract ingredient name from recipe ingredient string
     * e.g., "2 cups rice" -> "rice"
     */
    private fun extractIngredientName(ingredientString: String): String {
        // Remove quantities and common units
        val cleaned = ingredientString
            .replace("""^\d+\.?\d*\s*""".toRegex(), "") // Remove leading numbers
            .replace("""(cup|cups|tbsp|tsp|kg|g|ml|l|piece|pieces)s?\s*""".toRegex(RegexOption.IGNORE_CASE), "")
            .trim()
        
        // Take the first significant word
        val words = cleaned.split(" ")
        return words.firstOrNull { it.length > 2 } ?: cleaned
    }
    
    /**
     * Check if two ingredient names are similar
     */
    private fun isSimilar(name1: String, name2: String): Boolean {
        val n1 = name1.lowercase(Locale.getDefault())
        val n2 = name2.lowercase(Locale.getDefault())
        
        // Check if one contains the other
        if (n1.contains(n2) || n2.contains(n1)) {
            return true
        }
        
        // Check for plural forms
        if (n1 == "${n2}s" || n2 == "${n1}s") {
            return true
        }
        
        // Check Levenshtein distance for typos
        if (levenshteinDistance(n1, n2) <= 2) {
            return true
        }
        
        return false
    }
    
    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }
        
        for (i in 0..len1) {
            dp[i][0] = i
        }
        
        for (j in 0..len2) {
            dp[0][j] = j
        }
        
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[len1][len2]
    }
    
    /**
     * Generate quick recipe suggestions based on common patterns
     */
    fun generateQuickSuggestions(ingredients: List<Ingredient>): List<String> {
        val suggestions = mutableListOf<String>()
        val ingredientNames = ingredients.map { it.name.lowercase(Locale.getDefault()) }
        
        // Check for common combinations
        if (ingredientNames.any { it.contains("rice") }) {
            suggestions.add("Fried Rice with available vegetables")
        }
        
        if (ingredientNames.any { it.contains("egg") }) {
            suggestions.add("Scrambled Eggs or Omelette")
        }
        
        if (ingredientNames.any { it.contains("potato") }) {
            suggestions.add("Potato Curry or Mashed Potatoes")
        }
        
        if (ingredientNames.any { it.contains("tomato") } && 
            ingredientNames.any { it.contains("onion") }) {
            suggestions.add("Tomato-Onion Curry Base")
        }
        
        if (ingredientNames.any { it.contains("chicken") }) {
            suggestions.add("Chicken Stir Fry or Curry")
        }
        
        // Generic suggestions
        if (ingredientNames.size >= 3) {
            suggestions.add("Mixed Vegetable Stir Fry")
            suggestions.add("Soup with available ingredients")
        }
        
        return suggestions.take(5)
    }
}
