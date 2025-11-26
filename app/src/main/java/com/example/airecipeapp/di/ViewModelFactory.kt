package com.example.airecipeapp.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.airecipeapp.data.repository.ModelRepository
import com.example.airecipeapp.data.repository.ScanRepository
import com.example.airecipeapp.domain.ml.ModelDownloadManager
import com.example.airecipeapp.domain.usecase.GenerateAIRecipesUseCase
import com.example.airecipeapp.domain.usecase.MatchRecipesUseCase
import com.example.airecipeapp.domain.usecase.ProcessImageUseCase
import com.example.airecipeapp.ui.screens.camera.CameraViewModel
import com.example.airecipeapp.ui.screens.editor.EditorViewModel
import com.example.airecipeapp.ui.screens.history.HistoryViewModel
import com.example.airecipeapp.ui.screens.home.HomeViewModel
import com.example.airecipeapp.ui.screens.recipes.RecipeViewModel

class HomeViewModelFactory(
    private val scanRepository: ScanRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(scanRepository) as T
    }
}

class CameraViewModelFactory(
    private val processImageUseCase: ProcessImageUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CameraViewModel(processImageUseCase) as T
    }
}

class EditorViewModelFactory(
    private val scanRepository: ScanRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return EditorViewModel(scanRepository) as T
    }
}

class RecipeViewModelFactory(
    private val scanRepository: ScanRepository,
    private val matchRecipesUseCase: MatchRecipesUseCase,
    private val modelRepository: ModelRepository,
    private val modelDownloadManager: ModelDownloadManager,
    private val generateAIRecipesUseCase: GenerateAIRecipesUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return RecipeViewModel(
            scanRepository,
            matchRecipesUseCase,
            modelRepository,
            modelDownloadManager,
            generateAIRecipesUseCase
        ) as T
    }
}

class HistoryViewModelFactory(
    private val scanRepository: ScanRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HistoryViewModel(scanRepository) as T
    }
}
