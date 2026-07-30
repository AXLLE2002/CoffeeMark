package com.coffeemark.app.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Themed content card. Currently rendered as a SOLID, opaque surface with a
 * hairline warm border + soft shadow — sits on top of [AmbientBackground] so
 * the warm gradient shows through in the gaps between cards.
 *
 * (Name is deliberately neutral: the body can be swapped between a solid card
 * and a translucent frosted-glass card without touching call sites.)
 */
@Composable
fun CoffeeCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(elevation = 3.dp, shape = shape, clip = false)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surface
    ) {
        content()
    }
}

/**
 * Soft ambient background: a warm vertical gradient + several large, heavily
 * blurred color blobs (drawn with [BlurMaskFilter] on a Canvas, so it works
 * on every Compose version). Place this as the bottom layer of a screen so
 * the warm color shows through behind translucent UI and in the gaps between
 * solid [CoffeeCard]s.
 */
@Composable
fun AmbientBackground(modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val gradient = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF211C1A), Color(0xFF141110)))
    } else {
        // 更饱和的暖色渐变（暖奶白 → 浅焦糖 → 暖棕调），避免整体发白
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFCEDE2),
                Color(0xFFF4DFCB),
                Color(0xFFEFD6BE)
            )
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(gradient))

        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            fun blob(c: Color, a: Float, cx: Float, cy: Float, rDp: Float, blur: Float) {
                val paint = Paint().apply { color = c.copy(alpha = a) }
                paint.asFrameworkPaint().maskFilter =
                    BlurMaskFilter(blur.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                drawContext.canvas.drawCircle(
                    center = Offset(cx * w, cy * h),
                    radius = rDp.dp.toPx(),
                    paint = paint
                )
            }

            if (isDark) {
                blob(Color(0xFFFF8A65), 0.42f, 0.92f, 0.04f, 190f, 80f)
                blob(Color(0xFF6D4C41), 0.36f, 0.06f, 0.92f, 220f, 90f)
            } else {
                // 色块铺满更多区域（含屏幕中部），提高透明度，让玻璃卡背后透出颜色
                blob(Color(0xFFFF9E80), 0.50f, 0.90f, 0.05f, 210f, 95f) // 右上 暖橙
                blob(Color(0xFFD9A982), 0.46f, 0.08f, 0.96f, 250f, 100f) // 左下 焦糖
                blob(Color(0xFFF6C3A1), 0.40f, 0.50f, 0.45f, 240f, 100f) // 中部 蜜桃
                blob(Color(0xFFC98E63), 0.34f, 0.30f, 0.78f, 200f, 95f)  // 中下 浅咖
            }
        }
    }
}
