package com.coffeemark.app.ui.brewguide

import com.coffeemark.app.CoffeemarkApp
import com.coffeemark.app.data.enums.StepActionType
import com.coffeemark.app.ui.theme.Caramel
import com.coffeemark.app.ui.theme.CaramelLight
import com.coffeemark.app.ui.theme.CoffeeBrown
import com.coffeemark.app.ui.theme.CoffeeBrownVariant
import com.coffeemark.app.ui.theme.CoffeeLight
import com.coffeemark.app.ui.theme.OnCream
import com.coffeemark.app.ui.theme.OnCreamMuted
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import com.coffeemark.app.util.rememberReduceMotion
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel

/** 从任意 Context 向上回溯找到宿主 ComponentActivity（兼容 ContextThemeWrapper 包装）。 */
private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}

@Composable
fun BrewGuideScreen(
    recipeId: String,
    dose: Double? = null,
    beanId: String? = null,
    onFinished: () -> Unit,
    viewModel: BrewGuideViewModel = viewModel(factory = BrewGuideViewModel.Factory(recipeId, dose, beanId))
) {
    val state by viewModel.state.collectAsState()

    // 沉浸式全屏：冲煮引导期间隐藏系统栏，离开时自动恢复
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = view.context.findComponentActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

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

    // ── 注水涟漪（脉冲：每次进入「注水 / 闷蒸」步时从圆心扩散一次；文本在上、涟漪在下）──
    val reduceMotion = rememberReduceMotion()
    val rippleProgress = remember { Animatable(0f) }
    val isPourStep = step != null && step.actionType in setOf(StepActionType.POUR, StepActionType.BLOOM)
    LaunchedEffect(state.currentStepIndex, state.countdownNumber) {
        if (!reduceMotion && state.countdownNumber == 0 && isPourStep) {
            rippleProgress.snapTo(0f)
            rippleProgress.animateTo(1f, tween(durationMillis = 900))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFFEFBF6), Color(0xFFE1D2C4))
                )
            )
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // ── 总计时 ──
            Text(
                text = "%02d:%02d.%d".format(totalMin, totalSec, totalTenth),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = OnCream
            )
            Spacer(Modifier.height(24.dp))

            // ── 圆环进度 ──
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                val trackColor = CoffeeLight.copy(alpha = 0.45f)
                Canvas(modifier = Modifier.size(220.dp)) {
                    val strokeWidth = 12.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    // 底色轨
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // 进度弧（焦糖渐变；首尾同色 → 接缝色差消除）
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(CaramelLight, Caramel, CoffeeBrownVariant, CaramelLight)
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * s.stepProgress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // 注水涟漪（位于中心文字之下）
                Canvas(modifier = Modifier.size(220.dp)) {
                    val p = rippleProgress.value
                    if (p > 0f) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val inner = 26.dp.toPx()
                        val outer = size.width / 2 - 6.dp.toPx()
                        val radius = inner + (outer - inner) * p
                        // 柔和填充（水感）
                        drawCircle(
                            color = Caramel,
                            radius = radius,
                            center = center,
                            style = Fill,
                            alpha = (1f - p) * 0.12f
                        )
                        // 扩散圆环
                        drawCircle(
                            color = Caramel,
                            radius = radius,
                            center = center,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                            alpha = (1f - p) * 0.5f
                        )
                    }
                }

                // 中心文字
                if (s.countdownNumber > 0) {
                    // 开场倒计时 3-2-1
                    Text(
                        "${s.countdownNumber}",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                        fontWeight = FontWeight.Bold,
                        color = OnCream
                    )
                } else if (step != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            step.actionType.label,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            color = OnCream
                        )
                        Text(
                            "${(step.duration * 1000L - s.stepElapsedMs).coerceAtLeast(0) / 1000}",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 36.sp),
                            fontWeight = FontWeight.Bold,
                            color = OnCream
                        )
                        if (step.actionType != StepActionType.WAIT) {
                            Text(
                                "至 ${s.cumulativeTargetWater.toLong()}g",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = CoffeeBrown
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── 水量信息（累计注水为核心；WAIT 步骤不显示）──
            if (step != null && step.actionType != StepActionType.WAIT) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 本段注水
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "本段",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnCreamMuted
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${step.waterAmount.toLong()}g",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnCream
                        )
                    }

                    // 累计注水（高亮 — 核心需求）
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "累计",
                            style = MaterialTheme.typography.labelSmall,
                            color = CoffeeBrown.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${s.cumulativeTargetWater.toLong()}g",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CoffeeBrown
                        )
                    }

                    // 平均流速
                    val flow = if (step.duration > 0) step.waterAmount / step.duration else 0.0
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "流速",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnCreamMuted
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${String.format("%.1f", flow)} G/S",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnCream
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── 步骤进度条 ──
            Text(
                "第${s.currentStepIndex + 1}/${s.steps.size}步",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = OnCreamMuted
            )

            // 步骤指示点
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                s.steps.indices.forEach { i ->
                    Surface(
                        modifier = Modifier.size(if (i == s.currentStepIndex) 12.dp else 8.dp),
                        shape = CircleShape,
                        color = if (i <= s.currentStepIndex) Caramel
                        else Color(0xFFD6CCC3)
                    ) {}
                }
            }

            Spacer(Modifier.height(16.dp))

            // 音效状态
            Text(
                s.soundStatus,
                style = MaterialTheme.typography.bodySmall,
                color = OnCreamMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
