package com.coffeemark.app.ui.beans

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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeanDetailScreen(
    beanId: String,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
    viewModel: BeanDetailViewModel = viewModel(factory = BeanDetailViewModel.Factory(beanId))
) {
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.bean?.name ?: "豆子详情") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← 返回") } },
                actions = {
                    if (state.bean != null) {
                        TextButton(onClick = onEdit) { Text("编辑") }
                        TextButton(onClick = { showDeleteDialog = true }) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (state.bean != null) {
            val bean = state.bean!!

            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding)
                    .verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 名称 + 状态
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(bean.name, style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    StatusChip(bean.status)
                }

                // 核心参数
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailRow("净含量", "${bean.netWeight.toLong()}g")
                        DetailRow("当前剩余", "${bean.currentWeight.toLong()}g")
                        DetailRow("整包价格", "¥${bean.price}")
                        DetailRow("克单价", "¥${String.format("%.2f", bean.pricePerGram)}/g")
                        DetailRow("已使用价格", "¥${String.format("%.2f", bean.totalUsedPrice)}")
                        DetailRow("类型", bean.beanType.label)
                        bean.roastLevel?.let { DetailRow("烘焙程度", it.label) }
                        DetailRow("状态", bean.status.label)
                        if (bean.isEspresso) DetailRow("意式豆", "是")
                    }
                }

                // 产地信息
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        bean.origin?.let { DetailRow("产地", it) }
                        bean.process?.let { DetailRow("处理法", it) }
                        bean.varietal?.let { DetailRow("豆种", it) }
                        bean.altitude?.let { DetailRow("海拔", it) }
                        bean.roaster?.let { DetailRow("烘豆商", it) }
                        bean.estateStation?.let { DetailRow("庄园/处理站", it) }
                        bean.producer?.let { DetailRow("生产者", it) }
                        bean.batch?.let { DetailRow("批次", it) }
                    }
                }

                // 日期信息
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailRow("烘焙日期", dateFormat.format(Date(bean.roastDate)))
                        DetailRow("赏味期", "${bean.shelfLifeDays}天")
                    }
                }

                // 风味标签
                if (!bean.flavorTags.isNullOrEmpty()) {
                    Text("风味标签", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        bean.flavorTags.forEach { tag ->
                            SuggestionChip(onClick = {}, label = { Text(tag) })
                        }
                    }
                }

                // 备注
                bean.notes?.let {
                    Text("备注", style = MaterialTheme.typography.titleMedium)
                    Text(it, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("删除豆子") },
                text = { Text("确定删除「${state.bean?.name}」吗？") },
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
