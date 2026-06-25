package com.coffeemark.app.data.enums

enum class BeanType(val label: String) {
    SINGLE_ORIGIN("单品"),
    BLEND("拼配");

    companion object {
        fun fromLabel(label: String): BeanType =
            entries.find { it.label == label } ?: SINGLE_ORIGIN
    }
}
