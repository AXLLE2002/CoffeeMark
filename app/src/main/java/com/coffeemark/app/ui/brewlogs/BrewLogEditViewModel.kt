package com.coffeemark.app.ui.brewlogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coffeemark.app.CoffeemarkApp
import com.coffeemark.app.data.entity.BeanEntity
import com.coffeemark.app.data.entity.BrewLogEntity
import com.coffeemark.app.data.entity.RecipeEntity
import com.coffeemark.app.data.enums.GrindSize
import com.coffeemark.app.data.enums.Mood
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BrewLogEditState(
    // 豆子
    val beanId: String? = null,
    val beanUsedWeight: Double = 15.0,
    val availableBeans: List<BeanEntity> = emptyList(),
    val selectedBean: BeanEntity? = null,

    // 方案
    val recipeId: String? = null,
    val recipe: RecipeEntity? = null,
    val customRecipeName: String? = null,           // 手动输入方案名
    val availableRecipes: List<RecipeEntity> = emptyList(),

    // 冲煮参数
    val groundWeight: Double = 15.0,
    val totalWater: Double = 225.0,
    val waterTemp: Int? = 92,
    val grinder: String? = null,
    val grindSize: GrindSize? = null,
    val device: String? = null,
    val location: String? = null,
    val weather: String? = null,
    val brewTime: Long = System.currentTimeMillis(),
    val totalDuration: Int = 180,       // 秒

    // 评价
    val rating: Int = 3,
    val mood: Mood? = null,
    val tastingNotes: String? = null,
    val improvementNotes: String? = null,

    // 自动补全
    val grinderSuggestions: List<String> = emptyList(),
    val deviceSuggestions: List<String> = emptyList(),

    // UI
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false,
    val brewLogId: String? = null
)

class BrewLogEditViewModel(
    private val brewLogId: String? = null,
    private val prefillRecipeId: String? = null
) : ViewModel() {

    private val brewLogDao = CoffeemarkApp.instance.database.brewLogDao()
    private val beanDao = CoffeemarkApp.instance.database.beanDao()
    private val recipeDao = CoffeemarkApp.instance.database.recipeDao()
    private val beanRepository = CoffeemarkApp.instance.beanRepository

    private val _state = MutableStateFlow(BrewLogEditState())
    val state: StateFlow<BrewLogEditState> = _state.asStateFlow()

    init {
        // 加载豆子列表（排除已用完的）
        viewModelScope.launch {
            beanDao.getAll().collect { beans ->
                _state.update { it.copy(availableBeans = beans.filter { b -> b.currentWeight > 0 }) }
            }
        }

        // 加载可选方案列表
        viewModelScope.launch {
            recipeDao.getAll().collect { recipes ->
                _state.update { it.copy(availableRecipes = recipes) }
            }
        }

        // 加载自动补全数据
        viewModelScope.launch {
            val grinders = brewLogDao.getDistinctGrinders()
            _state.update { it.copy(grinderSuggestions = grinders) }
        }
        viewModelScope.launch {
            val devices = brewLogDao.getDistinctDevices()
            _state.update { it.copy(deviceSuggestions = devices) }
        }

        // 编辑模式：加载已有记录
        if (brewLogId != null) {
            loadRecord(brewLogId)
        }

        // 预填模式：从方案跳转
        if (prefillRecipeId != null) {
            prefillFromRecipe(prefillRecipeId)
        }

        // 预填模式：从冲煮引导完成跳转（含实际计时数据）
        val appPrefill = CoffeemarkApp.instance.brewGuidePrefillData
        if (appPrefill != null && appPrefill.recipeId == prefillRecipeId) {
            prefillFromGuide(appPrefill)
            CoffeemarkApp.instance.brewGuidePrefillData = null // 消费后清空
        }
    }

    private fun loadRecord(id: String) {
        viewModelScope.launch {
            val log = brewLogDao.getById(id) ?: return@launch
            val bean = beanDao.getById(log.beanId)
            val recipe = log.recipeId?.let { recipeDao.getById(it) }
            _state.update {
                it.copy(
                    isEditMode = true, brewLogId = id,
                    beanId = log.beanId, beanUsedWeight = log.beanUsedWeight,
                    selectedBean = bean,
                    recipeId = log.recipeId, recipe = recipe,
                    customRecipeName = log.customRecipeName,
                    groundWeight = log.groundWeight, totalWater = log.totalWater,
                    waterTemp = log.waterTemp, grinder = log.grinder,
                    grindSize = log.grindSize, device = log.device,
                    location = log.location, weather = log.weather,
                    brewTime = log.brewTime, totalDuration = log.totalDuration,
                    rating = log.rating, mood = log.mood,
                    tastingNotes = log.tastingNotes,
                    improvementNotes = log.improvementNotes
                )
            }
        }
    }

    private fun prefillFromRecipe(recipeId: String) {
        viewModelScope.launch {
            val recipe = recipeDao.getById(recipeId) ?: return@launch
            _state.update {
                it.copy(
                    recipeId = recipe.id, recipe = recipe,
                    groundWeight = recipe.beanWeight,
                    totalWater = recipe.totalWater,
                    waterTemp = recipe.waterTemp,
                    grindSize = recipe.grindSize,
                    device = recipe.device
                )
            }
        }
    }

    private fun prefillFromGuide(data: com.coffeemark.app.ui.brewguide.BrewGuidePrefillData) {
        _state.update {
            it.copy(
                totalDuration = data.totalDuration,
                recipeId = data.recipeId,
                groundWeight = data.groundWeight,
                totalWater = data.totalWater,
                waterTemp = data.waterTemp,
                grindSize = data.grindSize,
                device = data.device
            )
        }
    }

    // ── Setters ──
    fun selectBean(bean: BeanEntity) {
        _state.update {
            val maxWeight = bean.currentWeight.coerceAtLeast(0.0)
            it.copy(
                beanId = bean.id,
                selectedBean = bean,
                beanUsedWeight = it.beanUsedWeight.coerceAtMost(maxWeight).coerceAtLeast(0.0)
            )
        }
    }

    fun updateWeight(v: Double) {
        val maxWeight = _state.value.selectedBean?.currentWeight?.coerceAtLeast(0.0) ?: Double.MAX_VALUE
        _state.update { it.copy(beanUsedWeight = v.coerceAtMost(maxWeight).coerceAtLeast(0.0)) }
    }
    fun updateGroundWeight(v: Double) = _state.update { it.copy(groundWeight = v) }
    fun updateTotalWater(v: Double) = _state.update { it.copy(totalWater = v) }
    fun updateWaterTemp(v: Int?) = _state.update { it.copy(waterTemp = v?.coerceIn(0, 100)) }
    fun updateGrinder(v: String?) = _state.update { it.copy(grinder = v?.trim()?.ifBlank { null }) }
    fun updateGrindSize(v: GrindSize?) = _state.update { it.copy(grindSize = v) }
    fun updateDevice(v: String?) = _state.update { it.copy(device = v?.trim()?.ifBlank { null }) }
    fun updateLocation(v: String?) = _state.update { it.copy(location = v?.trim()?.ifBlank { null }) }
    fun updateWeather(v: String?) = _state.update { it.copy(weather = v?.trim()?.ifBlank { null }) }
    fun updateRating(v: Int) = _state.update { it.copy(rating = v.coerceIn(1, 5)) }
    fun updateMood(v: Mood?) = _state.update { it.copy(mood = v) }
    fun updateTastingNotes(v: String?) = _state.update { it.copy(tastingNotes = v?.trim()?.ifBlank { null }) }
    fun updateImprovementNotes(v: String?) = _state.update { it.copy(improvementNotes = v?.trim()?.ifBlank { null }) }
    fun updateTotalDuration(v: Int) = _state.update { it.copy(totalDuration = v) }
    fun updateBrewTime(timestamp: Long) = _state.update { it.copy(brewTime = timestamp) }
    fun selectRecipe(recipe: RecipeEntity) = _state.update {
        it.copy(recipeId = recipe.id, recipe = recipe, customRecipeName = null)
    }
    fun clearRecipeSelection() = _state.update {
        it.copy(recipeId = null, recipe = null, customRecipeName = null)
    }
    fun updateCustomRecipeName(name: String) = _state.update {
        it.copy(customRecipeName = name.ifBlank { null }, recipeId = null, recipe = null)
    }

    // ── 保存（含库存扣减）──
    fun save() {
        val s = _state.value
        if (s.beanId == null) {
            _state.update { it.copy(error = "请选择豆子") }
            return
        }
        if (s.beanUsedWeight <= 0) {
            _state.update { it.copy(error = "用豆量必须大于0") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            try {
                val brewLog = BrewLogEntity(
                    id = s.brewLogId ?: java.util.UUID.randomUUID().toString(),
                    beanId = s.beanId!!,
                    beanUsedWeight = s.beanUsedWeight,
                    recipeId = s.recipeId,
                    customRecipeName = s.customRecipeName,
                    groundWeight = s.groundWeight,
                    totalWater = s.totalWater,
                    waterTemp = s.waterTemp,
                    grinder = s.grinder,
                    grindSize = s.grindSize,
                    device = s.device,
                    location = s.location,
                    weather = s.weather,
                    brewTime = s.brewTime,
                    rating = s.rating,
                    mood = s.mood,
                    tastingNotes = s.tastingNotes,
                    improvementNotes = s.improvementNotes,
                    totalDuration = s.totalDuration
                )

                if (s.isEditMode) {
                    brewLogDao.update(brewLog)
                } else {
                    beanRepository.saveBrewLogWithStockDeduction(brewLog)
                }
                _state.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = "保存失败: ${e.message}") }
            }
        }
    }

    class Factory(private val brewLogId: String? = null, private val recipeId: String? = null) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BrewLogEditViewModel(brewLogId, recipeId) as T
    }
}
