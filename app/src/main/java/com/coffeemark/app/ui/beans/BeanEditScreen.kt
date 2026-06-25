package com.coffeemark.app.ui.beans

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeemark.app.data.enums.BeanStatus
import com.coffeemark.app.data.enums.BeanType
import com.coffeemark.app.data.enums.RoastLevel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*

val PRESET_FLAVOR_TAGS = listOf("水果", "花香", "坚果", "巧克力", "焦糖", "香料", "发酵", "草本", "茶感", "酒感")
val SHELF_LIFE_QUICK = listOf(7, 14, 21, 30)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeanEditScreen(
    beanId: String? = null,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: BeanEditViewModel = viewModel(factory = BeanEditViewModel.Factory(beanId))
) {
    val state by viewModel.state.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    LaunchedEffect(state.isSaved) { if (state.isSaved) onSaved() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "编辑豆子" else "添加豆子") },
                navigationIcon = { TextButton(onClick = onBack) { Text("取消") } },
                actions = {
                    TextButton(onClick = { viewModel.save() }, enabled = !state.isSaving) {
                        Text("保存", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (state.error != null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(state.error!!, modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            // 基本信息
            item {
                OutlinedTextField(value = state.name, onValueChange = viewModel::updateName,
                    label = { Text("名称 *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }

            item {
                var netWeightText by remember(state.netWeight) {
                    mutableStateOf(if (state.netWeight == 0.0) "" else state.netWeight.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() })
                }
                var currentWeightText by remember(state.currentWeight) {
                    mutableStateOf(if (state.currentWeight == 0.0) "" else state.currentWeight.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = netWeightText,
                        onValueChange = { input ->
                            netWeightText = input
                            input.toDoubleOrNull()?.let(viewModel::updateNetWeight)
                        },
                        label = { Text("净含量 (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = currentWeightText,
                        onValueChange = { input ->
                            currentWeightText = input
                            input.toDoubleOrNull()?.let(viewModel::updateCurrentWeight)
                        },
                        label = { Text("当前剩余 (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                }
            }

            item {
                var priceText by remember(state.price) {
                    mutableStateOf(if (state.price == 0.0) "" else state.price.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() })
                }
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { input ->
                        priceText = input
                        input.toDoubleOrNull()?.let(viewModel::updatePrice)
                    },
                    label = { Text("整包价格 (元)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }

            // 克单价实时显示
            if (state.netWeight > 0) {
                item {
                    Text("克单价：¥${String.format("%.2f", state.price / state.netWeight)}/g",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // 状态
            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = state.status.label, onValueChange = {}, readOnly = true,
                        label = { Text("状态") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        BeanStatus.entries.forEach { s ->
                            DropdownMenuItem(text = { Text(s.label) },
                                onClick = { viewModel.updateStatus(s); expanded = false })
                        }
                    }
                }
            }

            // 豆子类型
            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = state.beanType.label, onValueChange = {}, readOnly = true,
                        label = { Text("豆子类型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        BeanType.entries.forEach { bt ->
                            DropdownMenuItem(text = { Text(bt.label) },
                                onClick = { viewModel.updateBeanType(bt); expanded = false })
                        }
                    }
                }
            }

            // 烘焙程度
            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = state.roastLevel?.label ?: "不设置", onValueChange = {}, readOnly = true,
                        label = { Text("烘焙程度") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("不设置") },
                            onClick = { viewModel.updateRoastLevel(null); expanded = false })
                        RoastLevel.entries.forEach { rl ->
                            DropdownMenuItem(text = { Text(rl.label) },
                                onClick = { viewModel.updateRoastLevel(rl); expanded = false })
                        }
                    }
                }
            }

            // 产地信息
            item {
                OutlinedTextField(value = state.origin ?: "", onValueChange = { viewModel.updateOrigin(it.ifBlank { null }) },
                    label = { Text("产地") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(value = state.process ?: "", onValueChange = { viewModel.updateProcess(it.ifBlank { null }) },
                    label = { Text("处理法") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(value = state.varietal ?: "", onValueChange = { viewModel.updateVarietal(it.ifBlank { null }) },
                    label = { Text("豆种") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }

            // 烘豆商
            item {
                OutlinedTextField(value = state.roaster ?: "", onValueChange = { viewModel.updateRoaster(it.ifBlank { null }) },
                    label = { Text("烘豆商") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }

            // 庄园/处理站
            item {
                OutlinedTextField(value = state.estateStation ?: "", onValueChange = { viewModel.updateEstateStation(it.ifBlank { null }) },
                    label = { Text("庄园/处理站") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }

            // 生产者
            item {
                OutlinedTextField(value = state.producer ?: "", onValueChange = { viewModel.updateProducer(it.ifBlank { null }) },
                    label = { Text("生产者") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }

            // 批次
            item {
                OutlinedTextField(value = state.batch ?: "", onValueChange = { viewModel.updateBatch(it.ifBlank { null }) },
                    label = { Text("批次") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }

            // 海拔
            item {
                OutlinedTextField(value = state.altitude ?: "", onValueChange = { viewModel.updateAltitude(it.ifBlank { null }) },
                    label = { Text("海拔范围") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }

            // 烘焙日期
            item {
                var showDatePicker by remember { mutableStateOf(false) }

                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.roastDate)
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val pickedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                                    val newTimestamp = pickedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                    viewModel.updateRoastDate(newTimestamp)
                                }
                                showDatePicker = false
                            }) { Text("确定") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("取消") }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    onClick = { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("烘焙日期", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(2.dp))
                            Text(dateFormat.format(Date(state.roastDate)),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium)
                        }
                        Text("修改", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // 赏味期
            item {
                Text("赏味期（天）", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SHELF_LIFE_QUICK.forEach { days ->
                        FilterChip(selected = state.shelfLifeDays == days,
                            onClick = { viewModel.updateShelfLife(days) },
                            label = { Text("${days}天") })
                    }
                }
                Spacer(Modifier.height(4.dp))
                var shelfLifeText by remember(state.shelfLifeDays) {
                    mutableStateOf(if (state.shelfLifeDays == 0) "" else state.shelfLifeDays.toString())
                }
                OutlinedTextField(
                    value = shelfLifeText,
                    onValueChange = { input ->
                        shelfLifeText = input
                        input.toIntOrNull()?.let(viewModel::updateShelfLife)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }

            // 意式豆标记
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = state.isEspresso,
                        onCheckedChange = viewModel::updateIsEspresso)
                    Text("意式豆", style = MaterialTheme.typography.bodyLarge)
                }
            }

            // 风味标签卡片
            item {
                var expanded by remember { mutableStateOf(state.flavorTags.isEmpty()) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // ── 头部：点击展开/收起 ──
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("风味标签", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            if (state.flavorTags.isNotEmpty()) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text("${state.flavorTags.size}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Spacer(Modifier.width(8.dp))
                            }
                            TextButton(onClick = { expanded = !expanded }) {
                                Text(if (expanded) "收起 ▲" else "展开 ▼", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        // ── 已选标签（始终显示）──
                        if (state.flavorTags.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                state.flavorTags.forEach { tag ->
                                    InputChip(
                                        selected = true,
                                        onClick = { viewModel.removeFlavorTag(tag) },
                                        label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                                        trailingIcon = { Text("×") },
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }
                        }

                        // ── 展开区域 ──
                        if (expanded) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(12.dp))

                            // 预设风味选择
                            Text("预设风味", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                PRESET_FLAVOR_TAGS.chunked(3).forEach { row ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        row.forEach { tag ->
                                            val selected = tag in state.flavorTags
                                            FilterChip(
                                                selected = selected,
                                                onClick = { viewModel.toggleFlavorTag(tag) },
                                                label = {
                                                    Text(tag, style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = 2.dp))
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        // 补位占位
                                        repeat(3 - row.size) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // 自定义风味输入
                            var customTag by remember { mutableStateOf("") }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customTag,
                                    onValueChange = { customTag = it },
                                    label = { Text("自定义风味") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        viewModel.addCustomFlavorTag(customTag)
                                        customTag = ""
                                    },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Text("添加")
                                }
                            }
                        }
                    }
                }
            }

            // 备注
            item {
                OutlinedTextField(value = state.notes ?: "", onValueChange = { viewModel.updateNotes(it.ifBlank { null }) },
                    label = { Text("备注") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
