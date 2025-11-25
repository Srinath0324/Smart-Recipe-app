package com.example.airecipeapp.ui.screens.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.airecipeapp.data.models.Ingredient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientEditorScreen(
    viewModel: EditorViewModel,
    scanId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToRecipes: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(scanId) {
        viewModel.loadScan(scanId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Ingredients") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Add")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    viewModel.saveIngredients {
                        onNavigateToRecipes(scanId)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                enabled = uiState.ingredients.isNotEmpty()
            ) {
                Text("Find Recipes")
            }
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Review and edit your ingredients",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                itemsIndexed(uiState.ingredients) { index, ingredient ->
                    IngredientCard(
                        ingredient = ingredient,
                        onDelete = { viewModel.removeIngredient(index) },
                        onUpdate = { updated -> viewModel.updateIngredient(index, updated) }
                    )
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddIngredientDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { ingredient ->
                viewModel.addIngredient(ingredient)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun IngredientCard(
    ingredient: Ingredient,
    onDelete: () -> Unit,
    onUpdate: (Ingredient) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(ingredient.name) }
    var editedQuantity by remember { mutableStateOf(ingredient.quantity) }
    var editedUnit by remember { mutableStateOf(ingredient.unit) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        if (isEditing) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editedQuantity,
                        onValueChange = { editedQuantity = it },
                        label = { Text("Quantity") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = editedUnit,
                        onValueChange = { editedUnit = it },
                        label = { Text("Unit") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    TextButton(onClick = { isEditing = false }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = {
                        onUpdate(Ingredient(editedName, editedQuantity, editedUnit))
                        isEditing = false
                    }) {
                        Text("Save")
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ingredient.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${ingredient.quantity} ${ingredient.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { isEditing = true }) {
                    Icon(Icons.Default.Edit, "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun AddIngredientDialog(
    onDismiss: () -> Unit,
    onAdd: (Ingredient) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Ingredient") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(Ingredient(name, quantity, unit))
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
