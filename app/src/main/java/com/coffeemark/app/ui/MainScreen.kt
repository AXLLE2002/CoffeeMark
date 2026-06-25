package com.coffeemark.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.coffeemark.app.navigation.Routes
import com.coffeemark.app.navigation.TopLevelDestination
import com.coffeemark.app.ui.beans.BeanDetailScreen
import com.coffeemark.app.ui.beans.BeanEditScreen
import com.coffeemark.app.ui.beans.BeanListScreen
import com.coffeemark.app.CoffeemarkApp
import com.coffeemark.app.ui.brewguide.BrewCompleteScreen
import com.coffeemark.app.ui.brewguide.BrewGuideScreen
import com.coffeemark.app.ui.brewguide.BrewPrepareScreen
import com.coffeemark.app.ui.brewlogs.BrewLogDetailScreen
import com.coffeemark.app.ui.brewlogs.BrewLogEditScreen
import com.coffeemark.app.ui.brewlogs.BrewLogListScreen
import com.coffeemark.app.ui.recipes.RecipeDetailScreen
import com.coffeemark.app.ui.recipes.RecipeEditScreen
import com.coffeemark.app.ui.recipes.RecipeListScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val topLevelRoutes = TopLevelDestination.entries.map { it.route }
    val showBottomBar = currentDestination?.route in topLevelRoutes

    // 当前标签页信息
    val currentTab = TopLevelDestination.entries
        .firstOrNull { it.route == currentDestination?.route }
    val currentTabLabel = currentTab?.label ?: "CoffeeMark"
    val currentTabIcon = currentTab?.selectedIcon

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == destination.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) destination.selectedIcon
                                        else destination.unselectedIcon,
                                    contentDescription = destination.label
                                )
                            },
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            // ── 共享顶部品牌栏（仅标签页显示）──
            if (showBottomBar) {
                Surface(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 2.dp,
                    shadowElevation = 0.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentTabIcon != null) {
                                Icon(
                                    imageVector = currentTabIcon,
                                    contentDescription = currentTabLabel,
                                    modifier = Modifier.size(24.dp),
                                    tint = Color(0xFF6D4C41)
                                )
                            } else {
                                Text("☕", style = MaterialTheme.typography.titleLarge)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                currentTabLabel,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        HorizontalDivider(
                            color = Color(0xFF6D4C41),
                            thickness = 2.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            // ── NavHost ──
            NavHost(
                navController = navController,
                startDestination = Routes.BREW_LOGS,
                modifier = Modifier.fillMaxSize()
            ) {
                // ── Top-level tabs ──
                composable(Routes.RECIPES) {
                    RecipeListScreen(
                        onRecipeClick = { id -> navController.navigate(Routes.recipeDetail(id)) },
                        onCreateClick = { navController.navigate(Routes.recipeEdit()) }
                    )
                }
                composable(Routes.BREW_LOGS) {
                    BrewLogListScreen(
                        onBrewLogClick = { id -> navController.navigate(Routes.brewLogDetail(id)) },
                        onCreateClick = { navController.navigate(Routes.brewLogEdit()) }
                    )
                }
                composable(Routes.BEANS) {
                    BeanListScreen(
                        onBeanClick = { id -> navController.navigate(Routes.beanDetail(id)) },
                        onCreateClick = { navController.navigate(Routes.beanEdit()) }
                    )
                }

                // ── Recipe routes ──
                composable(
                    route = Routes.RECIPE_DETAIL,
                    arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val recipeId = backStackEntry.arguments?.getString("recipeId") ?: return@composable
                    RecipeDetailScreen(
                        recipeId = recipeId,
                        onEdit = { navController.navigate(Routes.recipeEdit(recipeId)) },
                        onStartBrew = { navController.navigate(Routes.brewPrepare(recipeId)) },
                        onDeleted = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Routes.RECIPE_EDIT,
                    arguments = listOf(navArgument("recipeId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    })
                ) { backStackEntry ->
                    val recipeId = backStackEntry.arguments?.getString("recipeId")
                    RecipeEditScreen(
                        recipeId = recipeId,
                        onSaved = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }

                // ── Bean routes ──
                composable(
                    route = Routes.BEAN_DETAIL,
                    arguments = listOf(navArgument("beanId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val beanId = backStackEntry.arguments?.getString("beanId") ?: return@composable
                    BeanDetailScreen(
                        beanId = beanId,
                        onEdit = { navController.navigate(Routes.beanEdit(beanId)) },
                        onDeleted = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Routes.BEAN_EDIT,
                    arguments = listOf(navArgument("beanId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    })
                ) { backStackEntry ->
                    val beanId = backStackEntry.arguments?.getString("beanId")
                    BeanEditScreen(
                        beanId = beanId,
                        onSaved = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }

                // ── BrewLog routes ──
                composable(
                    route = Routes.BREW_LOG_DETAIL,
                    arguments = listOf(navArgument("brewLogId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val brewLogId = backStackEntry.arguments?.getString("brewLogId") ?: return@composable
                    BrewLogDetailScreen(
                        brewLogId = brewLogId,
                        onEdit = { navController.navigate(Routes.brewLogEdit(brewLogId)) },
                        onSaveAsRecipe = { newRecipeId ->
                            navController.navigate(Routes.recipeEdit(newRecipeId))
                        },
                        onDeleted = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Routes.BREW_LOG_EDIT,
                    arguments = listOf(
                        navArgument("brewLogId") { type = NavType.StringType; nullable = true; defaultValue = null },
                        navArgument("recipeId") { type = NavType.StringType; nullable = true; defaultValue = null }
                    )
                ) { backStackEntry ->
                    val brewLogId = backStackEntry.arguments?.getString("brewLogId")
                    val recipeId = backStackEntry.arguments?.getString("recipeId")
                    BrewLogEditScreen(
                        brewLogId = brewLogId,
                        recipeId = recipeId,
                        onSaved = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }

                // ── Brew Guide routes ──
                composable(
                    route = Routes.BREW_PREPARE,
                    arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val recipeId = backStackEntry.arguments?.getString("recipeId") ?: return@composable
                    BrewPrepareScreen(
                        recipeId = recipeId,
                        onStart = { navController.navigate(Routes.brewGuide(recipeId)) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Routes.BREW_GUIDE,
                    arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val recipeId = backStackEntry.arguments?.getString("recipeId") ?: return@composable
                    BrewGuideScreen(
                        recipeId = recipeId,
                        onFinished = { navController.navigate(Routes.brewComplete(recipeId)) }
                    )
                }

                composable(
                    route = Routes.BREW_COMPLETE,
                    arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val recipeId = backStackEntry.arguments?.getString("recipeId") ?: return@composable
                    BrewCompleteScreen(
                        recipeId = recipeId,
                        onGoToRecord = { prefill ->
                            CoffeemarkApp.instance.brewGuidePrefillData = prefill
                            navController.navigate(Routes.brewLogEdit(recipeId = recipeId)) {
                                popUpTo(Routes.BREW_LOGS) { inclusive = false }
                            }
                        },
                        onBackToRecipes = {
                            navController.navigate(Routes.BREW_LOGS) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
