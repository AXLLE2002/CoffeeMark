package com.coffeemark.app

import android.app.Application
import com.coffeemark.app.data.AppDatabase
import com.coffeemark.app.data.reference.BUILTIN_RECIPES
import com.coffeemark.app.data.reference.CoffeeReference
import com.coffeemark.app.data.reference.CoffeeReferenceLoader
import com.coffeemark.app.data.reference.toRecipeEntity
import com.coffeemark.app.data.reference.toStepEntities
import com.coffeemark.app.data.repository.BeanRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

class CoffeemarkApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var beanRepository: BeanRepository
        private set

    lateinit var coffeeReference: CoffeeReference
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        beanRepository = BeanRepository(
            beanDao = database.beanDao(),
            brewLogDao = database.brewLogDao()
        )
        coffeeReference = CoffeeReferenceLoader.load(this)

        ensureBuiltinsSeeded()
    }

    /** 首次启动（或内置模板被清空后）自动播种 10 套内置冲煮模板，幂等 */
    private fun ensureBuiltinsSeeded() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val recipeDao = database.recipeDao()
            if (recipeDao.countPreset() == 0) {
                val stepDao = database.recipeStepDao()
                for (builtin in BUILTIN_RECIPES) {
                    val id = UUID.randomUUID().toString()
                    recipeDao.insert(builtin.toRecipeEntity(id))
                    stepDao.insertAll(builtin.toStepEntities(id))
                }
            }
        }
    }

    // 冲煮引导完成后传递到新建记录的预填数据
    var brewGuidePrefillData: com.coffeemark.app.ui.brewguide.BrewGuidePrefillData? = null

    companion object {
        lateinit var instance: CoffeemarkApp
            private set
    }
}
