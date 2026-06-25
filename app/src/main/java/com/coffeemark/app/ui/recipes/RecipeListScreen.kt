package com.coffeemark.app.ui.recipes

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text("+", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary)
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
                    Text("☕", style = MaterialTheme.typography.displayLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("还没有方案", style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("创建你的第一个冲煮方案吧", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(recipes, key = { it.recipe.id }) { item ->
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .combinedClickable(
                                onClick = { onRecipeClick(item.recipe.id) },
                                onLongClick = { showDeleteDialog = true }
                            ),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            // 第一行：方案名（大字体加粗）
                            Text(
                                text = item.recipe.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // 第二行：用豆量 · 粉水比 · 水温 · 研磨度
                            val ratioStr = if (item.recipe.beanWeight > 0)
                                "1:${String.format("%.1f", item.recipe.totalWater / item.recipe.beanWeight)}"
                                else null
                            val infoParts = listOfNotNull(
                                "${item.recipe.beanWeight.toLong()}g",
                                ratioStr,
                                "${item.recipe.waterTemp}℃",
                                item.recipe.grindSize.label
                            )
                            Text(
                                text = infoParts.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // 第三行：总时长 + 创建时间
                            Text(
                                text = "${TimeFormatUtil.formatDuration(item.totalDuration)} · ${dateFormat.format(Date(item.recipe.createdAt))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
