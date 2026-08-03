package com.coffeemark.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.coffeemark.app.data.converter.Converters
import com.coffeemark.app.data.dao.*
import com.coffeemark.app.data.entity.*

@Database(
    entities = [
        RecipeEntity::class,
        RecipeStepEntity::class,
        BeanEntity::class,
        BrewLogEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recipeDao(): RecipeDao
    abstract fun recipeStepDao(): RecipeStepDao
    abstract fun beanDao(): BeanDao
    abstract fun brewLogDao(): BrewLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE brew_logs ADD COLUMN custom_recipe_name TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipes ADD COLUMN ratio REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE recipes ADD COLUMN is_preset INTEGER NOT NULL DEFAULT 0")
                // 回填旧数据：ratio = 总水量 / 豆量
                db.execSQL(
                    "UPDATE recipes SET ratio = " +
                    "CASE WHEN bean_weight > 0 THEN total_water * 1.0 / bean_weight ELSE 0 END"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 豆仓自定义排序字段（可空；NULL 表示按剩余量默认排序）
                db.execSQL("ALTER TABLE beans ADD COLUMN manual_order INTEGER")
            }
        }

        private val MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "coffeemark.db"
                )
                    .addMigrations(*MIGRATIONS)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
