package com.coffeemark.app.data.reference

/**
 * 咖啡内置参考数据库的领域模型。
 * 数据来源于 assets/coffee_reference.json（物种 / 豆种 / 产地 / 处理法 / 风味），
 * 由 [CoffeeReferenceLoader] 解析。
 *
 * 用途：为 BeanEntity 的录入提供建议值与可选标签（产地、处理法、豆种、风味标签等）。
 * 本文件仅定义数据层，不涉及任何 UI。
 */

data class Species(
    val name: String,
    val nameEn: String,
    val latin: String,
    val flavorTags: List<String>,
    val altitudeMeters: IntRange,
    val notes: String
)

data class Varietal(
    val name: String,
    val nameEn: String,
    val aliases: List<String>,
    val species: String,
    val typicalOrigins: List<String>,
    val typicalProcesses: List<String>,
    val flavorTags: List<String>,
    val altitudeMeters: IntRange,
    val notes: String
)

data class Origin(
    val name: String,
    val nameEn: String,
    val typicalVarietals: List<String>,
    val typicalProcesses: List<String>,
    val flavorTags: List<String>,
    val acidity: String,
    val body: String,
    val notes: String
)

data class Process(
    val name: String,
    val nameEn: String,
    val aliases: List<String>,
    val flavorTags: List<String>,
    val notes: String
)

data class FlavorCategory(
    val category: String,
    val categoryEn: String,
    val tags: List<String>
)

/**
 * 整份内置参考数据库的根模型。
 * 提供按名称查找与展开后的标签列表，方便录入联想直接复用。
 */
data class CoffeeReference(
    val version: Int,
    val generatedNote: String,
    val species: List<Species>,
    val varietals: List<Varietal>,
    val origins: List<Origin>,
    val processes: List<Process>,
    val flavorCategories: List<FlavorCategory>
) {
    /** 所有物种中文名，用于录入联想。 */
    val speciesNames: List<String> get() = species.map { it.name }

    /** 所有豆种中文名（不含别名）。 */
    val varietalNames: List<String> get() = varietals.map { it.name }

    /** 所有产地中文名。 */
    val originNames: List<String> get() = origins.map { it.name }

    /** 所有处理法中文名（不含别名）。 */
    val processNames: List<String> get() = processes.map { it.name }

    /** 展开所有风味分类的标签，去重并保持出现顺序。 */
    val allFlavorTags: List<String> get() = flavorCategories.flatMap { it.tags }.distinct()

    /** 按豆种中文名或别名查找（如「艺伎」→ 瑰夏）。 */
    fun findVarietal(name: String): Varietal? =
        varietals.firstOrNull { it.name == name || it.aliases.contains(name) }

    /** 按产地中文名查找。 */
    fun findOrigin(name: String): Origin? = origins.firstOrNull { it.name == name }

    /** 按处理法中文名或别名查找（如「半日晒」→ 蜜处理）。 */
    fun findProcess(name: String): Process? =
        processes.firstOrNull { it.name == name || it.aliases.contains(name) }
}
