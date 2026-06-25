package com.coffeemark.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.coffeemark.app.data.enums.Difficulty
import com.coffeemark.app.data.enums.GrindSize
import java.util.UUID

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "device")
    val device: String,

    @ColumnInfo(name = "water_temp")
    val waterTemp: Int,         // ℃

    @ColumnInfo(name = "bean_weight")
    val beanWeight: Double,     // g (粉重)

    @ColumnInfo(name = "grind_size")
    val grindSize: GrindSize,

    @ColumnInfo(name = "total_water")
    val totalWater: Double,     // g

    @ColumnInfo(name = "difficulty")
    val difficulty: Difficulty? = null,

    @ColumnInfo(name = "source")
    val source: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
