package com.coffeemark.app.data.enums

enum class RoastLevel(val label: String) {
    EXTRA_LIGHT("极浅烘"),
    LIGHT("浅烘"),
    MEDIUM_LIGHT("中浅烘"),
    MEDIUM("中烘"),
    MEDIUM_DARK("中深烘"),
    DARK("深烘");

    companion object {
        fun fromLabel(label: String): RoastLevel =
            entries.find { it.label == label } ?: MEDIUM
    }
}
