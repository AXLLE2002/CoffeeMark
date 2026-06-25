package com.coffeemark.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level navigation destinations for the bottom bar.
 */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    BREW_LOGS(
        route = "brew_logs",
        label = "冲煮记录",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    ),
    RECIPES(
        route = "recipes",
        label = "方案库",
        selectedIcon = Icons.Filled.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook
    ),
    BEANS(
        route = "beans",
        label = "豆仓",
        selectedIcon = Icons.Filled.Inventory2,
        unselectedIcon = Icons.Outlined.Inventory2
    )
}

/**
 * All app routes.
 */
object Routes {
    // ── Top-level tabs ──
    const val RECIPES = "recipes"
    const val BREW_LOGS = "brew_logs"
    const val BEANS = "beans"

    // ── Recipe routes ──
    const val RECIPE_DETAIL = "recipes/{recipeId}"
    const val RECIPE_EDIT = "recipes/edit?recipeId={recipeId}"

    fun recipeDetail(recipeId: String) = "recipes/$recipeId"
    fun recipeEdit(recipeId: String? = null) =
        if (recipeId != null) "recipes/edit?recipeId=$recipeId" else "recipes/edit"

    // ── Bean routes ──
    const val BEAN_DETAIL = "beans/{beanId}"
    const val BEAN_EDIT = "beans/edit?beanId={beanId}"

    fun beanDetail(beanId: String) = "beans/$beanId"
    fun beanEdit(beanId: String? = null) =
        if (beanId != null) "beans/edit?beanId=$beanId" else "beans/edit"

    // ── BrewLog routes ──
    const val BREW_LOG_DETAIL = "brew_logs/{brewLogId}"
    const val BREW_LOG_EDIT = "brew_logs/edit?brewLogId={brewLogId}&recipeId={recipeId}"

    fun brewLogDetail(brewLogId: String) = "brew_logs/$brewLogId"
    fun brewLogEdit(brewLogId: String? = null, recipeId: String? = null): String {
        val params = mutableListOf<String>()
        brewLogId?.let { params.add("brewLogId=$it") }
        recipeId?.let { params.add("recipeId=$it") }
        return if (params.isEmpty()) "brew_logs/edit"
        else "brew_logs/edit?${params.joinToString("&")}"
    }

    // ── Brew Guide routes ──
    const val BREW_PREPARE = "brew/prepare/{recipeId}"
    const val BREW_GUIDE = "brew/guide/{recipeId}"
    const val BREW_COMPLETE = "brew/complete/{recipeId}"

    fun brewPrepare(recipeId: String) = "brew/prepare/$recipeId"
    fun brewGuide(recipeId: String) = "brew/guide/$recipeId"
    fun brewComplete(recipeId: String) = "brew/complete/$recipeId"
}
