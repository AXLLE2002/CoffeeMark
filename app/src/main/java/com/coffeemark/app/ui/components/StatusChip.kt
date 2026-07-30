package com.coffeemark.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeemark.app.data.enums.BeanStatus

/**
 * 豆子状态标签（未开封/已开封/已用完）。
 *
 * 说明：此处提取的是 BeanListScreen 中既有的内联实现（使用 MaterialTheme 容器色，
 * 自动适配亮/暗主题），而非改造任务书首页示例里基于固定色值 + 白字 (OnCoffee) 的版本，
 * 以避免在暗色模式下出现白字/低对比问题，保持与现有 UI 一致。
 */
@Composable
fun StatusChip(status: BeanStatus) {
    val (bg, text) = when (status) {
        BeanStatus.UNOPENED -> MaterialTheme.colorScheme.tertiaryContainer to "未开封"
        BeanStatus.OPENED -> MaterialTheme.colorScheme.secondaryContainer to "已开封"
        BeanStatus.USED_UP -> MaterialTheme.colorScheme.surfaceVariant to "已用完"
    }
    Surface(shape = MaterialTheme.shapes.small, color = bg) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
