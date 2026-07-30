package com.coffeemark.app.ui.brewlogs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Star
import com.coffeemark.app.ui.theme.CoffeeCard
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BrewLogDayListScreen(
    dateMillis: Long,
    onBrewLogClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: BrewLogDayListViewModel = viewModel(factory = BrewLogDayListViewModel.Factory(dateMillis))
) {
    val state by viewModel.state.collectAsState()
    val dateFormat = remember { SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault()) }
    val dateLabel = remember(dateMillis) {
        val d = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        d.format(DateTimeFormatter.ofPattern("M月d日"))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f),
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        topBar = {
            TopAppBar(
                title = { Text("${dateLabel}的记录") },
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (state.items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.EditNote, null, Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    Spacer(Modifier.height(16.dp))
                    Text("这一天还没有记录", style = MaterialTheme.typography.titleLarge)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(state.items, key = { it.brewLog.id }) { item ->
                    var showDeleteDialog by remember { mutableStateOf(false) }

                    CoffeeCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onBrewLogClick(item.brewLog.id) },
                                onLongClick = { showDeleteDialog = true }
                            ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // 第一行：豆种（大字体加粗）+ 评分
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    item.beanName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    "${item.brewLog.rating}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(Modifier.height(4.dp))

                            // 第二行：时间 · 参数 · 价格（合并到一行）
                            val ratioStr = if (item.brewLog.groundWeight > 0)
                                "1:${String.format("%.1f", item.brewLog.totalWater / item.brewLog.groundWeight)}"
                                else null
                            val infoParts = listOfNotNull(
                                dateFormat.format(Date(item.brewLog.brewTime)),
                                "${item.brewLog.beanUsedWeight.toLong()}g",
                                ratioStr,
                                item.brewLog.waterTemp?.let { "${it}℃" },
                                "¥${String.format("%.2f", item.beanUsedPrice)}"
                            )
                            Text(
                                infoParts.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // 第三行：感受（如果有）
                            if (!item.brewLog.tastingNotes.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    item.brewLog.tastingNotes.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            // 第四行：改进备注（Lightbulb 提示）
                            if (!item.brewLog.improvementNotes.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Lightbulb, null, Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        item.brewLog.improvementNotes.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("删除记录") },
                            text = { Text("确定删除此记录吗？\n（库存将自动回退）") },
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
