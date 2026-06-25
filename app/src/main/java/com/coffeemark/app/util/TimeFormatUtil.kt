package com.coffeemark.app.util

object TimeFormatUtil {

    /** 秒 → "X分X秒" */
    fun formatDuration(totalSeconds: Int): String {
        if (totalSeconds <= 0) return "0秒"
        val min = totalSeconds / 60
        val sec = totalSeconds % 60
        return buildString {
            if (min > 0) append("${min}分")
            if (sec > 0) append("${sec}秒")
        }
    }

    /** 秒 → "MM:SS.s" (用于冲煮引导计时) */
    fun formatTimer(totalSeconds: Int, tenths: Int): String {
        val min = totalSeconds / 60
        val sec = totalSeconds % 60
        return "%02d:%02d.%d".format(min, sec, tenths)
    }

    /** 毫秒 → "X分X秒" */
    fun formatDurationMs(ms: Long): String = formatDuration((ms / 1000).toInt())
}
