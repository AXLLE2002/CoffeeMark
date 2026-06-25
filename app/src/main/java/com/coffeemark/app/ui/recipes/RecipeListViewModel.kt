package com.coffeemark.app.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coffeemark.app.CoffeemarkApp
import com.coffeemark.app.data.entity.RecipeEntity
import com.coffeemark.app.data.entity.RecipeStepEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RecipeListItem(
    val recipe: RecipeEntity,
    val totalDuration: Int,     // 秒（所有步骤相加）
    val stepCount: Int          // 不计入显示，但内部用
)

class RecipeListViewModel : ViewModel() {

    private val recipeDao = CoffeemarkApp.instance.database.recipeDao()
    private val stepDao = CoffeemarkApp.instance.database.recipeStepDao()

    private val _recipes = MutableStateFlow<List<RecipeListItem>>(emptyList())
    val recipes: StateFlow<List<RecipeListItem>> = _recipes.asStateFlow()

    init {
        viewModelScope.launch {
            // combine：方案表或步骤表任一变化都触发卡片刷新
            combine(
                recipeDao.getAll(),
                stepDao.getAllStepsFlow()
            ) { recipes, _ -> recipes }
            .collect { recipeList ->
                val items = recipeList.map { recipe ->
                    val steps = stepDao.getByRecipeIdOnce(recipe.id)
                    val totalDuration = steps.sumOf { it.duration }
                    RecipeListItem(
                        recipe = recipe,
                        totalDuration = totalDuration,
                        stepCount = steps.size
                    )
                }
                _recipes.value = items
            }
        }
    }

    fun deleteRecipe(recipeId: String) {
        viewModelScope.launch {
            recipeDao.deleteById(recipeId)
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = RecipeListViewModel() as T
    }
}
