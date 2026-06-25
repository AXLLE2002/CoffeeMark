package com.coffeemark.app.ui.brewguide

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeemark.app.CoffeemarkApp
import com.coffeemark.app.util.TimeFormatUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrewCompleteScreen(
    recipeId: String,
    onGoToRecord: (BrewGuidePrefillData) -> Unit,
    onBackToRecipes: () -> Unit,
    viewModel: BrewGuideViewModel = viewModel(factory = BrewGuideViewModel.Factory(recipeId))
) {
    val state by viewModel.state.collectAsState()
    val prefill = CoffeemarkApp.instance.brewGuidePrefillData

    // 系统返回键 → 回到冲煮记录列表
    BackHandler(onBack = onBackToRecipes)

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🎉", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(24.dp))

            Text("冲煮完成", style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(16.dp))

            state.recipe?.let { recipe ->
                Text(recipe.name, style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(8.dp))

            Text("总耗时：${TimeFormatUtil.formatDurationMs(prefill?.totalDuration?.let { it * 1000L } ?: 0L)}",
                style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = { prefill?.let { onGoToRecord(it) } },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("📝 去记录", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onBackToRecipes) {
                Text("返回冲煮记录")
            }
        }
    }
}
