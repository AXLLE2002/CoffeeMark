package com.coffeemark.app.data.dao

import androidx.room.*
import com.coffeemark.app.data.entity.BeanEntity
import com.coffeemark.app.data.enums.BeanStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface BeanDao {

    @Query("SELECT * FROM beans ORDER BY roast_date DESC")
    fun getAll(): Flow<List<BeanEntity>>

    @Query("SELECT * FROM beans WHERE id = :id")
    suspend fun getById(id: String): BeanEntity?

    @Query("SELECT * FROM beans WHERE status = :status ORDER BY roast_date DESC")
    fun getByStatus(status: BeanStatus): Flow<List<BeanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bean: BeanEntity)

    @Update
    suspend fun update(bean: BeanEntity)

    @Delete
    suspend fun delete(bean: BeanEntity)

    @Query("DELETE FROM beans WHERE id = :id")
    suspend fun deleteById(id: String)

    // ── 状态修正：已使用但还显示"未开封"的豆子 → 改为"已开封" ──
    @Query("UPDATE beans SET status = 'OPENED' WHERE status = 'UNOPENED' AND current_weight < net_weight")
    suspend fun fixUnopenedButUsedBeans()

    // ── 顶部汇总查询 ──
    @Query("SELECT COALESCE(SUM(current_weight), 0) FROM beans WHERE status != 'USED_UP'")
    fun getTotalRemainingWeight(): Flow<Double>

    @Query("SELECT COALESCE(SUM(total_used_price), 0) FROM beans")
    fun getTotalUsedPrice(): Flow<Double>

    // ── 库存扣减（单条update，由Repository在@Transaction中调用）──
    @Query("""
        UPDATE beans
        SET current_weight = MAX(0, current_weight - :usedWeight),
            total_used_price = total_used_price + (:usedWeight * 1.0 * price / net_weight),
            status = CASE WHEN (current_weight - :usedWeight) <= 0 THEN 'USED_UP' WHEN status = 'UNOPENED' THEN 'OPENED' ELSE status END,
            updated_at = :timestamp
        WHERE id = :beanId
    """)
    suspend fun deductStock(beanId: String, usedWeight: Double, timestamp: Long = System.currentTimeMillis())

    // ── 库存回退（删除冲煮记录时调用）──
    @Query("""
        UPDATE beans
        SET current_weight = MIN(net_weight, current_weight + :usedWeight),
            total_used_price = MAX(0, total_used_price - (:usedWeight * 1.0 * price / net_weight)),
            status = CASE WHEN status = 'USED_UP' AND (current_weight + :usedWeight) > 0 THEN 'OPENED' ELSE status END,
            updated_at = :timestamp
        WHERE id = :beanId
    """)
    suspend fun restoreStock(beanId: String, usedWeight: Double, timestamp: Long = System.currentTimeMillis())
}
