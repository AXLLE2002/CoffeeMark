package com.coffeemark.app.ui.brewlogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeemark.app.data.enums.GrindSize
import com.coffeemark.app.data.enums.Mood
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val RATING_LABELS = listOf("不太满意", "一般", "好喝", "非常好喝", "超级好喝")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrewLogEditScreen(
    brewLogId: String? = null,
    recipeId: String? = null,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: BrewLogEditViewModel = viewModel(
        factory = BrewLogEditViewModel.Factory(brewLogId, recipeId)
    )
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isSaved) { if (state.isSaved) onSaved() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "编辑记录" else "新建记录") },
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

            // ── 豆子选择（必填）──
            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = state.selectedBean?.let { "${it.name} (剩余${it.currentWeight.toLong().coerceAtLeast(0)}g)" } ?: "请选择豆子 *",
                        onValueChange = {}, readOnly = true,
                        label = { Text("豆子 *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        state.availableBeans.forEach { bean ->
                            DropdownMenuItem(
                                text = { Text("${bean.name} (剩余${bean.currentWeight.toLong().coerceAtLeast(0)}g)") },
                                onClick = { viewModel.selectBean(bean); expanded = false }
                            )
                        }
                    }
                }
            }

            item {
                var weightText by remember(state.beanUsedWeight) {
                    mutableStateOf(if (state.beanUsedWeight == 0.0) "" else state.beanUsedWeight.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() })
                }
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { input ->
                        weightText = input
                        input.toDoubleOrNull()?.let(viewModel::updateWeight)
                    },
                    label = { Text("用豆量 (g) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }

            // ── 方案（可选：下拉选择 / 手动输入 / 不选）──
            item {
                var recipeExpanded by remember { mutableStateOf(false) }
                var customNameText by remember(state.customRecipeName) {
                    mutableStateOf(state.customRecipeName ?: "")
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("关联方案", style = MaterialTheme.typography.titleMedium)

                    // 选项一：从已有方案中选择
                    ExposedDropdownMenuBox(
                        expanded = recipeExpanded,
                        onExpandedChange = { recipeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = state.recipe?.name ?: "不关联方案",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("选择已有方案") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recipeExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = recipeExpanded,
                            onDismissRequest = { recipeExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("不关联方案") },
                                onClick = { viewModel.clearRecipeSelection(); recipeExpanded = false }
                            )
                            state.availableRecipes.forEach { recipe ->
                                DropdownMenuItem(
                                    text = { Text("${recipe.name} (${recipe.device} · ${recipe.totalWater}g)") },
                                    onClick = { viewModel.selectRecipe(recipe); recipeExpanded = false }
                                )
                            }
                        }
                    }

                    // 选项二：手动输入方案名称（与下拉互斥）
                    OutlinedTextField(
                        value = customNameText,
                        onValueChange = { input ->
                            customNameText = input
                            viewModel.updateCustomRecipeName(input)
                        },
                        label = { Text("或手动输入方案名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.recipeId == null
                    )

                    // 已选方案参数摘要
                    if (state.recipe != null) {
                        Card(colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("📋", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(state.recipe!!.name, fontWeight = FontWeight.Medium)
                                    Text("${state.recipe!!.device} · ${state.recipe!!.waterTemp}℃ · ${state.recipe!!.totalWater}g",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            // ── 冲煮时间 ──
            item {
                TimePickerCard(
                    brewTimeMs = state.brewTime,
                    onBrewTimeChanged = viewModel::updateBrewTime
                )
            }

            // ── 冲煮参数 ──
            item {
                var groundText by remember(state.groundWeight) {
                    mutableStateOf(if (state.groundWeight == 0.0) "" else state.groundWeight.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() })
                }
                var waterText by remember(state.totalWater) {
                    mutableStateOf(if (state.totalWater == 0.0) "" else state.totalWater.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = groundText,
                        onValueChange = { input ->
                            groundText = input
                            input.toDoubleOrNull()?.let(viewModel::updateGroundWeight)
                        },
                        label = { Text("粉重 (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = waterText,
                        onValueChange = { input ->
                            waterText = input
                            input.toDoubleOrNull()?.let(viewModel::updateTotalWater)
                        },
                        label = { Text("注水量 (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                }
            }

            item {
                var tempText by remember(state.waterTemp) {
                    mutableStateOf(state.waterTemp?.toString() ?: "")
                }
                OutlinedTextField(
                    value = tempText,
                    onValueChange = { input ->
                        tempText = input
                        viewModel.updateWaterTemp(input.toIntOrNull())
                    },
                    label = { Text("水温 (℃)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = state.grinder ?: "",
                    onValueChange = { viewModel.updateGrinder(it) },
                    label = { Text("磨豆机") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                // 历史建议
                if (state.grinderSuggestions.isNotEmpty() && state.grinder == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        state.grinderSuggestions.take(5).forEach { s ->
                            SuggestionChip(onClick = { viewModel.updateGrinder(s) },
                                label = { Text(s, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                }
            }

            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = state.grindSize?.label ?: "不设置", onValueChange = {}, readOnly = true,
                        label = { Text("研磨度") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("不设置") },
                            onClick = { viewModel.updateGrindSize(null); expanded = false })
                        GrindSize.entries.forEach { gs ->
                            DropdownMenuItem(text = { Text(gs.label) },
                                onClick = { viewModel.updateGrindSize(gs); expanded = false })
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = state.device ?: "",
                    onValueChange = { viewModel.updateDevice(it) },
                    label = { Text("器具") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                if (state.deviceSuggestions.isNotEmpty() && state.device == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        state.deviceSuggestions.take(5).forEach { s ->
                            SuggestionChip(onClick = { viewModel.updateDevice(s) },
                                label = { Text(s, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.location ?: "",
                        onValueChange = { viewModel.updateLocation(it) },
                        label = { Text("地点") }, singleLine = true, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = state.weather ?: "",
                        onValueChange = { viewModel.updateWeather(it) },
                        label = { Text("天气") }, singleLine = true, modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                var durationText by remember(state.totalDuration) {
                    mutableStateOf(if (state.totalDuration == 0) "" else state.totalDuration.toString())
                }
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { input ->
                        durationText = input
                        input.toIntOrNull()?.let(viewModel::updateTotalDuration)
                    },
                    label = { Text("总耗时 (秒)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }

            // ── 评价 ──
            item {
                Text("评分", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { star ->
                        TextButton(onClick = { viewModel.updateRating(star) },
                            modifier = Modifier.size(48.dp)) {
                            Text(if (star <= state.rating) "⭐" else "☆",
                                style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                    Text(RATING_LABELS[state.rating - 1],
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterVertically))
                }
            }

            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = state.mood?.label ?: "不设置", onValueChange = {}, readOnly = true,
                        label = { Text("心情") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("不设置") },
                            onClick = { viewModel.updateMood(null); expanded = false })
                        Mood.entries.forEach { m ->
                            DropdownMenuItem(text = { Text(m.label) },
                                onClick = { viewModel.updateMood(m); expanded = false })
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = state.tastingNotes ?: "",
                    onValueChange = { viewModel.updateTastingNotes(it) },
                    label = { Text("感受（干香、湿香、风味、触感...）") },
                    modifier = Modifier.fillMaxWidth(), maxLines = 4
                )
            }

            item {
                OutlinedTextField(
                    value = state.improvementNotes ?: "",
                    onValueChange = { viewModel.updateImprovementNotes(it) },
                    label = { Text("改进备注") },
                    modifier = Modifier.fillMaxWidth(), maxLines = 3
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerCard(
    brewTimeMs: Long,
    onBrewTimeChanged: (Long) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    val zone = ZoneId.systemDefault()
    val displayInstant = remember(brewTimeMs) { Instant.ofEpochMilli(brewTimeMs) }
    val displayLocalDateTime = remember(displayInstant) { displayInstant.atZone(zone).toLocalDateTime() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy年MM月dd日") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    // DatePickerDialog（只改日期，时分保持系统当前） */
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = brewTimeMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val pickedDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
                        val nowTime = java.time.LocalTime.now()
                        val newDateTime = pickedDate.atTime(nowTime)
                            .atZone(zone).toInstant().toEpochMilli()
                        onBrewTimeChanged(newDateTime)
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

    // ── 单行卡片，点击进入日期修改 ──
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = { showDatePicker = true }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📅", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    displayLocalDateTime.format(dateFormatter),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    displayLocalDateTime.format(timeFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("修改", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}
