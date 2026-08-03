package com.coffeemark.app.ui.beans

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CoffeeMaker
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Reorder
import com.coffeemark.app.ui.theme.CoffeeCard
import com.coffeemark.app.ui.components.MetricTile
import com.coffeemark.app.data.enums.BeanStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BeanListScreen(
    onBeanClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    viewModel: BeanListViewModel = viewModel(factory = BeanListViewModel.Factory())
) {
    val state by viewModel.state.collectAsState()
    val monthFormatter = remember { DateTimeFormatter.ofPattern("yyyy年M月") }

    // 排序弹层状态
    var showReorder by remember { mutableStateOf(false) }
    var sheetIds by remember { mutableStateOf<List<String>>(emptyList()) }
    val sheetListState = rememberLazyListState()
    var sheetDraggedId by remember { mutableStateOf<String?>(null) }
    var sheetDragOffsetY by remember { mutableStateOf(0f) }
    val autoScrollSpeed = remember { mutableStateOf(0f) }
    val beanMap = remember(state.beans) { state.beans.associateBy { it.id } }

    val commitAndClose: () -> Unit = {
        viewModel.saveOrder(sheetIds)
        showReorder = false
    }
    val dismissSheet: () -> Unit = {
        showReorder = false
    }

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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // ── 顶部汇总 ──
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MetricTile(
                            value = "${state.totalRemainingWeight.toLong().coerceAtLeast(0)}g",
                            label = "剩余总量",
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            value = "¥${String.format("%.0f", state.totalRemainingPrice)}",
                            label = "剩余价值",
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            value = "¥${String.format("%.0f", state.totalUsedPrice)}",
                            label = "已用价值",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── 月度使用饼图 ──
            item {
                BeanUsagePieCard(
                    selectedMonth = state.selectedMonth,
                    monthFormatter = monthFormatter,
                    beanUsage = state.beanUsage,
                    totalWeight = state.usageTotalWeight,
                    earliestBrewMonth = state.earliestBrewMonth,
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
                            Icon(
                                Icons.Filled.CoffeeMaker,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("还没有豆子", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            Text("添加你的第一包咖啡豆吧", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                // 排序标题 + 排序按钮
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("我的豆仓", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            sheetIds = state.beans.map { it.id }
                            showReorder = true
                        }) {
                            Text("排序", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                items(state.beans, key = { it.id }) { bean ->
                    CoffeeCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBeanClick(bean.id) },
                        shape = MaterialTheme.shapes.medium
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

                            val remainingFraction =
                                (bean.currentWeight / bean.netWeight).toFloat().coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { remainingFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant,
                            )
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
                }
            }
        }
    }

    // ── 排序弹层：半屏底部弹层，暗色遮罩，原页面顶部仍可见 ──
    if (showReorder) {
        Box(
            Modifier
                .fillMaxSize()
                .zIndex(10f)
        ) {
            // 暗色遮罩：原页面可见但变暗，点击取消排序
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
                    .clickable { dismissSheet() }
            )
            // 半屏弹层：从底部弹出，原页面顶部 38% 仍可见
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shape = BottomSheetDefaults.ExpandedShape,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.62f)
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "提示：请长按每一个豆条进行拖拽排序",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 拖拽时持续边缘自动滚动：速度随手指进入边缘深度平滑增长，
                // 用独立协程循环驱动（而非每次 onDrag 事件各 scroll 一次，那样会瞬移）。
                LaunchedEffect(showReorder) {
                    if (!showReorder) return@LaunchedEffect
                    while (true) {
                        val speed = autoScrollSpeed.value
                        if (speed != 0f) {
                            val consumed = sheetListState.scrollBy(speed)
                            // 自动滚动会移动内容，若不补偿被拖豆条会脱离手指并触发失控滚动；
                            // 把已滚动的位移补回拖拽偏移，使豆条始终钉在手指下方。
                            sheetDragOffsetY += consumed
                        }
                        delay(16)
                    }
                }

                LazyColumn(
                    state = sheetListState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(items = sheetIds, key = { it -> it }) { id ->
                        val bean = beanMap[id] ?: return@items
                        val isDragging = sheetDraggedId == id
                        CoffeeCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer {
                                    translationY = if (isDragging) sheetDragOffsetY else 0f
                                    shadowElevation = if (isDragging) 8.dp.toPx() else 0f
                                }
                                .pointerInput(id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { sheetDraggedId = id },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val draggedId = sheetDraggedId ?: return@detectDragGesturesAfterLongPress
                                            val list = sheetIds
                                            val from = list.indexOf(draggedId)
                                            if (from !in list.indices) return@detectDragGesturesAfterLongPress
                                            // 手指位移直接累加，拖动的豆条 1:1 跟手
                                            sheetDragOffsetY += dragAmount.y

                                            val layoutInfo = sheetListState.layoutInfo
                                            val draggedInfo = layoutInfo.visibleItemsInfo
                                                .firstOrNull { it.key == draggedId }
                                                ?: return@detectDragGesturesAfterLongPress
                                            val itemSize = draggedInfo.size
                                            val centerY = draggedInfo.offset + sheetDragOffsetY + itemSize / 2f

                                            // 找到手指中心当前落在哪个条目区间内，跨过则交换
                                            val target = layoutInfo.visibleItemsInfo
                                                .firstOrNull { centerY >= it.offset && centerY <= it.offset + it.size }
                                            if (target != null && target.key != draggedId) {
                                                val to = list.indexOf(target.key)
                                                if (to != -1 && to != from) {
                                                    val oldDraggedOffset = draggedInfo.offset
                                                    val oldTargetOffset = target.offset
                                                    sheetIds = sheetIds.toMutableList().also { l -> l.add(to, l.removeAt(from)) }
                                                    // 交换后保持被拖豆条视觉位置不变（仍在手指下方）
                                                    sheetDragOffsetY -= (oldTargetOffset - oldDraggedOffset)
                                                }
                                            }

                                            // 靠近视口上下边缘时平滑自动滚动，越界越深越快
                                            val vp = layoutInfo
                                            val threshold = 90f
                                            val maxSpeed = 16f
                                            val overStart = (vp.viewportStartOffset + threshold) - centerY
                                            val overEnd = centerY - (vp.viewportEndOffset - threshold)
                                            autoScrollSpeed.value = when {
                                                overStart > 0f -> -(min(overStart, maxSpeed))
                                                overEnd > 0f -> min(overEnd, maxSpeed)
                                                else -> 0f
                                            }
                                        },
                                        onDragEnd = {
                                            sheetDraggedId = null
                                            sheetDragOffsetY = 0f
                                            autoScrollSpeed.value = 0f
                                        },
                                        onDragCancel = {
                                            sheetDraggedId = null
                                            sheetDragOffsetY = 0f
                                            autoScrollSpeed.value = 0f
                                        }
                                    )
                                },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Reorder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(bean.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                                    Text(
                                        "剩余 ${bean.currentWeight.toLong().coerceAtLeast(0)}g",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { commitAndClose() },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("保存当前排序")
                    }
                    TextButton(
                        onClick = { dismissSheet() },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("取消")
                    }
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
    earliestBrewMonth: YearMonth?,
    onSelectMonth: (YearMonth) -> Unit
) {
    CoffeeCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行 + 月份选择
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.PieChart,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
                Text("豆子用量分布", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))

                var expanded by remember { mutableStateOf(false) }
                Box {
                    AssistChip(
                        onClick = { expanded = true },
                        label = { Text(selectedMonth.format(monthFormatter), style = MaterialTheme.typography.labelSmall) },
                        trailingIcon = {
                            Icon(
                                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp
                                               else Icons.Filled.KeyboardArrowDown,
                                contentDescription = if (expanded) "收起" else "展开",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        val now = YearMonth.now()
                        // 只从首条记录所在月到当前月，不展示无记录的月份
                        val start = earliestBrewMonth ?: now.minusMonths(11)
                        var m = now
                        while (!m.isBefore(start)) {
                            val month = m
                            DropdownMenuItem(
                                text = { Text(month.format(monthFormatter)) },
                                onClick = { onSelectMonth(month); expanded = false }
                            )
                            m = m.minusMonths(1)
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
                        Icon(
                            Icons.Filled.PieChart,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("本月暂无冲煮记录", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Top
                ) {
                    // 左侧饼图（160dp，居中显示）
                    Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(160.dp)) {
                            val strokeWidth = 32.dp.toPx()
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

                    // 右侧图例（可滚动，展示全部豆子，不再截断为 "+N 种豆子"）
                    Column(
                        modifier = Modifier
                            .widthIn(max = 180.dp)
                            .heightIn(max = 160.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        beanUsage.forEachIndexed { i, item ->
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

