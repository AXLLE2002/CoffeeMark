package com.coffeemark.app.ui.brewlogs

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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrewLogListScreen(
    onBrewLogClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    viewModel: BrewLogListViewModel = viewModel(factory = BrewLogListViewModel.Factory())
) {
    val state by viewModel.state.collectAsState()
    val dateFormat = remember { SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault()) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateClick, containerColor = MaterialTheme.colorScheme.primary) {
                Text("+", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // ── 日历卡片（始终显示）──
            item {
                BrewCalendarCard(
                    currentMonth = state.currentMonth,
                    brewDates = state.brewDatesInMonth,
                    isExpanded = state.isCalendarExpanded,
                    earliestBrewTime = state.earliestBrewTime,
                    onToggle = { viewModel.toggleCalendar() },
                    onPrevMonth = { viewModel.goToPrevMonth() },
                    onNextMonth = { viewModel.goToNextMonth() }
                )
                Spacer(Modifier.height(4.dp))
            }

            if (state.items.isEmpty()) {
                // 空状态
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📝", style = MaterialTheme.typography.displayLarge)
                            Spacer(Modifier.height(16.dp))
                            Text("还没有记录", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            Text("冲一杯咖啡，开始记录吧", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                items(state.items, key = { it.brewLog.id }) { item ->
                    var showDeleteDialog by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier.fillMaxWidth().animateContentSize()
                            .combinedClickable(
                                onClick = { onBrewLogClick(item.brewLog.id) },
                                onLongClick = { showDeleteDialog = true }
                            ),
                        shape = MaterialTheme.shapes.medium,
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // 第一行：豆种（大字体）
                            Text(item.beanName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)

                            Spacer(Modifier.height(4.dp))

                            // 第二行：时间 + 豆子价值
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(dateFormat.format(Date(item.brewLog.brewTime)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("¥${String.format("%.2f", item.beanUsedPrice)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Spacer(Modifier.height(2.dp))

                            // 第三行：克重 · 粉水比 · 水温
                            val ratioStr = if (item.brewLog.groundWeight > 0)
                                "1:${String.format("%.1f", item.brewLog.totalWater / item.brewLog.groundWeight)}"
                                else null
                            val infoParts = listOfNotNull(
                                "${item.brewLog.beanUsedWeight.toLong()}g",
                                ratioStr,
                                item.brewLog.waterTemp?.let { "${it}℃" }
                            )
                            Text(infoParts.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(Modifier.height(4.dp))

                            // 第四行：星数
                            Text(
                                "⭐".repeat(item.brewLog.rating),
                                style = MaterialTheme.typography.bodyLarge
                            )

                            // 第五行：感受
                            val hasTasting = !item.brewLog.tastingNotes.isNullOrBlank()
                            val hasImprovement = !item.brewLog.improvementNotes.isNullOrBlank()
                            if (hasTasting) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    item.brewLog.tastingNotes!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                            // 第六行：改进备注
                            if (hasImprovement) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "💡${item.brewLog.improvementNotes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("删除记录") },
                            text = { Text("确定删除此记录吗？") },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.delete(item.brewLog.id)
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
