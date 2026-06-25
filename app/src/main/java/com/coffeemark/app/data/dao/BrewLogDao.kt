package com.coffeemark.app.data.dao

import androidx.room.*
import com.coffeemark.app.data.entity.BrewLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BrewLogDao {

    @Query("SELECT * FROM brew_logs ORDER BY brew_time DESC")
    fun getAll(): Flow<List<BrewLogEntity>>

    @Query("SELECT * FROM brew_logs WHERE id = :id")
    suspend fun getById(id: String): BrewLogEntity?

    @Query("SELECT * FROM brew_logs WHERE bean_id = :beanId ORDER BY brew_time DESC")
    fun getByBeanId(beanId: String): Flow<List<BrewLogEntity>>

    @Query("SELECT * FROM brew_logs WHERE recipe_id = :recipeId ORDER BY brew_time DESC")
    fun getByRecipeId(recipeId: String): Flow<List<BrewLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(brewLog: BrewLogEntity)

    @Update
    suspend fun update(brewLog: BrewLogEntity)

    @Delete
    suspend fun delete(brewLog: BrewLogEntity)

    @Query("DELETE FROM brew_logs WHERE id = :id")
    suspend fun deleteById(id: String)

    // ── 自动补全：获取历史填过的磨豆机 ──
    @Query("SELECT DISTINCT grinder FROM brew_logs WHERE grinder IS NOT NULL AND grinder != '' ORDER BY brew_time DESC LIMIT 20")
    suspend fun getDistinctGrinders(): List<String>

    // ── 自动补全：获取历史填过的器具 ──
    @Query("SELECT DISTINCT device FROM brew_logs WHERE device IS NOT NULL AND device != '' ORDER BY brew_time DESC LIMIT 20")
    suspend fun getDistinctDevices(): List<String>

    // ── 日历视图 ──
    @Query("SELECT MIN(brew_time) FROM brew_logs")
    suspend fun getEarliestBrewTime(): Long?

    @Query("SELECT DISTINCT brew_time FROM brew_logs WHERE brew_time BETWEEN :startMs AND :endMs")
    fun getBrewTimestampsInRange(startMs: Long, endMs: Long): Flow<List<Long>>

    @Query("SELECT * FROM brew_logs WHERE brew_time BETWEEN :startMs AND :endMs")
    suspend fun getByDateRange(startMs: Long, endMs: Long): List<BrewLogEntity>
}
