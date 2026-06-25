package com.coffeemark.app.ui.beans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coffeemark.app.CoffeemarkApp
import com.coffeemark.app.data.entity.BeanEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BeanDetailState(
    val bean: BeanEntity? = null,
    val isLoading: Boolean = true
)

class BeanDetailViewModel(private val beanId: String) : ViewModel() {

    private val beanDao = CoffeemarkApp.instance.database.beanDao()

    private val _state = MutableStateFlow(BeanDetailState())
    val state: StateFlow<BeanDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val bean = beanDao.getById(beanId)
            _state.update { it.copy(bean = bean, isLoading = false) }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            beanDao.deleteById(beanId)
            onDeleted()
        }
    }

    class Factory(private val beanId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = BeanDetailViewModel(beanId) as T
    }
}
