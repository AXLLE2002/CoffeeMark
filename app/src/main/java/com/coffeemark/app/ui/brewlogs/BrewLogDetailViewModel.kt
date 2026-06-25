package com.coffeemark.app.ui.brewlogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coffeemark.app.CoffeemarkApp
import com.coffeemark.app.data.entity.BeanEntity
import com.coffeemark.app.data.entity.BrewLogEntity
import com.coffeemark.app.data.entity.RecipeEntity
import com.coffeemark.app.data.entity.RecipeStepEntity
import com.coffeemark.app.data.enums.GrindSize
import com.coffeemark.app.data.enums.StepActionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BrewLogDetailState(
    val brewLog: BrewLogEntity? = null,
    val bean: BeanEntity? = null,
    val recipe: RecipeEntity? = null,
    val isLoading: Boolean = true,
    val savedAsRecipeId: String? = null  // 另存为方案后的新方案ID
)

class BrewLogDetailViewModel(private val brewLogId: String) : ViewModel() {

    private val brewLogDao = CoffeemarkApp.instance.database.brewLogDao()
    private val beanDao = CoffeemarkApp.instance.database.beanDao()
    private val recipeDao = CoffeemarkApp.instance.database.recipeDao()
    private val stepDao = CoffeemarkApp.instance.database.recipeStepDao()
    private val beanRepository = CoffeemarkApp.instance.beanRepository

    private val _state = MutableStateFlow(BrewLogDetailState())
    val state: StateFlow<BrewLogDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val log = brewLogDao.getById(brewLogId)
            val bean = log?.let { beanDao.getById(it.beanId) }
            val recipe = log?.recipeId?.let { recipeDao.getById(it) }
            _state.update { it.copy(brewLog = log, bean = bean, recipe = recipe, isLoading = false) }
        }
    }

    /** 删除记录（自动回退库存） */
    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val log = _state.value.brewLog ?: return@launch
            beanRepository.deleteBrewLogWithStockRestore(log)
            onDeleted()
        }
    }

    /** 另存为新方案（基于当前记录参数创建新方案） */
    fun saveAsRecipe() {
        viewModelScope.launch {
            val log = _state.value.brewLog ?: return@launch
            val recipe = RecipeEntity(
                id = java.util.UUID.randomUUID().toString(),
                name = log.beanUsedWeight.toString() + "g " + (_state.value.bean?.name ?: "手冲"),
                device = log.device ?: "V60",
                waterTemp = log.waterTemp ?: 92,
                beanWeight = log.groundWeight,
                grindSize = log.grindSize ?: GrindSize.MEDIUM,
                totalWater = log.totalWater,
                difficulty = null,
                source = "来自冲煮记录",
                updatedAt = System.currentTimeMillis()
            )
            recipeDao.insert(recipe)

            // 创建一个默认注水步骤
            val defaultStep = RecipeStepEntity(
                recipeId = recipe.id,
                order = 0,
                actionType = StepActionType.POUR,
                waterAmount = log.totalWater,
                duration = log.totalDuration.takeIf { it > 0 } ?: 180
            )
            stepDao.insert(defaultStep)

            _state.update { it.copy(savedAsRecipeId = recipe.id) }
        }
    }

    class Factory(private val brewLogId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BrewLogDetailViewModel(brewLogId) as T
    }
}
