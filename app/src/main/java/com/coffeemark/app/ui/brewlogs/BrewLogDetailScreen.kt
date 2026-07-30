package com.coffeemark.app.ui.brewlogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeemark.app.ui.components.DetailRow
import com.coffeemark.app.ui.components.DetailSection
import com.coffeemark.app.ui.components.ParamBadge
import com.coffeemark.app.util.TimeFormatUtil
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrewLogDetailScreen(
    brewLogId: String,
    onEdit: () -> Unit,
    onSaveAsRecipe: (String) -> Unit,  // 新方案ID
    onDeleted: () -> Unit,
    onBack: () -> Unit,
    viewModel: BrewLogDetailViewModel = viewModel(factory = BrewLogDetailViewModel.Factory(brewLogId))
) {
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault()) }

    // 另存为方案完成后跳转到新方案编辑页
    LaunchedEffect(state.savedAsRecipeId) {
        state.savedAsRecipeId?.let { onSaveAsRecipe(it) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        topBar = {
            TopAppBar(
                title = { Text("") },
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    if (state.brewLog != null) {
                        TextButton(onClick = onEdit) { Text("编辑") }
                        TextButton(onClick = { showDeleteDialog = true }) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            if (state.brewLog != null) {
                Surface(shadowElevation = 8.dp) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // 另存为方案
                        OutlinedButton(
                            onClick = { viewModel.saveAsRecipe() },
                            modifier = Modifier.weight(1f)
                        ) { Text("另存为方案") }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (state.brewLog != null) {
            val log = state.brewLog!!

            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. 评分 — 大数字 + 五角星
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${log.rating}",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Row {
                            repeat(5) { i ->
                                Icon(
                                    imageVector = if (i < log.rating) Icons.Filled.Star
                                                  else Icons.Outlined.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            log.ratingTag,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 2. 核心参数标签行
                val ratioStr = if (log.groundWeight > 0)
                    "1:${String.format("%.1f", log.totalWater / log.groundWeight)}" else null
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ParamBadge("${log.beanUsedWeight}g")
                    if (ratioStr != null) ParamBadge(ratioStr)
                    log.waterTemp?.let { ParamBadge("${it}℃") }
                    log.grindSize?.let { ParamBadge(it.label) }
                }

                // 3. 冲煮参数详情
                DetailSection(title = "冲煮参数") {
                    DetailRow("豆子", state.bean?.name ?: "未知")
                    DetailRow("用豆量", "${log.beanUsedWeight}g")
                    state.recipe?.let { DetailRow("方案", it.name) }
                        ?: log.customRecipeName?.let { DetailRow("方案", it) }
                    DetailRow("粉重", "${log.groundWeight}g")
                    DetailRow("注水量", "${log.totalWater}g")
                    log.waterTemp?.let { DetailRow("水温", "${it}℃") }
                    log.grinder?.let { DetailRow("磨豆机", it) }
                    log.grindSize?.let { DetailRow("研磨度", it.label) }
                    log.device?.let { DetailRow("器具", it) }
                    DetailRow("总耗时", TimeFormatUtil.formatDuration(log.totalDuration))
                }

                // 4. 环境信息
                if (log.location != null || log.weather != null || log.mood != null) {
                    DetailSection(title = "环境") {
                        log.location?.let { DetailRow("地点", it) }
                        log.weather?.let { DetailRow("天气", it) }
                        log.mood?.let { DetailRow("心情", it.label) }
                        DetailRow("冲煮时间", dateFormat.format(Date(log.brewTime)))
                    }
                }

                // 5. 感受
                log.tastingNotes?.let { notes ->
                    DetailSection(title = "感受") {
                        Text(notes, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // 6. 改进备注
                log.improvementNotes?.let { notes ->
                    DetailSection(title = "改进备注") {
                        Text(notes, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("删除记录") },
                text = { Text("确定删除此记录吗？\n（库存将自动回退）") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.delete(onDeleted)
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
