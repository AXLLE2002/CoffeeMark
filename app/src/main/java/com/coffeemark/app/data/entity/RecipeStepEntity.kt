package com.coffeemark.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.coffeemark.app.data.enums.StepActionType
import java.util.UUID

@Entity(
    tableName = "recipe_steps",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipe_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["recipe_id"])]
)
data class RecipeStepEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "recipe_id")
    val recipeId: String,

    @ColumnInfo(name = "step_order")
    val order: Int,

    @ColumnInfo(name = "action_type")
    val actionType: StepActionType,

    @ColumnInfo(name = "water_amount")
    val waterAmount: Double,    // g (stir/wait可以填0)

    @ColumnInfo(name = "duration")
    val duration: Int           // 秒
)
