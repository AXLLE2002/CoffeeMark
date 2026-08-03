package com.coffeemark.app.ui.beans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coffeemark.app.CoffeemarkApp
import com.coffeemark.app.data.entity.BeanEntity
import kotlinx.coroutines.flow.*
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
        // 监听数据库流：从编辑页保存后 popBackStack 返回本页时，数据会实时刷新
        viewModelScope.launch {
            beanDao.observeById(beanId).collect { bean ->
                _state.update { it.copy(bean = bean, isLoading = false) }
            }
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
