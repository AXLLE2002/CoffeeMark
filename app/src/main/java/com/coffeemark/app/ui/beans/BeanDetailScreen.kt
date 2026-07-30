package com.coffeemark.app.ui.beans

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeemark.app.ui.components.DetailRow
import com.coffeemark.app.ui.components.DetailSection
import com.coffeemark.app.ui.components.MetricTile
import com.coffeemark.app.ui.components.ParamBadge
import com.coffeemark.app.ui.components.StatusChip
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
                    if (state.bean != null) {
                        TextButton(onClick = onEdit) { Text("编辑") }
                        TextButton(onClick = { showDeleteDialog = true }) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. 名称（大字号）+ 状态 Chip
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        bean.name,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    StatusChip(bean.status)
                }

                // 2. 核心指标行 — 用 MetricTile
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MetricTile(
                            value = "${bean.currentWeight.toLong()}g",
                            label = "当前剩余",
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            value = "¥${bean.price}",
                            label = "整包价格",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 3. 参数标签行（ParamBadge）
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ParamBadge(bean.beanType.label)
                    bean.roastLevel?.let { ParamBadge(it.label) }
                    if (bean.isEspresso) ParamBadge("意式")
                }

                // 4. 关键参数
                DetailSection(title = "核心参数") {
                    DetailRow("净含量", "${bean.netWeight.toLong()}g")
                    DetailRow("克单价", "¥${String.format("%.2f", bean.pricePerGram)}/g")
                    DetailRow("已使用价格", "¥${String.format("%.2f", bean.totalUsedPrice)}")
                }

                // 5. 产地信息
                DetailSection(title = "产地信息") {
                    bean.origin?.let { DetailRow("产地", it) }
                    bean.process?.let { DetailRow("处理法", it) }
                    bean.varietal?.let { DetailRow("豆种", it) }
                    bean.altitude?.let { DetailRow("海拔", it) }
                    bean.roaster?.let { DetailRow("烘豆商", it) }
                    bean.estateStation?.let { DetailRow("庄园/处理站", it) }
                    bean.producer?.let { DetailRow("生产者", it) }
                    bean.batch?.let { DetailRow("批次", it) }
                }

                // 6. 日期信息
                DetailSection(title = "日期") {
                    DetailRow("烘焙日期", dateFormat.format(Date(bean.roastDate)))
                    DetailRow("赏味期", "${bean.shelfLifeDays}天")
                }

                // 7. 风味标签
                if (!bean.flavorTags.isNullOrEmpty()) {
                    Text(
                        "风味标签",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        bean.flavorTags.forEach { tag ->
                            SuggestionChip(onClick = {}, label = { Text(tag) })
                        }
                    }
                }

                // 8. 备注
                bean.notes?.let {
                    Spacer(Modifier.height(4.dp))
                    DetailSection(title = "备注") {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))
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
