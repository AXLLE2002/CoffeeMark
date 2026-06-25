package com.coffeemark.app.data.repository

import com.coffeemark.app.data.dao.BeanDao
import com.coffeemark.app.data.dao.BrewLogDao
import com.coffeemark.app.data.entity.BrewLogEntity

/**
 * 豆仓库存业务逻辑：
 * - 冲煮记录保存时自动扣减库存
 * - 记录删除时自动回退库存
 */
class BeanRepository(
    private val beanDao: BeanDao,
    private val brewLogDao: BrewLogDao
) {

    /**
     * 保存冲煮记录并自动扣减豆仓库存。
     *
     * 1. 根据 bean_id 查找对应豆子
     * 2. current_weight -= bean_used_weight
     * 3. total_used_price += bean_used_weight × price_per_gram
     * 4. 若 current_weight ≤ 0，status = "已用完"
     * 5. 更新豆子 → 保存记录
     */
    suspend fun saveBrewLogWithStockDeduction(brewLog: BrewLogEntity) {
        beanDao.deductStock(
            beanId = brewLog.beanId,
            usedWeight = brewLog.beanUsedWeight
        )
        brewLogDao.insert(brewLog)
    }

    /**
     * 删除冲煮记录并回退库存。
     * 先回退豆子库存（current_weight + usedWeight, total_used_price - usedCost），
     * 再删除记录。
     */
    suspend fun deleteBrewLogWithStockRestore(brewLog: BrewLogEntity) {
        beanDao.restoreStock(
            beanId = brewLog.beanId,
            usedWeight = brewLog.beanUsedWeight
        )
        brewLogDao.delete(brewLog)
    }
}
