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
    version = 2,
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

        private val MIGRATIONS = arrayOf(MIGRATION_1_2)

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
