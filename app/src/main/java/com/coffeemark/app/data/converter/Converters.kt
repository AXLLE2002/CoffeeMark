package com.coffeemark.app.data.converter

import androidx.room.TypeConverter
import com.coffeemark.app.data.enums.*
import org.json.JSONArray

class Converters {

    // ── List<String> ↔ JSON ──
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { JSONArray(it).toString() }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let {
            val arr = JSONArray(it)
            (0 until arr.length()).map { i -> arr.getString(i) }
        }
    }

    // ── Enums ──
    @TypeConverter fun fromGrindSize(value: GrindSize?): String? = value?.name
    @TypeConverter fun toGrindSize(value: String?): GrindSize? =
        value?.let { GrindSize.valueOf(it) }

    @TypeConverter fun fromStepActionType(value: StepActionType?): String? = value?.name
    @TypeConverter fun toStepActionType(value: String?): StepActionType? =
        value?.let { StepActionType.valueOf(it) }

    @TypeConverter fun fromDifficulty(value: Difficulty?): String? = value?.name
    @TypeConverter fun toDifficulty(value: String?): Difficulty? =
        value?.let { Difficulty.valueOf(it) }

    @TypeConverter fun fromBeanType(value: BeanType?): String? = value?.name
    @TypeConverter fun toBeanType(value: String?): BeanType? =
        value?.let { BeanType.valueOf(it) }

    @TypeConverter fun fromRoastLevel(value: RoastLevel?): String? = value?.name
    @TypeConverter fun toRoastLevel(value: String?): RoastLevel? =
        value?.let { RoastLevel.valueOf(it) }

    @TypeConverter fun fromBeanStatus(value: BeanStatus?): String? = value?.name
    @TypeConverter fun toBeanStatus(value: String?): BeanStatus? =
        value?.let { BeanStatus.valueOf(it) }

    @TypeConverter fun fromMood(value: Mood?): String? = value?.name
    @TypeConverter fun toMood(value: String?): Mood? =
        value?.let { Mood.valueOf(it) }
}
