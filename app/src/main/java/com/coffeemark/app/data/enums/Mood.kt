package com.coffeemark.app.data.enums

enum class Mood(val label: String) {
    HAPPY("开心"),
    CALM("平静"),
    EXCITED("期待"),
    TIRED("疲倦"),
    OTHER("其他");

    companion object {
        fun fromLabel(label: String): Mood =
            entries.find { it.label == label } ?: CALM
    }
}
