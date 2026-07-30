package com.coffeemark.app.ui.recipes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FreeBreakfast
import com.coffeemark.app.ui.theme.CoffeeCard
import com.coffeemark.app.util.TimeFormatUtil
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecipeListScreen(
    onRecipeClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    viewModel: RecipeListViewModel = viewModel(factory = RecipeListViewModel.Factory())
) {
    val recipes by viewModel.recipes.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f),
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加")
            }
        }
    ) { innerPadding ->
        if (recipes.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.FreeBreakfast, null, Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("还没有方案", style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("创建你的第一个冲煮方案吧", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(recipes, key = { it.recipe.id }) { item ->
                    val visibility = remember { MutableTransitionState(false) }.apply {
                        targetState = true
                    }
                    AnimatedVisibility(
                        visibleState = visibility,
                        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { 20 }
                    ) {
                        var showDeleteDialog by remember { mutableStateOf(false) }
                        val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

                        CoffeeCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                                .combinedClickable(
                                    onClick = { onRecipeClick(item.recipe.id) },
                                    onLongClick = { showDeleteDialog = true }
                                ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .padding(14.dp)
                            ) {
                                // 方案名
                                Text(
                                    text = item.recipe.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // 核心参数一行
                                val ratioStr = if (item.recipe.beanWeight > 0)
                                    "1:${String.format("%.1f", item.recipe.totalWater / item.recipe.beanWeight)}"
                                    else null
                                val paramParts = listOfNotNull(
                                    "${item.recipe.beanWeight.toLong()}g",
                                    ratioStr,
                                    "${item.recipe.waterTemp}℃"
                                )
                                Text(
                                    text = paramParts.joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // 弹性占位：把底部「器具·时间」行顶到卡片底边，保证卡片高度统一
                                Spacer(modifier = Modifier.weight(1f))

                                // 底部：器具 + 时间
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        item.recipe.device,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        dateFormat.format(Date(item.recipe.createdAt)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                title = { Text("删除方案") },
                                text = { Text("确定删除「${item.recipe.name}」吗？此操作不可撤销。") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.deleteRecipe(item.recipe.id)
                                        showDeleteDialog = false
                                    }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
