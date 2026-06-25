package com.coffeemark.app.ui.beans

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeemark.app.data.enums.BeanStatus
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val PIE_COLORS = listOf(
    Color(0xFF6D4C41), // 咖啡棕
    Color(0xFFD7CCC8), // 浅棕
    Color(0xFF8D6E63), // 中棕
    Color(0xFFFF8A65), // 橙
    Color(0xFFFFAB91), // 浅橙
    Color(0xFFA1887F), // 灰棕
    Color(0xFFBCAAA4), // 米棕
    Color(0xFF4E342E), // 深咖
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BeanListScreen(
    onBeanClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    viewModel: BeanListViewModel = viewModel(factory = BeanListViewModel.Factory())
) {
    val state by viewModel.state.collectAsState()
    val monthFormatter = remember { DateTimeFormatter.ofPattern("yyyy年M月") }

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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // ── 顶部汇总 ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SummaryItem(
                            label = "剩余",
                            value = "${state.totalRemainingWeight.toLong().coerceAtLeast(0)}g"
                        )
                        SummaryItem(
                            label = "剩余价格",
                            value = "¥${String.format("%.0f", state.totalRemainingPrice)}"
                        )
                        SummaryItem(
                            label = "已用价格",
                            value = "¥${String.format("%.0f", state.totalUsedPrice)}"
                        )
                    }
                }
            }

            // ── 月度使用饼图（始终显示）──
            item {
                BeanUsagePieCard(
                    selectedMonth = state.selectedMonth,
                    monthFormatter = monthFormatter,
                    beanUsage = state.beanUsage,
                    totalWeight = state.usageTotalWeight,
                    onSelectMonth = { viewModel.selectMonth(it) }
                )
            }

            // ── 豆子列表或空状态 ──
            if (state.beans.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🫘", style = MaterialTheme.typography.displayLarge)
                            Spacer(Modifier.height(16.dp))
                            Text("还没有豆子", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            Text("添加你的第一包咖啡豆吧", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(state.beans, key = { it.id }) { bean ->
                    var showDeleteDialog by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .combinedClickable(
                                onClick = { onBeanClick(bean.id) },
                                onLongClick = { showDeleteDialog = true }
                            ),
                        shape = MaterialTheme.shapes.medium,
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusChip(bean.status)
                                Spacer(Modifier.width(8.dp))
                                Text(bean.name, style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                bean.roastLevel?.let {
                                    Text(it.label, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Spacer(Modifier.height(6.dp))

                            if (!bean.flavorTags.isNullOrEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    bean.flavorTags.take(3).forEach { tag ->
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.height(24.dp)
                                        )
                                    }
                                    if (bean.flavorTags.size > 3) {
                                        Text("+${bean.flavorTags.size - 3}", style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                            }

                            val subInfo = listOfNotNull(bean.origin, bean.varietal).joinToString(" · ")
                            if (subInfo.isNotBlank()) {
                                Text(subInfo, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("剩余 ${bean.currentWeight.toLong().coerceAtLeast(0)}g",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium)
                                Text("¥${String.format("%.2f", bean.pricePerGram)}/g",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("删除豆子") },
                            text = { Text("确定删除「${bean.name}」吗？") },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.deleteBean(bean.id)
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

// ── 月度饼图卡片 ──
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeanUsagePieCard(
    selectedMonth: YearMonth,
    monthFormatter: DateTimeFormatter,
    beanUsage: List<BeanUsageItem>,
    totalWeight: Double,
    onSelectMonth: (YearMonth) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行 + 月份选择
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📊 豆子用量分布", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))

                var expanded by remember { mutableStateOf(false) }
                Box {
                    AssistChip(
                        onClick = { expanded = true },
                        label = { Text(selectedMonth.format(monthFormatter), style = MaterialTheme.typography.labelSmall) },
                        trailingIcon = {
                            Text(if (expanded) "▲" else "▼", style = MaterialTheme.typography.labelSmall)
                        }
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        val now = YearMonth.now()
                        for (m in (0..11).reversed()) {
                            val month = now.minusMonths(m.toLong())
                            DropdownMenuItem(
                                text = { Text(month.format(monthFormatter)) },
                                onClick = { onSelectMonth(month); expanded = false }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (beanUsage.isEmpty() || totalWeight <= 0) {
                // 无数据时显示占位
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📊", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("本月暂无冲煮记录", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧饼图
                    Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(140.dp)) {
                            val strokeWidth = 28.dp.toPx()
                            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                            var startAngle = -90f

                            beanUsage.forEachIndexed { i, item ->
                                val sweepAngle = item.fraction * 360f
                                drawArc(
                                    color = PIE_COLORS[i % PIE_COLORS.size],
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                                startAngle += sweepAngle
                            }
                        }

                        // 中心总计
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${totalWeight.toLong()}g", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            Text("总计", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    // 右侧图例
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        beanUsage.take(5).forEachIndexed { i, item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(10.dp),
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = PIE_COLORS[i % PIE_COLORS.size]
                                ) {}
                                Spacer(Modifier.width(6.dp))
                                Text(item.beanName, style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${item.usedWeight.toLong()}g", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (beanUsage.size > 5) {
                            Text("+${beanUsage.size - 5} 种豆子", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: BeanStatus) {
    val (bg, text) = when (status) {
        BeanStatus.UNOPENED -> MaterialTheme.colorScheme.tertiaryContainer to "未开封"
        BeanStatus.OPENED -> MaterialTheme.colorScheme.secondaryContainer to "已开封"
        BeanStatus.USED_UP -> MaterialTheme.colorScheme.surfaceVariant to "已用完"
    }
    Surface(shape = MaterialTheme.shapes.small, color = bg) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
