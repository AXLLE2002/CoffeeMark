package com.coffeemark.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.coffeemark.app.data.enums.BeanStatus
import com.coffeemark.app.data.enums.BeanType
import com.coffeemark.app.data.enums.RoastLevel
import java.util.UUID

@Entity(tableName = "beans")
data class BeanEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "net_weight")
    val netWeight: Double,      // g (整包重量)

    @ColumnInfo(name = "current_weight")
    val currentWeight: Double,  // g (当前剩余)

    @ColumnInfo(name = "price")
    val price: Double,          // 元 (整包价格)

    @ColumnInfo(name = "total_used_price")
    val totalUsedPrice: Double = 0.0,  // 元 (累计已使用)

    @ColumnInfo(name = "roaster")
    val roaster: String? = null,

    @ColumnInfo(name = "estate_station")
    val estateStation: String? = null,

    @ColumnInfo(name = "producer")
    val producer: String? = null,

    @ColumnInfo(name = "batch")
    val batch: String? = null,

    @ColumnInfo(name = "altitude")
    val altitude: String? = null,

    @ColumnInfo(name = "roast_date")
    val roastDate: Long,        // 烘焙日期 (timestamp)

    @ColumnInfo(name = "shelf_life_days")
    val shelfLifeDays: Int,     // 赏味期（天）

    @ColumnInfo(name = "bean_type")
    val beanType: BeanType,

    @ColumnInfo(name = "origin")
    val origin: String? = null,

    @ColumnInfo(name = "process")
    val process: String? = null,

    @ColumnInfo(name = "varietal")
    val varietal: String? = null,

    @ColumnInfo(name = "roast_level")
    val roastLevel: RoastLevel? = null,

    @ColumnInfo(name = "is_espresso")
    val isEspresso: Boolean = false,

    @ColumnInfo(name = "flavor_tags")
    val flavorTags: List<String>? = null,

    @ColumnInfo(name = "status")
    val status: BeanStatus = BeanStatus.UNOPENED,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** 克单价 = 整包价格 ÷ 净含量（自动计算，不存DB） */
    val pricePerGram: Double
        get() = if (netWeight > 0) price / netWeight else 0.0
}
