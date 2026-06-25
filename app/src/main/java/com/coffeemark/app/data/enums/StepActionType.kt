package com.coffeemark.app.data.enums

enum class StepActionType(val label: String) {
    BLOOM("闷蒸"),
    POUR("注水"),
    STIR("搅拌"),
    WAIT("等待");

    companion object {
        fun fromLabel(label: String): StepActionType =
            entries.find { it.label == label } ?: POUR
    }
}
