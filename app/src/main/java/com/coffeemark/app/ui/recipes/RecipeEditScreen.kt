package com.coffeemark.app.ui.recipes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeemark.app.data.enums.Difficulty
import com.coffeemark.app.data.enums.GrindSize
import com.coffeemark.app.data.enums.StepActionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditScreen(
    recipeId: String? = null,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: RecipeEditViewModel = viewModel(
        factory = RecipeEditViewModel.Factory(recipeId)
    )
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "编辑方案" else "新建方案") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("取消") }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save() },
                        enabled = !state.isSaving
                    ) { Text("保存", color = MaterialTheme.colorScheme.primary) }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // 错误提示
            if (state.error != null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(state.error!!, modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            // ── 基本信息 ──
            item { SectionHeader("基本信息") }

            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::updateName,
                    label = { Text("方案名称 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                // 器具：自定义输入
                OutlinedTextField(
                    value = state.device,
                    onValueChange = viewModel::updateDevice,
                    label = { Text("器具") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                var waterTempText by remember(state.waterTemp) {
                    mutableStateOf(if (state.waterTemp == 0) "" else state.waterTemp.toString())
                }
                var beanWeightText by remember(state.beanWeight) {
                    mutableStateOf(if (state.beanWeight == 0.0) "" else state.beanWeight.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = waterTempText,
                        onValueChange = { input ->
                            waterTempText = input
                            input.toIntOrNull()?.let(viewModel::updateWaterTemp)
                        },
                        label = { Text("水温 (℃)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = beanWeightText,
                        onValueChange = { input ->
                            beanWeightText = input
                            input.toDoubleOrNull()?.let(viewModel::updateBeanWeight)
                        },
                        label = { Text("粉重 (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            item {
                // 研磨度下拉
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = state.grindSize.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("研磨度") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        GrindSize.entries.forEach { gs ->
                            DropdownMenuItem(
                                text = { Text(gs.label) },
                                onClick = { viewModel.updateGrindSize(gs); expanded = false }
                            )
                        }
                    }
                }
            }

            item {
                // 总水量 + 微调按钮
                var totalWaterText by remember(state.totalWater) {
                    mutableStateOf(if (state.totalWater == 0.0) "" else state.totalWater.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalWaterText,
                        onValueChange = { input ->
                            totalWaterText = input
                            input.toDoubleOrNull()?.let(viewModel::updateTotalWater)
                        },
                        label = { Text("总注水量 (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Column {
                        listOf(+1.5, +1.0, +0.5).forEach { d ->
                            TextButton(onClick = { viewModel.adjustTotalWater(d) }, modifier = Modifier.height(28.dp)) {
                                Text("+$d", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Column {
                        listOf(-0.5, -1.0, -1.5).forEach { d ->
                            TextButton(onClick = { viewModel.adjustTotalWater(d) }, modifier = Modifier.height(28.dp)) {
                                Text("${d}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = state.difficulty?.label ?: "不设置",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("难度") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("不设置") },
                            onClick = { viewModel.updateDifficulty(null); expanded = false }
                        )
                        Difficulty.entries.forEach { d ->
                            DropdownMenuItem(
                                text = { Text(d.label) },
                                onClick = { viewModel.updateDifficulty(d); expanded = false }
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = state.source ?: "",
                    onValueChange = { viewModel.updateSource(it.ifBlank { null }) },
                    label = { Text("出处") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── 步骤编辑 ──
            item { SectionHeader("冲煮步骤") }

            // 四个积木按钮
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StepActionType.entries.forEach { actionType ->
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.addStep(actionType) },
                            label = { Text(actionType.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (state.steps.isEmpty()) {
                item {
                    Text(
                        "至少添加一个步骤",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                // 步骤列表
                itemsIndexed(state.steps, key = { index, _ -> index }) { index, step ->
                    StepEditCard(
                        index = index,
                        step = step,
                        onUpdate = { viewModel.updateStep(index, it) },
                        onDelete = { viewModel.removeStep(index) },
                        onMoveUp = { if (index > 0) viewModel.moveStep(index, index - 1) },
                        onMoveDown = { if (index < state.steps.size - 1) viewModel.moveStep(index, index + 1) }
                    )
                }
            }

            // 底部留白（FAB不遮挡）
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepEditCard(
    index: Int,
    step: RecipeStepState,
    onUpdate: (RecipeStepState) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 顶部：序号 + 动作类型 + 移动/删除按钮
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "第${index + 1}步",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                    Text("▲", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                    Text("▼", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Text("✕", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 动作类型下拉
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = step.actionType.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("动作") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    StepActionType.entries.forEach { at ->
                        DropdownMenuItem(
                            text = { Text(at.label) },
                            onClick = { onUpdate(step.copy(actionType = at)); expanded = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            var waterText by remember(step.waterAmount) {
                mutableStateOf(if (step.waterAmount == 0.0) "" else step.waterAmount.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() })
            }
            var durationText by remember(step.duration) {
                mutableStateOf(if (step.duration == 0) "" else step.duration.toString())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = waterText,
                    onValueChange = { input ->
                        waterText = input
                        input.toDoubleOrNull()?.let { v -> onUpdate(step.copy(waterAmount = v)) }
                    },
                    label = { Text("水量 (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = step.actionType != StepActionType.STIR && step.actionType != StepActionType.WAIT
                )
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { input ->
                        durationText = input
                        input.toIntOrNull()?.let { v -> onUpdate(step.copy(duration = v)) }
                    },
                    label = { Text("时长 (秒)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
    }
}
