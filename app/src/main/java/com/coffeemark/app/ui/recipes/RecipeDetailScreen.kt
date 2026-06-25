package com.coffeemark.app.ui.recipes

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
import com.coffeemark.app.data.entity.RecipeStepEntity
import com.coffeemark.app.util.TimeFormatUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    onEdit: () -> Unit,
    onStartBrew: () -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
    viewModel: RecipeDetailViewModel = viewModel(
        factory = RecipeDetailViewModel.Factory(recipeId)
    )
) {
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.recipe?.name ?: "方案详情") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("← 返回") }
                },
                actions = {
                    if (state.recipe != null) {
                        TextButton(onClick = onEdit) { Text("编辑") }
                        TextButton(onClick = { showDeleteDialog = true }) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (state.recipe != null) {
                Surface(shadowElevation = 8.dp) {
                    Button(
                        onClick = onStartBrew,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(48.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("▶ 开始冲煮引导", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (state.recipe != null) {
            val recipe = state.recipe!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 方案名称
                Text(recipe.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Medium)

                // 参数卡片
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailRow("器具", recipe.device)
                        DetailRow("水温", "${recipe.waterTemp}℃")
                        DetailRow("粉重", "${recipe.beanWeight}g")
                        DetailRow("研磨度", recipe.grindSize.label)
                        DetailRow("总注水量", "${recipe.totalWater}g")
                        DetailRow("粉水比", "1:${String.format("%.1f", recipe.totalWater / recipe.beanWeight)}")
                        recipe.difficulty?.let { DetailRow("难度", it.label) }
                        recipe.source?.let { DetailRow("出处", it) }
                    }
                }

                // 步骤列表
                Text("冲煮步骤", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)

                val totalDuration = state.steps.sumOf { it.duration }

                state.steps.forEachIndexed { index, step ->
                    StepDetailCard(index + 1, step)
                }

                // 总耗时
                Text(
                    "总耗时：${TimeFormatUtil.formatDuration(totalDuration)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 删除确认
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("删除方案") },
                text = { Text("确定删除此方案吗？") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteRecipe(onDeleted)
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

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun StepDetailCard(index: Int, step: RecipeStepEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 步骤序号圆圈
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$index", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(step.actionType.label, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium)
                Text("水量 ${step.waterAmount}g · 时长 ${step.duration}秒",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
