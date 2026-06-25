package com.coffeemark.app.ui.brewguide

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeemark.app.util.TimeFormatUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrewPrepareScreen(
    recipeId: String,
    onStart: () -> Unit,
    onBack: () -> Unit,
    viewModel: BrewGuideViewModel = viewModel(factory = BrewGuideViewModel.Factory(recipeId))
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("冲煮准备") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← 返回") } }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (state.recipe != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("☕", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.height(24.dp))

                Text(state.recipe!!.name, style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Medium)

                Spacer(Modifier.height(16.dp))

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("共 ${state.steps.size} 步", style = MaterialTheme.typography.bodyLarge)
                        Text("预计耗时 ${TimeFormatUtil.formatDuration(state.steps.sumOf { it.duration })}",
                            style = MaterialTheme.typography.bodyLarge)
                        Text("总注水量 ${state.steps.sumOf { it.waterAmount }.toLong()}g",
                            style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Spacer(Modifier.height(32.dp))

                Text("准备好了吗？", style = MaterialTheme.typography.titleLarge)
                Text("点击开始，全程自动引导，无需操作",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("▶ 开始", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
