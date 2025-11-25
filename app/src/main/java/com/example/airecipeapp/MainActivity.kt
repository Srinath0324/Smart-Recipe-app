package com.example.airecipeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.airecipeapp.data.repository.RecipeRepository
import com.example.airecipeapp.data.repository.ScanRepository
import com.example.airecipeapp.di.*
import com.example.airecipeapp.domain.usecase.MatchRecipesUseCase
import com.example.airecipeapp.domain.usecase.ProcessImageUseCase
import com.example.airecipeapp.ui.navigation.Screen
import com.example.airecipeapp.ui.screens.camera.CameraPermissionScreen
import com.example.airecipeapp.ui.screens.camera.CameraViewModel
import com.example.airecipeapp.ui.screens.editor.EditorViewModel
import com.example.airecipeapp.ui.screens.editor.IngredientEditorScreen
import com.example.airecipeapp.ui.screens.history.HistoryScreen
import com.example.airecipeapp.ui.screens.history.HistoryViewModel
import com.example.airecipeapp.ui.screens.home.HomeScreen
import com.example.airecipeapp.ui.screens.home.HomeViewModel
import com.example.airecipeapp.ui.screens.recipedetail.RecipeDetailScreen
import com.example.airecipeapp.ui.screens.recipes.RecipeViewModel
import com.example.airecipeapp.ui.screens.recipes.RecipeResultsScreen
import com.example.airecipeapp.ui.theme.AIRecipeAppTheme

class MainActivity : ComponentActivity() {
    
    private lateinit var scanRepository: ScanRepository
    private lateinit var recipeRepository: RecipeRepository
    private lateinit var processImageUseCase: ProcessImageUseCase
    private lateinit var matchRecipesUseCase: MatchRecipesUseCase
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize repositories
        val app = application as RecipeApplication
        val database = app.database
        scanRepository = ScanRepository(database.scanHistoryDao(), database.ingredientDao())
        recipeRepository = RecipeRepository(this)
        processImageUseCase = ProcessImageUseCase(this, scanRepository)
        matchRecipesUseCase = MatchRecipesUseCase(recipeRepository)
        
        setContent {
            AIRecipeAppTheme {
                MainApp(
                    scanRepository = scanRepository,
                    recipeRepository = recipeRepository,
                    processImageUseCase = processImageUseCase,
                    matchRecipesUseCase = matchRecipesUseCase
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    scanRepository: ScanRepository,
    recipeRepository: RecipeRepository,
    processImageUseCase: ProcessImageUseCase,
    matchRecipesUseCase: MatchRecipesUseCase
) {
    val navController = rememberNavController()
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Smart Recipe App") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                val viewModel: HomeViewModel = viewModel(
                    factory = HomeViewModelFactory(scanRepository)
                )
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToCamera = {
                        navController.navigate(Screen.Camera.route)
                    },
                    onNavigateToHistory = {
                        navController.navigate(Screen.History.route)
                    }
                )
            }
            
            composable(Screen.Camera.route) {
                val viewModel: CameraViewModel = viewModel(
                    factory = CameraViewModelFactory(processImageUseCase)
                )
                CameraPermissionScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToEditor = { scanId ->
                        navController.navigate(Screen.Editor.createRoute(scanId))
                    }
                )
            }
            
            composable(
                route = Screen.Editor.route,
                arguments = listOf(navArgument("scanId") { type = NavType.LongType })
            ) { backStackEntry ->
                val scanId = backStackEntry.arguments?.getLong("scanId") ?: 0L
                val viewModel: EditorViewModel = viewModel(
                    factory = EditorViewModelFactory(scanRepository)
                )
                IngredientEditorScreen(
                    viewModel = viewModel,
                    scanId = scanId,
                    onNavigateBack = {
                        // Navigate to home instead of going back to camera
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToRecipes = { id ->
                        navController.navigate(Screen.Recipes.createRoute(id))
                    }
                )
            }
            
            composable(
                route = Screen.Recipes.route,
                arguments = listOf(navArgument("scanId") { type = NavType.LongType })
            ) { backStackEntry ->
                val scanId = backStackEntry.arguments?.getLong("scanId") ?: 0L
                val viewModel: RecipeViewModel = viewModel(
                    factory = RecipeViewModelFactory(scanRepository, matchRecipesUseCase)
                )
                RecipeResultsScreen(
                    viewModel = viewModel,
                    scanId = scanId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToRecipeDetail = { recipeId ->
                        navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
                    }
                )
            }
            
            composable(Screen.History.route) {
                val viewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModelFactory(scanRepository)
                )
                HistoryScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToEditor = { scanId ->
                        navController.navigate(Screen.Editor.createRoute(scanId))
                    }
                )
            }
            
            composable(
                route = Screen.RecipeDetail.route,
                arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getString("recipeId") ?: ""
                RecipeDetailScreen(
                    recipeId = recipeId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}