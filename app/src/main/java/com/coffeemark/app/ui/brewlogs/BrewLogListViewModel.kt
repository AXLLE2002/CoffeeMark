package com.coffeemark.app.ui.brewlogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coffeemark.app.CoffeemarkApp
import com.coffeemark.app.data.entity.BeanEntity
import com.coffeemark.app.data.entity.BrewLogEntity
import com.coffeemark.app.data.entity.RecipeEntity
import com.coffeemark.app.data.repository.BeanRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class BrewLogListItem(
    val brewLog: BrewLogEntity,
    val beanName: String,
    val recipeName: String?,
    val beanUsedPrice: Double = 0.0   // 本次使用的豆子价值
)

data class BrewLogListState(
    val items: List<BrewLogListItem> = emptyList(),
    val isCalendarExpanded: Boolean = true,
    val currentMonth: YearMonth = YearMonth.now(),
    val brewDatesInMonth: Set<LocalDate> = emptySet(),
    val earliestBrewTime: Long? = null
)

class BrewLogListViewModel : ViewModel() {

    private val brewLogDao = CoffeemarkApp.instance.database.brewLogDao()
    private val beanDao = CoffeemarkApp.instance.database.beanDao()
    private val recipeDao = CoffeemarkApp.instance.database.recipeDao()
    private val beanRepository = CoffeemarkApp.instance.beanRepository

    private val _state = MutableStateFlow(BrewLogListState())
    val state: StateFlow<BrewLogListState> = _state.asStateFlow()

    init {
        // 加载记录列表
        viewModelScope.launch {
            brewLogDao.getAll().collect { logs ->
                val list = logs.map { log ->
                    val bean = beanDao.getById(log.beanId)
                    val recipe = log.recipeId?.let { recipeDao.getById(it) }
                    val usedPrice = bean?.let {
                        if (it.netWeight > 0) log.beanUsedWeight * it.price / it.netWeight else 0.0
                    } ?: 0.0
                    BrewLogListItem(
                        brewLog = log,
                        beanName = bean?.name ?: "未知豆子",
                        recipeName = recipe?.name ?: log.customRecipeName,
                        beanUsedPrice = usedPrice
                    )
                }
                _state.update { it.copy(items = list) }
            }
        }

        // 加载最早记录日期（日历左边界）
        viewModelScope.launch {
            val earliest = brewLogDao.getEarliestBrewTime()
            _state.update { it.copy(earliestBrewTime = earliest) }
        }

        // 加载本月冲煮日期
        loadBrewDatesForMonth(_state.value.currentMonth)
    }

    fun toggleCalendar() {
        _state.update { it.copy(isCalendarExpanded = !it.isCalendarExpanded) }
    }

    fun goToPrevMonth() {
        val newMonth = _state.value.currentMonth.minusMonths(1)
        val earliest = _state.value.earliestBrewTime
        if (earliest != null) {
            val earliestMonth = YearMonth.from(Instant.ofEpochMilli(earliest).atZone(ZoneId.systemDefault()))
            if (newMonth.isBefore(earliestMonth)) return
        }
        _state.update { it.copy(currentMonth = newMonth) }
        loadBrewDatesForMonth(newMonth)
    }

    fun goToNextMonth() {
        val newMonth = _state.value.currentMonth.plusMonths(1)
        if (newMonth.isAfter(YearMonth.now())) return
        _state.update { it.copy(currentMonth = newMonth) }
        loadBrewDatesForMonth(newMonth)
    }

    private fun loadBrewDatesForMonth(month: YearMonth) {
        viewModelScope.launch {
            val startMs = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMs = month.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            brewLogDao.getBrewTimestampsInRange(startMs, endMs).collect { timestamps ->
                val dates = timestamps.map { ts ->
                    Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()
                }.toSet()
                _state.update { it.copy(brewDatesInMonth = dates) }
            }
        }
    }

    /** 删除记录并回退库存 */
    fun delete(brewLogId: String) {
        viewModelScope.launch {
            val log = brewLogDao.getById(brewLogId) ?: return@launch
            beanRepository.deleteBrewLogWithStockRestore(log)
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = BrewLogListViewModel() as T
    }
}
