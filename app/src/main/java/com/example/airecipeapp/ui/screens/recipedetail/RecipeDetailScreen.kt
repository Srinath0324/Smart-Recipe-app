package com.example.airecipeapp.ui.screens.recipedetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.airecipeapp.data.repository.RecipeRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val recipeRepository = remember { RecipeRepository(context) }
    var recipe by remember { mutableStateOf<com.example.airecipeapp.data.models.Recipe?>(null) }
    
    LaunchedEffect(recipeId) {
        when (val result = recipeRepository.getRecipeById(recipeId)) {
            is com.example.airecipeapp.utils.Result.Success<*> -> {
                recipe = result.data as com.example.airecipeapp.data.models.Recipe?
            }
            else -> {
                recipe = null
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe?.name ?: "Recipe Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (recipe == null) {
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
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Recipe header
                    Text(
                        text = recipe!!.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = recipe!!.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Recipe info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column {
                            Icon(Icons.Default.Timer, contentDescription = null)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${recipe!!.prepTimeMinutes} min",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Column {
                            Icon(Icons.Default.Restaurant, contentDescription = null)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = recipe!!.difficulty,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Ingredients section
                    Text(
                        text = "Ingredients",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                itemsIndexed(recipe!!.ingredients) { _, ingredient ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text("• ", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = ingredient,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Instructions section
                    Text(
                        text = "Instructions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                itemsIndexed(recipe!!.instructions) { index, instruction ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "${index + 1}. ",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = instruction,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
