package com.coffeemark.app.data.enums

enum class GrindSize(val label: String) {
    COARSE("粗"),
    MEDIUM_COARSE("中粗"),
    MEDIUM("中"),
    MEDIUM_FINE("中细"),
    FINE("细");

    companion object {
        fun fromLabel(label: String): GrindSize =
            entries.find { it.label == label } ?: MEDIUM
    }
}
