package com.example.airecipeapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Domain model representing a scan result
 */
data class ScanResult(
    val id: Long = 0,
    val timestamp: Long,
    val rawText: String,
    val ingredients: List<Ingredient>,
    val imageUri: String?,
    val processedImageUri: String? = null
)

/**
 * Room entity for storing scan history
 */
@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val rawText: String,
    val imageUri: String?,
    val processedImageUri: String?
)

/**
 * Extension functions for converting between domain and entity models
 */
fun ScanResult.toEntity() = ScanHistoryEntity(
    id = id,
    timestamp = timestamp,
    rawText = rawText,
    imageUri = imageUri,
    processedImageUri = processedImageUri
)

fun ScanHistoryEntity.toDomain(ingredients: List<Ingredient>) = ScanResult(
    id = id,
    timestamp = timestamp,
    rawText = rawText,
    ingredients = ingredients,
    imageUri = imageUri,
    processedImageUri = processedImageUri
)
