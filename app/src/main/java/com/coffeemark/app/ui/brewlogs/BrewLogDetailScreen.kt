package com.coffeemark.app.ui.brewlogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeemark.app.ui.recipes.DetailRow
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
        topBar = {
            TopAppBar(
                title = { Text("记录详情") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← 返回") } },
                actions = {
                    if (state.brewLog != null) {
                        TextButton(onClick = onEdit) { Text("编辑") }
                        TextButton(onClick = { showDeleteDialog = true }) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
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
                    .verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 评分
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⭐".repeat(log.rating), style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(log.ratingTag, style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                }

                // 核心信息卡片
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                }

                // 环境信息
                if (log.location != null || log.weather != null || log.mood != null) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            log.location?.let { DetailRow("地点", it) }
                            log.weather?.let { DetailRow("天气", it) }
                            log.mood?.let { DetailRow("心情", it.label) }
                            DetailRow("冲煮时间", dateFormat.format(Date(log.brewTime)))
                        }
                    }
                }

                // 感受
                log.tastingNotes?.let { notes ->
                    Text("感受", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Text(notes, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // 改进
                log.improvementNotes?.let { notes ->
                    Text("改进备注", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Text(notes, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(16.dp))
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
