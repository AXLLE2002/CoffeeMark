package com.coffeemark.app.data.dao

import androidx.room.*
import com.coffeemark.app.data.entity.RecipeStepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeStepDao {

    @Query("SELECT * FROM recipe_steps WHERE recipe_id = :recipeId ORDER BY step_order ASC")
    fun getByRecipeId(recipeId: String): Flow<List<RecipeStepEntity>>

    @Query("SELECT * FROM recipe_steps WHERE recipe_id = :recipeId ORDER BY step_order ASC")
    suspend fun getByRecipeIdOnce(recipeId: String): List<RecipeStepEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(step: RecipeStepEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(steps: List<RecipeStepEntity>)

    @Update
    suspend fun update(step: RecipeStepEntity)

    @Delete
    suspend fun delete(step: RecipeStepEntity)

    @Query("DELETE FROM recipe_steps WHERE recipe_id = :recipeId")
    suspend fun deleteByRecipeId(recipeId: String)

    @Query("DELETE FROM recipe_steps WHERE id = :id")
    suspend fun deleteById(id: String)

    /** 全局监听步骤变化，用于触发方案列表卡片刷新 */
    @Query("SELECT * FROM recipe_steps")
    fun getAllStepsFlow(): Flow<List<RecipeStepEntity>>
}
