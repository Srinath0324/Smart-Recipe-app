package com.example.airecipeapp.data.repository

import com.example.airecipeapp.data.local.database.IngredientDao
import com.example.airecipeapp.data.local.database.ScanHistoryDao
import com.example.airecipeapp.data.models.*
import com.example.airecipeapp.utils.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine

class ScanRepository(
    private val scanHistoryDao: ScanHistoryDao,
    private val ingredientDao: IngredientDao
) {
    
    /**
     * Save a complete scan result (history + ingredients)
     */
    suspend fun saveScan(scanResult: ScanResult): Result<Long> {
        return try {
            // Insert scan history
            val scanId = scanHistoryDao.insert(scanResult.toEntity())
            
            // Insert ingredients
            val ingredientEntities = scanResult.ingredients.mapIndexed { index, ingredient ->
                ingredient.toEntity(scanId, index)
            }
            ingredientDao.insertAll(ingredientEntities)
            
            Result.Success(scanId)
        } catch (e: Exception) {
            Result.Error(e, "Failed to save scan")
        }
    }
    
    /**
     * Get a scan by ID with its ingredients
     */
    suspend fun getScanById(scanId: Long): Result<ScanResult?> {
        return try {
            val scanEntity = scanHistoryDao.getScanById(scanId)
            if (scanEntity != null) {
                val ingredients = ingredientDao.getIngredientsByScanId(scanId)
                    .map { it.toDomain() }
                Result.Success(scanEntity.toDomain(ingredients))
            } else {
                Result.Success(null)
            }
        } catch (e: Exception) {
            Result.Error(e, "Failed to retrieve scan")
        }
    }
    
    /**
     * Get all scans with their ingredients as Flow
     */
    fun getAllScans(): Flow<List<ScanResult>> {
        return scanHistoryDao.getAllScans().map { scans ->
            scans.map { scanEntity ->
                val ingredients = ingredientDao.getIngredientsByScanId(scanEntity.id)
                    .map { it.toDomain() }
                scanEntity.toDomain(ingredients)
            }
        }
    }
    
    /**
     * Get recent scans
     */
    fun getRecentScans(limit: Int = 10): Flow<List<ScanResult>> {
        return scanHistoryDao.getRecentScans(limit).map { scans ->
            scans.map { scanEntity ->
                val ingredients = ingredientDao.getIngredientsByScanId(scanEntity.id)
                    .map { it.toDomain() }
                scanEntity.toDomain(ingredients)
            }
        }
    }
    
    /**
     * Update ingredients for a scan
     */
    suspend fun updateIngredients(scanId: Long, ingredients: List<Ingredient>): Result<Unit> {
        return try {
            // Delete existing ingredients
            ingredientDao.deleteIngredientsByScanId(scanId)
            
            // Insert updated ingredients
            val ingredientEntities = ingredients.mapIndexed { index, ingredient ->
                ingredient.toEntity(scanId, index)
            }
            ingredientDao.insertAll(ingredientEntities)
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to update ingredients")
        }
    }
    
    /**
     * Delete a scan and its ingredients
     */
    suspend fun deleteScan(scanId: Long): Result<Unit> {
        return try {
            ingredientDao.deleteIngredientsByScanId(scanId)
            scanHistoryDao.deleteScanById(scanId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to delete scan")
        }
    }
    
    /**
     * Get total scan count
     */
    fun getScanCount(): Flow<Int> = scanHistoryDao.getScanCount()
    
    /**
     * Delete all scans
     */
    suspend fun deleteAllScans(): Result<Unit> {
        return try {
            scanHistoryDao.deleteAllScans()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to delete all scans")
        }
    }
}
