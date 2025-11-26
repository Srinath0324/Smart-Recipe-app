package com.example.airecipeapp.ui.screens.recipes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.airecipeapp.ui.components.AIRecipeCard
import com.example.airecipeapp.ui.components.ModelDownloadDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeResultsScreen(
    viewModel: RecipeViewModel,
    scanId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToRecipeDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(scanId) {
        viewModel.loadScanAndMatchRecipes(scanId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recipe Suggestions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Ingredients summary
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Your Ingredients",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.ingredients.joinToString(", ") { it.name },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Toggle buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !uiState.showAIRecipes,
                            onClick = { if (uiState.showAIRecipes) viewModel.toggleSuggestionMode() },
                            label = { Text("Recipe Matches") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = uiState.showAIRecipes,
                            onClick = { if (!uiState.showAIRecipes) viewModel.toggleSuggestionMode() },
                            label = { Text("AI Recipes") },
                            modifier = Modifier.weight(1f),
                            leadingIcon = if (uiState.showAIRecipes) {
                                { Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                if (uiState.showAIRecipes) {
                    // AI Recipes
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "AI Recipe Suggestions",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    if (uiState.isGeneratingAI) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text(
                                        text = "Generating AI recipes...",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "This may take 10-30 seconds",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    } else if (uiState.error != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = uiState.error!!,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    } else if (uiState.aiRecipes.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "No AI recipes generated yet",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    } else {
                        items(uiState.aiRecipes.size) { index ->
                            AIRecipeCard(recipe = uiState.aiRecipes[index])
                        }
                    }
                } else {
                    // Recipe matches
                    item {
                        Text(
                            text = "Matched Recipes (${uiState.recipeMatches.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    items(uiState.recipeMatches.size) { index ->
                        val match = uiState.recipeMatches[index]
                        RecipeMatchCard(
                            match = match,
                            onViewRecipe = {
                                onNavigateToRecipeDetail(match.recipe.id)
                            }
                        )
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // Model download dialog
            if (uiState.showModelDialog && uiState.requirementsCheck != null) {
                ModelDownloadDialog(
                    isModelDownloaded = uiState.isModelDownloaded,
                    requirementsCheck = uiState.requirementsCheck!!,
                    downloadProgress = uiState.downloadProgress,
                    onDownloadClick = viewModel::downloadModel,
                    onDeleteClick = viewModel::deleteModel,
                    onDismiss = viewModel::hideModelDialog
                )
            }
        }
    }
}

@Composable
fun RecipeMatchCard(
    match: com.example.airecipeapp.data.models.RecipeMatch,
    onViewRecipe: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                // Match percentage
                Surface(
                    color = when {
                        match.matchScore >= 0.8f -> MaterialTheme.colorScheme.primary
                        match.matchScore >= 0.5f -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.tertiary
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${(match.matchScore * 100).toInt()}%",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${match.recipe.prepTimeMinutes} min",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = match.recipe.difficulty,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            if (match.matchedIngredients.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "✓ Matched: ${match.matchedIngredients.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (match.missingIngredients.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Missing: ${match.missingIngredients.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Ingredients: ${match.recipe.ingredients.size}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onViewRecipe,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Recipe")
            }
        }
    }
}
