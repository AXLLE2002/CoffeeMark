package com.coffeemark.app.ui.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeemark.app.data.entity.RecipeStepEntity
import com.coffeemark.app.util.TimeFormatUtil
import com.coffeemark.app.ui.components.DetailRow
import com.coffeemark.app.ui.components.DetailSection
import com.coffeemark.app.ui.components.ParamBadge

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                    if (state.recipe != null) {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.PlayArrow, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("开始冲煮引导", style = MaterialTheme.typography.labelLarge)
                        }
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
            // 优先使用 ratio 字段，未维护时回退到 总水量/粉量
            val displayRatio = if (recipe.ratio > 0) recipe.ratio else recipe.totalWater / recipe.beanWeight

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. 名称
                Text(
                    recipe.name,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Medium
                )

                // 2. 出处（如果有）
                recipe.source?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(4.dp))

                // 3. 核心参数标签行
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (recipe.isPreset) ParamBadge("内置模板")
                    ParamBadge(recipe.device)
                    ParamBadge("${recipe.waterTemp}℃")
                    ParamBadge("${recipe.beanWeight}g")
                    ParamBadge("1:${String.format("%.1f", displayRatio)}")
                }

                // 4. 参数详情
                DetailSection(title = "参数") {
                    DetailRow("研磨度", recipe.grindSize.label)
                    DetailRow("总注水量", "${recipe.totalWater}g")
                    recipe.difficulty?.let { DetailRow("难度", it.label) }
                }

                // 5. 冲煮步骤（时间线）
                Text(
                    "冲煮步骤",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                state.steps.forEachIndexed { index, step ->
                    TimelineStep(
                        index = index + 1,
                        step = step,
                        isLast = index == state.steps.lastIndex
                    )
                }

                // 6. 总耗时
                val totalDuration = state.steps.sumOf { it.duration }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "总耗时：${TimeFormatUtil.formatDuration(totalDuration)}",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(Modifier.height(32.dp))
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
fun TimelineStep(
    index: Int,
    step: RecipeStepEntity,
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier.height(IntrinsicSize.Min)
    ) {
        // 左侧：序号圆点 + 连接线
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Surface(
                modifier = Modifier.size(24.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "$index",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // 右侧：步骤内容
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                step.actionType.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            val detail = buildList {
                add("水量 ${step.waterAmount}g")
                add("时长 ${step.duration}秒")
            }.joinToString(" · ")
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
