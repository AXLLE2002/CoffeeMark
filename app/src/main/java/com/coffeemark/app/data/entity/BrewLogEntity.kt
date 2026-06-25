package com.coffeemark.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.coffeemark.app.data.enums.GrindSize
import com.coffeemark.app.data.enums.Mood
import java.util.UUID

@Entity(
    tableName = "brew_logs",
    indices = [
        Index(value = ["bean_id"]),
        Index(value = ["recipe_id"])
    ]
)
data class BrewLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "bean_id")
    val beanId: String,

    @ColumnInfo(name = "bean_used_weight")
    val beanUsedWeight: Double,     // g (用豆量)

    @ColumnInfo(name = "recipe_id")
    val recipeId: String? = null,   // 关联方案（可选）

    @ColumnInfo(name = "custom_recipe_name")
    val customRecipeName: String? = null, // 手动输入方案名（不与recipe_id共存）

    @ColumnInfo(name = "ground_weight")
    val groundWeight: Double,       // g (粉重)

    @ColumnInfo(name = "total_water")
    val totalWater: Double,         // g (注水量)

    @ColumnInfo(name = "water_temp")
    val waterTemp: Int? = null,     // ℃

    @ColumnInfo(name = "grinder")
    val grinder: String? = null,    // 磨豆机

    @ColumnInfo(name = "grind_size")
    val grindSize: GrindSize? = null,

    @ColumnInfo(name = "device")
    val device: String? = null,     // 器具

    @ColumnInfo(name = "location")
    val location: String? = null,

    @ColumnInfo(name = "weather")
    val weather: String? = null,

    @ColumnInfo(name = "brew_time")
    val brewTime: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "rating")
    val rating: Int,                // 1-5

    @ColumnInfo(name = "mood")
    val mood: Mood? = null,

    @ColumnInfo(name = "tasting_notes")
    val tastingNotes: String? = null,

    @ColumnInfo(name = "improvement_notes")
    val improvementNotes: String? = null,

    @ColumnInfo(name = "total_duration")
    val totalDuration: Int,         // 秒

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    /** 根据星级自动生成标签 */
    val ratingTag: String
        get() = when (rating) {
            5 -> "超级好喝"
            4 -> "非常好喝"
            3 -> "好喝"
            2 -> "一般"
            1 -> "不太满意"
            else -> ""
        }
}
