package com.coffeemark.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp

/**
 * 带联想提示的文本输入框。
 *
 * @param value 当前输入值
 * @param onValueChange 输入变化回调
 * @param suggestions 联想建议列表
 * @param label 输入框标签
 * @param modifier Modifier
 * @param maxSuggestions 最多显示的建议数量（默认5条）
 * @param onSuggestionSelected 点击建议的回调（可选）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    label: String,
    modifier: Modifier = Modifier,
    maxSuggestions: Int = 5,
    onSuggestionSelected: ((String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var hasFocus by remember { mutableStateOf(false) }

    // 根据输入过滤建议（不区分大小写，支持中英文混合）
    val filteredSuggestions = remember(value, suggestions) {
        if (value.isBlank()) {
            emptyList()
        } else {
            suggestions.filter { suggestion ->
                suggestion.contains(value, ignoreCase = true)
            }.take(maxSuggestions)
        }
    }

    // 失焦时延迟收起（让用户有时间点击建议）
    LaunchedEffect(hasFocus) {
        if (!hasFocus && expanded) {
            kotlinx.coroutines.delay(150)
            expanded = false
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded && filteredSuggestions.isNotEmpty(),
        onExpandedChange = { /* 手动控制展开 */ },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { input ->
                onValueChange(input)
                expanded = input.isNotBlank()
            },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .onFocusChanged { focusState ->
                    hasFocus = focusState.hasFocus
                    if (focusState.hasFocus && value.isNotBlank()) {
                        expanded = true
                    }
                }
        )

        ExposedDropdownMenu(
            expanded = expanded && filteredSuggestions.isNotEmpty(),
            onDismissRequest = { expanded = false }
        ) {
            filteredSuggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    onClick = {
                        onValueChange(suggestion)
                        expanded = false
                        onSuggestionSelected?.invoke(suggestion)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}