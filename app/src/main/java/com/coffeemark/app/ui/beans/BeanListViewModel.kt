package com.coffeemark.app.ui.beans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coffeemark.app.CoffeemarkApp
import com.coffeemark.app.data.entity.BeanEntity
import kotlin.comparisons.compareBy
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class BeanUsageItem(
    val beanId: String,
    val beanName: String,
    val usedWeight: Double,
    val fraction: Float  // 0..1
)

data class BeanListState(
    val beans: List<BeanEntity> = emptyList(),
    val totalRemainingWeight: Double = 0.0,
    val totalUsedPrice: Double = 0.0,
    val selectedMonth: YearMonth = YearMonth.now(),
    val beanUsage: List<BeanUsageItem> = emptyList(),
    val usageTotalWeight: Double = 0.0,
    val earliestBrewMonth: YearMonth? = null   // 首条冲煮记录所在月（日期下拉左边界）
) {
    /** 总剩余价格 = Σ (current_weight × price_per_gram)，实时计算 */
    val totalRemainingPrice: Double
        get() = beans
            .filter { it.status != com.coffeemark.app.data.enums.BeanStatus.USED_UP }
            .sumOf { it.currentWeight * it.pricePerGram }
}

class BeanListViewModel : ViewModel() {

    private val beanDao = CoffeemarkApp.instance.database.beanDao()
    private val brewLogDao = CoffeemarkApp.instance.database.brewLogDao()

    private val _state = MutableStateFlow(BeanListState())
    val state: StateFlow<BeanListState> = _state.asStateFlow()

    init {
        // 修正历史数据：已使用但显示"未开封"的豆子 → "已开封"
        viewModelScope.launch {
            beanDao.fixUnopenedButUsedBeans()
        }
        viewModelScope.launch {
            beanDao.getAll().collect { beans ->
                _state.update { it.copy(beans = sortBeans(beans)) }
            }
        }
        viewModelScope.launch {
            beanDao.getTotalRemainingWeight().collect { w ->
                _state.update { it.copy(totalRemainingWeight = w) }
            }
        }
        viewModelScope.launch {
            beanDao.getTotalUsedPrice().collect { p ->
                _state.update { it.copy(totalUsedPrice = p) }
            }
        }
        loadMonthlyUsage(YearMonth.now())
        // 加载首条冲煮记录月份（日期下拉左边界）
        viewModelScope.launch {
            val earliest = brewLogDao.getEarliestBrewTime()
            val month = earliest?.let {
                YearMonth.from(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
            }
            _state.update { it.copy(earliestBrewMonth = month) }
        }
        // 监听冲煮记录变化，自动刷新饼图
        viewModelScope.launch {
            brewLogDao.getAll().collect {
                loadMonthlyUsage(_state.value.selectedMonth)
            }
        }
    }

    fun selectMonth(month: YearMonth) {
        _state.update { it.copy(selectedMonth = month) }
        loadMonthlyUsage(month)
    }

    private fun loadMonthlyUsage(month: YearMonth) {
        viewModelScope.launch {
            val startMs = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMs = month.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            val logs = brewLogDao.getByDateRange(startMs, endMs)
            // 按 beanId 汇总使用重量
            val grouped = logs.groupBy { it.beanId }.mapValues { (_, list) -> list.sumOf { it.beanUsedWeight } }
            val total = grouped.values.sum()
            // 直接从 DAO 查豆名，避免竞态导致显示"已删除豆子"
            val usageItems = grouped.map { (beanId, weight) ->
                val bean = beanDao.getById(beanId)
                BeanUsageItem(
                    beanId = beanId,
                    beanName = bean?.name ?: "(已删除)",
                    usedWeight = weight,
                    fraction = if (total > 0) (weight / total).toFloat() else 0f
                )
            }.sortedByDescending { it.usedWeight }
            _state.update { it.copy(beanUsage = usageItems, usageTotalWeight = total) }
        }
    }

    fun deleteBean(beanId: String) {
        viewModelScope.launch { beanDao.deleteById(beanId) }
    }

    /**
     * 排序规则：
     * - 没有任何豆子设置过 manualOrder（默认状态）→ 按剩余量降序（剩余多的在上）
     * - 已有手动排序 → 按 manualOrder 升序；未设置的（如后新增的豆子）排在末尾
     */
    private fun sortBeans(list: List<BeanEntity>): List<BeanEntity> {
        val hasManual = list.any { it.manualOrder != null }
        return if (!hasManual) {
            list.sortedByDescending { it.currentWeight }
        } else {
            list.sortedWith(
                compareBy<BeanEntity> { it.manualOrder ?: Int.MAX_VALUE }
                    .thenByDescending { it.currentWeight }
            )
        }
    }

    /** 从排序弹层提交：按给定 id 顺序写入 manual_order（落库后 VM 自动重排） */
    fun saveOrder(orderedIds: List<String>) {
        viewModelScope.launch {
            val orders = orderedIds.mapIndexed { index, id -> id to index }.toMap()
            beanDao.setManualOrders(orders)
        }
    }

    /** 重置为默认排序（清空所有 manual_order） */
    fun resetBeanOrder() {
        viewModelScope.launch { beanDao.resetManualOrders() }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = BeanListViewModel() as T
    }
}
