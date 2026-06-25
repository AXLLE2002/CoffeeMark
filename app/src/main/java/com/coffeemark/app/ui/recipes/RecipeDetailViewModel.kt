package com.coffeemark.app.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coffeemark.app.CoffeemarkApp
import com.coffeemark.app.data.entity.RecipeEntity
import com.coffeemark.app.data.entity.RecipeStepEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecipeDetailState(
    val recipe: RecipeEntity? = null,
    val steps: List<RecipeStepEntity> = emptyList(),
    val isLoading: Boolean = true
)

class RecipeDetailViewModel(private val recipeId: String) : ViewModel() {

    private val recipeDao = CoffeemarkApp.instance.database.recipeDao()
    private val stepDao = CoffeemarkApp.instance.database.recipeStepDao()

    private val _state = MutableStateFlow(RecipeDetailState())
    val state: StateFlow<RecipeDetailState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val recipe = recipeDao.getById(recipeId)
            val steps = stepDao.getByRecipeIdOnce(recipeId)
            _state.update {
                it.copy(recipe = recipe, steps = steps, isLoading = false)
            }
        }
    }

    fun deleteRecipe(onDeleted: () -> Unit) {
        viewModelScope.launch {
            recipeDao.deleteById(recipeId)
            onDeleted()
        }
    }

    class Factory(private val recipeId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RecipeDetailViewModel(recipeId) as T
    }
}
