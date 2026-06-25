package com.coffeemark.app.data.enums

enum class BeanStatus(val label: String) {
    UNOPENED("未开封"),
    OPENED("已开封"),
    USED_UP("已用完");

    companion object {
        fun fromLabel(label: String): BeanStatus =
            entries.find { it.label == label } ?: UNOPENED
    }
}
