package com.coffeemark.app.ui.brewguide

import com.coffeemark.app.CoffeemarkApp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun BrewGuideScreen(
    recipeId: String,
    onFinished: () -> Unit,
    viewModel: BrewGuideViewModel = viewModel(factory = BrewGuideViewModel.Factory(recipeId))
) {
    val state by viewModel.state.collectAsState()

    // 初始化内置提示音
    LaunchedEffect(Unit) {
        viewModel.initSound()
        viewModel.start()
    }

    // 监听完成：保存预填数据再跳转
    LaunchedEffect(state.isFinished) {
        if (state.isFinished) {
            CoffeemarkApp.instance.brewGuidePrefillData = viewModel.getPrefillData()
            onFinished()
        }
    }

    // ── 计时格式化 ──
    val totalMin = (state.totalElapsedMs / 1000) / 60
    val totalSec = (state.totalElapsedMs / 1000) % 60
    val totalTenth = (state.totalElapsedMs % 1000) / 100

    val s = state
    val step = s.currentStep

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // ── 总计时 ──
            Text(
                text = "%02d:%02d.%d".format(totalMin, totalSec, totalTenth),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 40.sp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))

            // ── 圆环进度 ──
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                Canvas(modifier = Modifier.size(220.dp)) {
                    val strokeWidth = 12.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    // 底色轨
                    drawArc(
                        color = Color(0xFFEFEBE9),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // 进度弧
                    drawArc(
                        color = Color(0xFFFF8A65),
                        startAngle = -90f,
                        sweepAngle = 360f * s.stepProgress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // 中心文字
                if (s.countdownNumber > 0) {
                    // 开场倒计时 3-2-1
                    Text(
                        "${s.countdownNumber}",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (step != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(step.actionType.label, style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium)
                        Text(
                            "${(step.duration * 1000L - s.stepElapsedMs).coerceAtLeast(0) / 1000}",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 36.sp),
                            fontWeight = FontWeight.Bold
                        )
                        if (step.actionType != com.coffeemark.app.data.enums.StepActionType.WAIT) {
                            Text("${step.waterAmount.toLong()}g", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── 水量信息 ──
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("总目标水量", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${s.totalTargetWater.toLong()}g", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                if (step != null && step.actionType != com.coffeemark.app.data.enums.StepActionType.WAIT) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("阶段水量", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${step.waterAmount.toLong()}g", style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("平均流速", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val flow = if (step.duration > 0) step.waterAmount / step.duration else 0.0
                        Text("${String.format("%.1f", flow)} G/S", style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── 步骤进度条 ──
            Text("第${s.currentStepIndex + 1}/${s.steps.size}步",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            // 步骤指示点
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                s.steps.indices.forEach { i ->
                    Surface(
                        modifier = Modifier.size(if (i == s.currentStepIndex) 12.dp else 8.dp),
                        shape = MaterialTheme.shapes.small,
                        color = if (i <= s.currentStepIndex) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    ) {}
                }
            }

            Spacer(Modifier.height(16.dp))

            // 音效状态
            Text(s.soundStatus, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
