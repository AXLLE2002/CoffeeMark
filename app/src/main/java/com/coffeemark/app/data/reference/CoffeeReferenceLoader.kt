package com.coffeemark.app.data.reference

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 从 assets/coffee_reference.json 解析内置参考数据库。
 *
 * 使用 Android 内置的 org.json，不引入任何第三方依赖。
 * 解析结果会被缓存（双重检查锁），多次调用只解析一次、且只在首次访问时读取 assets。
 *
 * 用法（已在 CoffeemarkApp.onCreate 中预加载）：
 *   val ref = CoffeemarkApp.instance.coffeeReference
 *   ref.originNames          // 录入产地时做联想
 *   ref.allFlavorTags        // 风味标签快捷添加
 *   ref.findVarietal("艺伎") // 按别名解析
 */
object CoffeeReferenceLoader {

    private const val ASSET_NAME = "coffee_reference.json"

    @Volatile
    private var cached: CoffeeReference? = null

    fun load(context: Context): CoffeeReference {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val json = context.applicationContext.assets.open(ASSET_NAME)
                .bufferedReader()
                .use { it.readText() }
            return parse(JSONObject(json)).also { cached = it }
        }
    }

    private fun parse(root: JSONObject): CoffeeReference = CoffeeReference(
        version = root.optInt("version", 1),
        generatedNote = root.optString("generatedNote", ""),
        species = root.optJSONArray("species").orEmpty().mapObjects(::parseSpecies),
        varietals = root.optJSONArray("varietals").orEmpty().mapObjects(::parseVarietal),
        origins = root.optJSONArray("origins").orEmpty().mapObjects(::parseOrigin),
        processes = root.optJSONArray("processes").orEmpty().mapObjects(::parseProcess),
        flavorCategories = root.optJSONArray("flavorTags").orEmpty().mapObjects(::parseFlavorCategory)
    )

    private fun parseSpecies(o: JSONObject) = Species(
        name = o.getString("name"),
        nameEn = o.getString("nameEn"),
        latin = o.getString("latin"),
        flavorTags = o.getJSONArray("flavorTags").toStringList(),
        altitudeMeters = o.getJSONArray("altitudeMeters").toIntRange(),
        notes = o.getString("notes")
    )

    private fun parseVarietal(o: JSONObject) = Varietal(
        name = o.getString("name"),
        nameEn = o.getString("nameEn"),
        aliases = o.optJSONArray("aliases").orEmpty().toStringList(),
        species = o.getString("species"),
        typicalOrigins = o.optJSONArray("typicalOrigins").orEmpty().toStringList(),
        typicalProcesses = o.optJSONArray("typicalProcesses").orEmpty().toStringList(),
        flavorTags = o.getJSONArray("flavorTags").toStringList(),
        altitudeMeters = o.getJSONArray("altitudeMeters").toIntRange(),
        notes = o.getString("notes")
    )

    private fun parseOrigin(o: JSONObject) = Origin(
        name = o.getString("name"),
        nameEn = o.getString("nameEn"),
        typicalVarietals = o.optJSONArray("typicalVarietals").orEmpty().toStringList(),
        typicalProcesses = o.optJSONArray("typicalProcesses").orEmpty().toStringList(),
        flavorTags = o.getJSONArray("flavorTags").toStringList(),
        acidity = o.getString("acidity"),
        body = o.getString("body"),
        notes = o.getString("notes")
    )

    private fun parseProcess(o: JSONObject) = Process(
        name = o.getString("name"),
        nameEn = o.getString("nameEn"),
        aliases = o.optJSONArray("aliases").orEmpty().toStringList(),
        flavorTags = o.getJSONArray("flavorTags").toStringList(),
        notes = o.getString("notes")
    )

    private fun parseFlavorCategory(o: JSONObject) = FlavorCategory(
        category = o.getString("category"),
        categoryEn = o.getString("categoryEn"),
        tags = o.getJSONArray("tags").toStringList()
    )

    private fun JSONArray?.orEmpty(): JSONArray = this ?: JSONArray()

    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
        val out = ArrayList<T>(length())
        for (i in 0 until length()) out.add(transform(getJSONObject(i)))
        return out
    }

    private fun JSONArray.toStringList(): List<String> {
        val out = ArrayList<String>(length())
        for (i in 0 until length()) out.add(getString(i))
        return out
    }

    private fun JSONArray.toIntRange(): IntRange = IntRange(getInt(0), getInt(1))
}
