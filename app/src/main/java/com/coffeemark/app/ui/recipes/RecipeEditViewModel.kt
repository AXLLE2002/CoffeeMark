package com.coffeemark.app.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coffeemark.app.CoffeemarkApp
import com.coffeemark.app.data.entity.RecipeEntity
import com.coffeemark.app.data.entity.RecipeStepEntity
import com.coffeemark.app.data.enums.Difficulty
import com.coffeemark.app.data.enums.GrindSize
import com.coffeemark.app.data.enums.StepActionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecipeEditState(
    // 基本信息
    val name: String = "",
    val device: String = "V60",
    val waterTemp: Int = 92,
    val beanWeight: Double = 15.0,
    val grindSize: GrindSize = GrindSize.MEDIUM,
    val totalWater: Double = 225.0,
    val difficulty: Difficulty? = null,
    val source: String? = null,

    // 步骤
    val steps: List<RecipeStepState> = emptyList(),

    // UI 状态
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,

    // 编辑模式
    val isEditMode: Boolean = false,
    val recipeId: String? = null
)

data class RecipeStepState(
    val id: String = java.util.UUID.randomUUID().toString(),
    val actionType: StepActionType = StepActionType.POUR,
    val waterAmount: Double = 60.0,
    val duration: Int = 30
)

class RecipeEditViewModel(private val recipeId: String? = null) : ViewModel() {

    private val recipeDao = CoffeemarkApp.instance.database.recipeDao()
    private val stepDao = CoffeemarkApp.instance.database.recipeStepDao()

    private val _state = MutableStateFlow(RecipeEditState())
    val state: StateFlow<RecipeEditState> = _state.asStateFlow()

    init {
        if (recipeId != null) {
            loadRecipe(recipeId)
        }
    }

    private fun loadRecipe(id: String) {
        viewModelScope.launch {
            val recipe = recipeDao.getById(id) ?: return@launch
            val steps = stepDao.getByRecipeIdOnce(id)
            _state.update {
                it.copy(
                    isEditMode = true,
                    recipeId = id,
                    name = recipe.name,
                    device = recipe.device,
                    waterTemp = recipe.waterTemp,
                    beanWeight = recipe.beanWeight,
                    grindSize = recipe.grindSize,
                    totalWater = recipe.totalWater,
                    difficulty = recipe.difficulty,
                    source = recipe.source,
                    steps = steps.map { s ->
                        RecipeStepState(
                            id = s.id,
                            actionType = s.actionType,
                            waterAmount = s.waterAmount,
                            duration = s.duration
                        )
                    }
                )
            }
        }
    }

    // ── 基本信息更新 ──
    fun updateName(name: String) = _state.update { it.copy(name = name) }
    fun updateDevice(device: String) = _state.update { it.copy(device = device) }
    fun updateWaterTemp(temp: Int) = _state.update { it.copy(waterTemp = temp.coerceIn(0, 100)) }
    fun updateBeanWeight(weight: Double) = _state.update { it.copy(beanWeight = weight) }
    fun updateGrindSize(grindSize: GrindSize) = _state.update { it.copy(grindSize = grindSize) }
    fun updateTotalWater(water: Double) = _state.update { it.copy(totalWater = water) }
    fun updateDifficulty(difficulty: Difficulty?) = _state.update { it.copy(difficulty = difficulty) }
    fun updateSource(source: String?) = _state.update { it.copy(source = source) }

    /** 微调总水量 */
    fun adjustTotalWater(delta: Double) {
        _state.update { it.copy(totalWater = (it.totalWater + delta).coerceAtLeast(0.0)) }
    }

    // ── 步骤操作 ──
    fun addStep(actionType: StepActionType) {
        val defaultWater = when (actionType) {
            StepActionType.STIR, StepActionType.WAIT -> 0.0
            else -> 60.0
        }
        val defaultDuration = when (actionType) {
            StepActionType.BLOOM -> 30
            StepActionType.STIR -> 10
            StepActionType.WAIT -> 45
            else -> 30
        }
        _state.update {
            it.copy(steps = it.steps + RecipeStepState(
                actionType = actionType,
                waterAmount = defaultWater,
                duration = defaultDuration
            ))
        }
    }

    fun updateStep(index: Int, step: RecipeStepState) {
        _state.update {
            val newSteps = it.steps.toMutableList()
            if (index in newSteps.indices) newSteps[index] = step
            it.copy(steps = newSteps)
        }
    }

    fun removeStep(index: Int) {
        _state.update {
            it.copy(steps = it.steps.toMutableList().also { list ->
                if (index in list.indices) list.removeAt(index)
            })
        }
    }

    /** 拖动排序 */
    fun moveStep(fromIndex: Int, toIndex: Int) {
        _state.update {
            val newSteps = it.steps.toMutableList()
            if (fromIndex in newSteps.indices && toIndex in newSteps.indices) {
                val item = newSteps.removeAt(fromIndex)
                newSteps.add(toIndex, item)
            }
            it.copy(steps = newSteps)
        }
    }

    // ── 保存 ──
    fun save() {
        val s = _state.value
        // 校验
        if (s.name.isBlank()) {
            _state.update { it.copy(error = "方案名称不能为空") }
            return
        }
        if (s.steps.isEmpty()) {
            _state.update { it.copy(error = "至少添加一个步骤") }
            return
        }
        // 校验：步骤注水量总和必须等于总水量
        val stepsWaterSum = s.steps.sumOf { it.waterAmount }
        if (kotlin.math.abs(stepsWaterSum - s.totalWater) > 0.01) {
            _state.update { it.copy(error = "步骤注水量总和（${stepsWaterSum.toLong()}g）不等于总水量（${s.totalWater.toLong()}g）") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            try {
                val recipe = RecipeEntity(
                    id = s.recipeId ?: java.util.UUID.randomUUID().toString(),
                    name = s.name.trim(),
                    device = s.device,
                    waterTemp = s.waterTemp,
                    beanWeight = s.beanWeight,
                    grindSize = s.grindSize,
                    totalWater = s.totalWater,
                    difficulty = s.difficulty,
                    source = s.source?.trim()?.ifBlank { null },
                    updatedAt = System.currentTimeMillis()
                )

                if (s.isEditMode) {
                    recipeDao.update(recipe)
                    stepDao.deleteByRecipeId(recipe.id)
                } else {
                    recipeDao.insert(recipe)
                }

                // 重新插入步骤
                val stepEntities = s.steps.mapIndexed { index, step ->
                    RecipeStepEntity(
                        recipeId = recipe.id,
                        order = index,
                        actionType = step.actionType,
                        waterAmount = step.waterAmount,
                        duration = step.duration
                    )
                }
                stepDao.insertAll(stepEntities)

                _state.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = "保存失败: ${e.message}") }
            }
        }
    }

    class Factory(private val recipeId: String? = null) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RecipeEditViewModel(recipeId) as T
    }
}
