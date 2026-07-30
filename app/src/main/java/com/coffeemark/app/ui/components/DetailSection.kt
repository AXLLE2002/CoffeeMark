package com.coffeemark.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeemark.app.ui.theme.CoffeeCard

/**
 * 带标题的详情卡片段落。
 * 替换 3 个 DetailScreen 中重复的 Card + Column 模式。
 * 卡片本体使用 [CoffeeCard]（实心 surface + 暖色描边 + 柔影），与全站卡片风格一致。
 *
 * 使用示例：
 *   DetailSection(title = "产地信息") {
 *       DetailRow("产地", bean.origin ?: "-")
 *       DetailRow("处理法", bean.process ?: "-")
 *   }
 */
@Composable
fun DetailSection(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    CoffeeCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}
