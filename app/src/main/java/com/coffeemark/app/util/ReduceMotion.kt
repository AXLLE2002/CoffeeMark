package com.coffeemark.app.util

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * 读取系统「移除动画 / 减少动态效果」设置（ANIMATOR_DURATION_SCALE == 0），
 * 用于无障碍降级：开启时关闭非必要动画。零依赖实现，兼容当前 Compose 1.6.x。
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context: Context = LocalContext.current
    return remember(context) {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        scale == 0f
    }
}
