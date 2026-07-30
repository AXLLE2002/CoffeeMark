package com.coffeemark.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 参数标签 — 用于详情页顶部展示核心参数行。
 * 背景使用 primaryContainer，文字使用 onPrimaryContainer。
 *
 * 使用示例：
 *   ParamBadge(recipe.device)
 *   ParamBadge("${recipe.waterTemp}℃")
 */
@Composable
fun ParamBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
