package com.coffeemark.app.ui.beans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coffeemark.app.CoffeemarkApp
import com.coffeemark.app.data.entity.BeanEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val usageTotalWeight: Double = 0.0
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
                _state.update { it.copy(beans = beans) }
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

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = BeanListViewModel() as T
    }
}
