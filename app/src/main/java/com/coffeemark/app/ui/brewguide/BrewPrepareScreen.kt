package com.coffeemark.app.ui.brewguide

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.PlayArrow
import com.coffeemark.app.util.TimeFormatUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrewPrepareScreen(
    recipeId: String,
    onStart: (Double?) -> Unit,
    onBack: () -> Unit,
    viewModel: BrewGuideViewModel = viewModel(factory = BrewGuideViewModel.Factory(recipeId))
) {
    val state by viewModel.state.collectAsState()

    // 豆量输入框（预填模板基准粉量）；修改后实时换算总注水量
    var doseText by remember { mutableStateOf("") }
    LaunchedEffect(state.recipe) {
        state.recipe?.let { doseText = String.format("%.0f", it.beanWeight) }
    }
    val dose = doseText.toDoubleOrNull()
    val baseRatio = state.recipe?.ratio ?: 0.0
    val ratio = if (baseRatio > 0) baseRatio else state.recipe?.let { it.totalWater / it.beanWeight } ?: 0.0
    val previewTotal = if (dose != null && dose > 0 && ratio > 0) {
        dose * ratio
    } else {
        state.steps.sumOf { it.waterAmount }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        topBar = {
            TopAppBar(
                title = { Text("冲煮准备") },
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (state.recipe != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .imePadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.FreeBreakfast, null, Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                Spacer(Modifier.height(24.dp))

                Text(state.recipe!!.name, style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Medium)

                Spacer(Modifier.height(12.dp))

                // 粉水比徽标（模板核心参数）
                AssistChip(
                    onClick = { },
                    label = { Text("粉水比 1:${ratio.toLong()}") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                Spacer(Modifier.height(16.dp))

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("共 ${state.steps.size} 步", style = MaterialTheme.typography.bodyLarge)
                        Text("预计耗时 ${TimeFormatUtil.formatDuration(state.steps.sumOf { it.duration })}",
                            style = MaterialTheme.typography.bodyLarge)
                        Text("总注水量 ${state.steps.sumOf { it.waterAmount }.toLong()}g（模板基准）",
                            style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 豆量输入框：输入后各阶段水量按粉水比自动缩放
                OutlinedTextField(
                    value = doseText,
                    onValueChange = { doseText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("豆量 (g)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "总注水 ≈ ${previewTotal.toLong()}g · 各阶段按粉水比自动缩放",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                Text("准备好了吗？", style = MaterialTheme.typography.titleLarge)
                Text("点击开始，全程自动引导，无需操作",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = { onStart(dose) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PlayArrow, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("开始", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
