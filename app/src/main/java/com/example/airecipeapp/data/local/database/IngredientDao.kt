package com.example.airecipeapp.data.local.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.airecipeapp.data.models.IngredientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ingredient: IngredientEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ingredients: List<IngredientEntity>)
    
    @Update
    suspend fun update(ingredient: IngredientEntity)
    
    @Delete
    suspend fun delete(ingredient: IngredientEntity)
    
    @Query("SELECT * FROM ingredients WHERE scanId = :scanId ORDER BY position ASC")
    suspend fun getIngredientsByScanId(scanId: Long): List<IngredientEntity>
    
    @Query("SELECT * FROM ingredients WHERE scanId = :scanId ORDER BY position ASC")
    fun getIngredientsByScanIdFlow(scanId: Long): Flow<List<IngredientEntity>>
    
    @Query("DELETE FROM ingredients WHERE scanId = :scanId")
    suspend fun deleteIngredientsByScanId(scanId: Long)
    
    @Query("SELECT * FROM ingredients WHERE id = :id")
    suspend fun getIngredientById(id: Long): IngredientEntity?
}
