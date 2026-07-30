package com.coffeemark.app.ui.brewlogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coffeemark.app.CoffeemarkApp
import com.coffeemark.app.data.entity.BrewLogEntity
import com.coffeemark.app.data.repository.BeanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class BrewLogDayState(
    val items: List<BrewLogListItem> = emptyList(),
    val isLoading: Boolean = true,
    val date: LocalDate? = null
)

class BrewLogDayListViewModel(private val dateMillis: Long) : ViewModel() {

    private val brewLogDao = CoffeemarkApp.instance.database.brewLogDao()
    private val beanDao = CoffeemarkApp.instance.database.beanDao()
    private val recipeDao = CoffeemarkApp.instance.database.recipeDao()
    private val beanRepository: BeanRepository = CoffeemarkApp.instance.beanRepository

    private val _state = MutableStateFlow(BrewLogDayState())
    val state: StateFlow<BrewLogDayState> = _state.asStateFlow()

    init {
        val date = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        _state.update { it.copy(date = date) }
        loadForDay(date)
    }

    private fun loadForDay(date: LocalDate) {
        viewModelScope.launch {
            val startMs = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMs = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            val logs = brewLogDao.getByDateRange(startMs, endMs)
            val items = logs.map { log -> mapItem(log) }
            _state.update { it.copy(items = items, isLoading = false) }
        }
    }

    private suspend fun mapItem(log: BrewLogEntity): BrewLogListItem {
        val bean = beanDao.getById(log.beanId)
        val recipe = log.recipeId?.let { recipeDao.getById(it) }
        val usedPrice = bean?.let {
            if (it.netWeight > 0) log.beanUsedWeight * it.price / it.netWeight else 0.0
        } ?: 0.0
        return BrewLogListItem(
            brewLog = log,
            beanName = bean?.name ?: "未知豆子",
            recipeName = recipe?.name ?: log.customRecipeName,
            beanUsedPrice = usedPrice
        )
    }

    /** 删除记录并回退库存，随后重载当日列表 */
    fun delete(brewLogId: String) {
        viewModelScope.launch {
            val log = brewLogDao.getById(brewLogId) ?: return@launch
            beanRepository.deleteBrewLogWithStockRestore(log)
            state.value.date?.let { loadForDay(it) }
        }
    }

    class Factory(private val dateMillis: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BrewLogDayListViewModel(dateMillis) as T
    }
}
