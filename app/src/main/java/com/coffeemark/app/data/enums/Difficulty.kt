package com.coffeemark.app.data.enums

enum class Difficulty(val label: String) {
    EASY("简单"),
    MEDIUM("中等"),
    HARD("困难");

    companion object {
        fun fromLabel(label: String): Difficulty =
            entries.find { it.label == label } ?: MEDIUM
    }
}
