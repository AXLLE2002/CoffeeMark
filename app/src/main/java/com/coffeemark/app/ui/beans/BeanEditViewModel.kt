package com.coffeemark.app.ui.beans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coffeemark.app.CoffeemarkApp
import com.coffeemark.app.data.entity.BeanEntity
import com.coffeemark.app.data.enums.BeanStatus
import com.coffeemark.app.data.enums.BeanType
import com.coffeemark.app.data.enums.RoastLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BeanEditState(
    val name: String = "",
    val netWeight: Double = 200.0,
    val currentWeight: Double = 200.0,
    val price: Double = 0.0,
    val roaster: String? = null,
    val estateStation: String? = null,
    val producer: String? = null,
    val batch: String? = null,
    val altitude: String? = null,
    val roastDate: Long = System.currentTimeMillis(),
    val shelfLifeDays: Int = 30,
    val beanType: BeanType = BeanType.SINGLE_ORIGIN,
    val origin: String? = null,
    val process: String? = null,
    val varietal: String? = null,
    val roastLevel: RoastLevel? = null,
    val isEspresso: Boolean = false,
    val flavorTags: List<String> = emptyList(),
    val status: BeanStatus = BeanStatus.UNOPENED,
    val notes: String? = null,

    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false,
    val beanId: String? = null
)

class BeanEditViewModel(private val beanId: String? = null) : ViewModel() {

    private val beanDao = CoffeemarkApp.instance.database.beanDao()

    private val _state = MutableStateFlow(BeanEditState())
    val state: StateFlow<BeanEditState> = _state.asStateFlow()

    init {
        if (beanId != null) loadBean(beanId)
    }

    private fun loadBean(id: String) {
        viewModelScope.launch {
            val bean = beanDao.getById(id) ?: return@launch
            _state.update {
                it.copy(
                    isEditMode = true, beanId = id,
                    name = bean.name, netWeight = bean.netWeight,
                    currentWeight = bean.currentWeight, price = bean.price,
                    roaster = bean.roaster, estateStation = bean.estateStation,
                    producer = bean.producer, batch = bean.batch,
                    altitude = bean.altitude, roastDate = bean.roastDate,
                    shelfLifeDays = bean.shelfLifeDays, beanType = bean.beanType,
                    origin = bean.origin, process = bean.process,
                    varietal = bean.varietal, roastLevel = bean.roastLevel,
                    isEspresso = bean.isEspresso, flavorTags = bean.flavorTags ?: emptyList(),
                    status = bean.status, notes = bean.notes
                )
            }
        }
    }

    // ── Setters ──
    fun updateName(v: String) = _state.update { it.copy(name = v) }
    fun updateNetWeight(v: Double) = _state.update {
        it.copy(netWeight = v, currentWeight = if (!it.isEditMode) v else it.currentWeight)
    }
    fun updateCurrentWeight(v: Double) = _state.update { it.copy(currentWeight = v) }
    fun updatePrice(v: Double) = _state.update { it.copy(price = v) }
    fun updateRoaster(v: String?) = _state.update { it.copy(roaster = v) }
    fun updateOrigin(v: String?) = _state.update { it.copy(origin = v) }
    fun updateProcess(v: String?) = _state.update { it.copy(process = v) }
    fun updateVarietal(v: String?) = _state.update { it.copy(varietal = v) }
    fun updateRoastLevel(v: RoastLevel?) = _state.update { it.copy(roastLevel = v) }
    fun updateIsEspresso(v: Boolean) = _state.update { it.copy(isEspresso = v) }
    fun updateNotes(v: String?) = _state.update { it.copy(notes = v) }
    fun updateBeanType(v: BeanType) = _state.update { it.copy(beanType = v) }
    fun updateStatus(v: BeanStatus) = _state.update { it.copy(status = v) }
    fun updateRoastDate(v: Long) = _state.update { it.copy(roastDate = v) }
    fun updateShelfLife(v: Int) = _state.update { it.copy(shelfLifeDays = v) }
    fun updateEstateStation(v: String?) = _state.update { it.copy(estateStation = v) }
    fun updateProducer(v: String?) = _state.update { it.copy(producer = v) }
    fun updateBatch(v: String?) = _state.update { it.copy(batch = v) }
    fun updateAltitude(v: String?) = _state.update { it.copy(altitude = v) }

    fun toggleFlavorTag(tag: String) {
        _state.update {
            val tags = it.flavorTags.toMutableList()
            if (tags.contains(tag)) tags.remove(tag) else tags.add(tag)
            it.copy(flavorTags = tags)
        }
    }

    fun addCustomFlavorTag(tag: String) {
        if (tag.isNotBlank() && tag !in _state.value.flavorTags) {
            _state.update { it.copy(flavorTags = it.flavorTags + tag.trim()) }
        }
    }

    fun removeFlavorTag(tag: String) {
        _state.update { it.copy(flavorTags = it.flavorTags - tag) }
    }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.update { it.copy(error = "名称不能为空") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            try {
                val bean = BeanEntity(
                    id = s.beanId ?: java.util.UUID.randomUUID().toString(),
                    name = s.name.trim(),
                    netWeight = s.netWeight,
                    currentWeight = s.currentWeight,
                    price = s.price,
                    roaster = s.roaster?.trim()?.ifBlank { null },
                    estateStation = s.estateStation?.trim()?.ifBlank { null },
                    producer = s.producer?.trim()?.ifBlank { null },
                    batch = s.batch?.trim()?.ifBlank { null },
                    altitude = s.altitude?.trim()?.ifBlank { null },
                    roastDate = s.roastDate,
                    shelfLifeDays = s.shelfLifeDays,
                    beanType = s.beanType,
                    origin = s.origin?.trim()?.ifBlank { null },
                    process = s.process?.trim()?.ifBlank { null },
                    varietal = s.varietal?.trim()?.ifBlank { null },
                    roastLevel = s.roastLevel,
                    isEspresso = s.isEspresso,
                    flavorTags = s.flavorTags.ifEmpty { null },
                    status = if (s.currentWeight <= 0) BeanStatus.USED_UP else s.status,
                    notes = s.notes?.trim()?.ifBlank { null },
                    updatedAt = System.currentTimeMillis()
                )
                if (s.isEditMode) beanDao.update(bean) else beanDao.insert(bean)
                _state.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = "保存失败: ${e.message}") }
            }
        }
    }

    class Factory(private val beanId: String? = null) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = BeanEditViewModel(beanId) as T
    }
}
