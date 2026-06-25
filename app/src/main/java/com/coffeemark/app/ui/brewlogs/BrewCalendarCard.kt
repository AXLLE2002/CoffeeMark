package com.coffeemark.app.ui.brewlogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

val CoffeeBrown = Color(0xFF6D4C41)
val CoffeeCream = Color(0xFFEFEBE9)
val CoffeeLight = Color(0xFFD7CCC8)

@Composable
fun BrewCalendarCard(
    currentMonth: YearMonth,
    brewDates: Set<LocalDate>,
    isExpanded: Boolean,
    earliestBrewTime: Long?,
    onToggle: () -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }

    // 边界判断
    val earliestMonth = remember(earliestBrewTime) {
        earliestBrewTime?.let {
            YearMonth.from(java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()))
        }
    }
    val canGoPrev = earliestMonth == null || currentMonth.isAfter(earliestMonth)
    val canGoNext = currentMonth.isBefore(YearMonth.now())

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // ── 头部：月份 + 折叠按钮 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📅",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "冲煮日历",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                if (brewDates.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CoffeeBrown.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "${brewDates.size}天",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = CoffeeBrown
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    if (isExpanded) "▲" else "▼",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── 展开内容 ──
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                    // ── 月份导航 ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = onPrevMonth,
                            enabled = canGoPrev,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text(
                                "◀",
                                color = if (canGoPrev) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }

                        Text(
                            currentMonth.format(DateTimeFormatter.ofPattern("yyyy年M月")),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        IconButton(
                            onClick = onNextMonth,
                            enabled = canGoNext,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text(
                                "▶",
                                color = if (canGoNext) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // ── 星期头 ──
                    val dayOfWeekNames = remember {
                        listOf("一", "二", "三", "四", "五", "六", "日")
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        dayOfWeekNames.forEach { name ->
                            Text(
                                name,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (name == "六" || name == "日")
                                    CoffeeBrown else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // ── 日期网格 ──
                    val firstDayOfMonth = currentMonth.atDay(1)
                    val daysInMonth = currentMonth.lengthOfMonth()
                    // 周一起始：Monday=1 ... Sunday=7，转为0-based索引
                    val startDayIndex = (firstDayOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
                    val totalCells = startDayIndex + daysInMonth
                    val rows = (totalCells + 6) / 7 // 向上取整

                    for (row in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            for (col in 0..6) {
                                val cellIndex = row * 7 + col
                                val day = cellIndex - startDayIndex + 1

                                if (day in 1..daysInMonth) {
                                    val date = currentMonth.atDay(day)
                                    val isBrewDay = date in brewDates
                                    val isToday = date == today

                                    DayCell(
                                        day = day,
                                        isBrewDay = isBrewDay,
                                        isToday = isToday,
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    // 空白占位
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isBrewDay: Boolean,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .then(
                if (isBrewDay) {
                    Modifier.border(
                        BorderStroke(2.dp, CoffeeBrown),
                        RoundedCornerShape(8.dp)
                    )
                } else Modifier
            )
            .then(
                if (isToday) {
                    Modifier.background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        RoundedCornerShape(8.dp)
                    )
                } else Modifier
            )
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isToday || isBrewDay) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp
            ),
            color = when {
                isToday -> MaterialTheme.colorScheme.primary
                isBrewDay -> CoffeeBrown
                else -> MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center
        )
    }
}
