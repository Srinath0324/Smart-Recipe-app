package com.example.airecipeapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey


data class Ingredient(
    val name: String,
    val quantity: String,
    val unit: String,
    val confidence: Float = 1.0f
)


@Entity(tableName = "ingredients")
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scanId: Long,
    val name: String,
    val quantity: String,
    val unit: String,
    val confidence: Float,
    val position: Int
)


fun Ingredient.toEntity(scanId: Long, position: Int) = IngredientEntity(
    scanId = scanId,
    name = name,
    quantity = quantity,
    unit = unit,
    confidence = confidence,
    position = position
)

fun IngredientEntity.toDomain() = Ingredient(
    name = name,
    quantity = quantity,
    unit = unit,
    confidence = confidence
)
