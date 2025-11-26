package com.example.airecipeapp

import android.app.Application
import com.example.airecipeapp.data.local.database.AppDatabase
import com.example.airecipeapp.data.repository.ModelRepository
import com.example.airecipeapp.domain.ml.LLMInferenceManager
import com.example.airecipeapp.domain.ml.ModelDownloadManager
import com.example.airecipeapp.domain.usecase.GenerateAIRecipesUseCase

class RecipeApplication : Application() {
    
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }
    
    val modelRepository: ModelRepository by lazy {
        ModelRepository(this)
    }
    
    val modelDownloadManager: ModelDownloadManager by lazy {
        ModelDownloadManager(this, modelRepository)
    }
    
    val llmInferenceManager: LLMInferenceManager by lazy {
        LLMInferenceManager(this, modelRepository)
    }
    
    val generateAIRecipesUseCase: GenerateAIRecipesUseCase by lazy {
        GenerateAIRecipesUseCase(llmInferenceManager)
    }
    
    override fun onCreate() {
        super.onCreate()
    }
}
