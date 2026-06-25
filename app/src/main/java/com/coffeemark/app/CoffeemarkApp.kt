package com.coffeemark.app

import android.app.Application
import com.coffeemark.app.data.AppDatabase
import com.coffeemark.app.data.repository.BeanRepository

class CoffeemarkApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var beanRepository: BeanRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        beanRepository = BeanRepository(
            beanDao = database.beanDao(),
            brewLogDao = database.brewLogDao()
        )
    }

    // 冲煮引导完成后传递到新建记录的预填数据
    var brewGuidePrefillData: com.coffeemark.app.ui.brewguide.BrewGuidePrefillData? = null

    companion object {
        lateinit var instance: CoffeemarkApp
            private set
    }
}
