package com.example.airecipeapp.domain.ml

import android.content.Context
import android.util.Log
import com.example.airecipeapp.data.models.AIRecipe
import com.example.airecipeapp.data.repository.ModelRepository
import com.example.airecipeapp.domain.ml.stub.LlamaHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Manages LLM inference using llama.cpp for recipe generation
 */
class LLMInferenceManager(
    private val context: Context,
    private val modelRepository: ModelRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var llamaHelper: LlamaHelper? = null
    private var isModelLoaded = false
    
    companion object {
        private const val TAG = "LLMInferenceManager"
        private const val CONTEXT_LENGTH = 2048
        private const val MAX_TOKENS = 800
        private const val TEMPERATURE = 0.7f
        private const val INFERENCE_TIMEOUT_MS = 60000L // 60 seconds
    }
    
    /**
     * Load the model into memory
     */
    suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isModelLoaded) return@withContext true
            
            if (!modelRepository.isModelDownloaded()) {
                Log.e(TAG, "Model not downloaded")
                return@withContext false
            }
            
            val modelPath = modelRepository.getModelPath()
            Log.d(TAG, "Loading model from: $modelPath")
            
            llamaHelper = LlamaHelper(scope)
            llamaHelper?.load(
                path = modelPath,
                contextLength = CONTEXT_LENGTH
            )
            
            isModelLoaded = true
            Log.d(TAG, "Model loaded successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
            isModelLoaded = false
            false
        }
    }
    
    /**
     * Generate recipe suggestions based on ingredients
     */
    suspend fun generateRecipes(ingredients: List<String>): List<AIRecipe> = withContext(Dispatchers.IO) {
        try {
            if (!isModelLoaded) {
                val loaded = loadModel()
                if (!loaded) {
                    Log.e(TAG, "Failed to load model for inference")
                    return@withContext emptyList()
                }
            }
            
            val prompt = buildPrompt(ingredients)
            Log.d(TAG, "Generating recipes with prompt: $prompt")
            
            val response = withTimeout(INFERENCE_TIMEOUT_MS) {
                generateText(prompt)
            }
            
            Log.d(TAG, "LLM Response: $response")
            parseRecipes(response)
        } catch (e: Exception) {
            Log.e(TAG, "Recipe generation failed", e)
            emptyList()
        }
    }
    
    /**
     * Build prompt for recipe generation
     */
    private fun buildPrompt(ingredients: List<String>): String {
        val ingredientList = ingredients.joinToString(", ")
        return """<|im_start|>system
You are a helpful cooking assistant. Generate creative and practical recipes using the given ingredients.<|im_end|>
<|im_start|>user
Create 2 simple recipes using these ingredients: $ingredientList

For each recipe, provide:
1. Recipe name
2. Ingredients needed (with quantities)
3. Step-by-step instructions
4. Cooking time in minutes
5. One helpful tip

Format each recipe clearly with headers.<|im_end|>
<|im_start|>assistant
"""
    }
    
    /**
     * Generate text using LlamaHelper
     */
    private suspend fun generateText(prompt: String): String = withContext(Dispatchers.IO) {
        val helper = llamaHelper ?: return@withContext ""
        
        val responseBuilder = StringBuilder()
        
        try {
            // Set up collector flow
            val flow = helper.setCollector()
            
            // Start prediction
            helper.predict(
                prompt = prompt,
                partialCompletion = true
            )
            // Collect results
            flow.collect { chunk ->
                responseBuilder.append(chunk)
            }
            
            helper.unsetCollector()
        } catch (e: Exception) {
            Log.e(TAG, "Text generation failed", e)
        }
        
        responseBuilder.toString()
    }
    
    /**
     * Parse LLM response into structured recipes
     */
    private fun parseRecipes(response: String): List<AIRecipe> {
        val recipes = mutableListOf<AIRecipe>()
        
        try {
            // Split by recipe markers or numbered recipes
            val recipeSections = response.split(Regex("(?i)(Recipe \\d+:|\\d+\\.|###\\s*Recipe)"))
                .filter { it.trim().isNotEmpty() }
            
            for (section in recipeSections) {
                val recipe = parseRecipeSection(section)
                if (recipe != null) {
                    recipes.add(recipe)
                }
            }
            
            // If parsing failed, create a simple fallback recipe
            if (recipes.isEmpty() && response.isNotBlank()) {
                recipes.add(createFallbackRecipe(response))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse recipes", e)
        }
        
        return recipes
    }
    
    /**
     * Parse a single recipe section
     */
    private fun parseRecipeSection(section: String): AIRecipe? {
        try {
            val lines = section.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) return null
            
            var title = "AI Generated Recipe"
            val ingredients = mutableListOf<String>()
            val instructions = mutableListOf<String>()
            val tips = mutableListOf<String>()
            var cookingTime: Int? = null
            
            var currentSection = ""
            
            for (line in lines) {
                when {
                    line.matches(Regex("(?i).*name.*:|.*title.*:")) -> {
                        title = line.substringAfter(":").trim()
                        currentSection = "title"
                    }
                    line.matches(Regex("(?i)ingredients?:")) -> {
                        currentSection = "ingredients"
                    }
                    line.matches(Regex("(?i)instructions?:|steps?:")) -> {
                        currentSection = "instructions"
                    }
                    line.matches(Regex("(?i)cooking time:|time:")) -> {
                        val timeStr = line.substringAfter(":").trim()
                        cookingTime = timeStr.replace(Regex("[^0-9]"), "").toIntOrNull()
                        currentSection = "time"
                    }
                    line.matches(Regex("(?i)tips?:")) -> {
                        currentSection = "tips"
                    }
                    else -> {
                        when (currentSection) {
                            "ingredients" -> if (line.isNotBlank()) ingredients.add(line.removePrefix("-").removePrefix("*").trim())
                            "instructions" -> if (line.isNotBlank()) instructions.add(line.removePrefix("-").removePrefix("*").replace(Regex("^\\d+\\."), "").trim())
                            "tips" -> if (line.isNotBlank()) tips.add(line.removePrefix("-").removePrefix("*").trim())
                            "" -> if (title == "AI Generated Recipe" && line.length > 5) title = line
                        }
                    }
                }
            }
            
            // Only return if we have at least title and some content
            if (ingredients.isNotEmpty() || instructions.isNotEmpty()) {
                return AIRecipe(
                    title = title,
                    ingredients = ingredients,
                    instructions = instructions,
                    cookingTimeMinutes = cookingTime,
                    tips = tips
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse recipe section", e)
        }
        
        return null
    }
    
    /**
     * Create a fallback recipe from raw response
     */
    private fun createFallbackRecipe(response: String): AIRecipe {
        val lines = response.lines().filter { it.trim().isNotEmpty() }
        return AIRecipe(
            title = "AI Recipe Suggestion",
            ingredients = emptyList(),
            instructions = lines.take(10),
            cookingTimeMinutes = null,
            tips = emptyList()
        )
    }
    
    /**
     * Unload model from memory
     */
    fun unloadModel() {
        try {
            llamaHelper?.abort()
            llamaHelper?.release()
            llamaHelper = null
            isModelLoaded = false
            Log.d(TAG, "Model unloaded")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unload model", e)
        }
    }
    
    /**
     * Check if model is currently loaded
     */
    fun isLoaded(): Boolean = isModelLoaded
}
