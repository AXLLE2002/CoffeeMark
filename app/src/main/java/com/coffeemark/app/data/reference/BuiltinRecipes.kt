package com.coffeemark.app.data.reference

import com.coffeemark.app.data.entity.RecipeEntity
import com.coffeemark.app.data.entity.RecipeStepEntity
import com.coffeemark.app.data.enums.Difficulty
import com.coffeemark.app.data.enums.GrindSize
import com.coffeemark.app.data.enums.StepActionType
import java.util.UUID

/**
 * 内置冲煮模板的落库结构（与 App 的 RecipeEntity / RecipeStepEntity 对齐）。
 * 步骤按 App 步骤模型拆解：BLOOM(闷蒸,带水量) + POUR(注水,带水量) + WAIT(等待,水量0) + STIR(搅拌,水量0)。
 * 注水段统一按 4g/s 流速拆成「真实注水时长」+「WAIT 等待」，时长恒定、不随豆量缩放。
 */
data class BuiltinStep(
    val actionType: StepActionType,
    val waterAmount: Double,   // g
    val duration: Int          // 秒
)

data class BuiltinRecipe(
    val key: String,           // 稳定标识（用于幂等播种判断）
    val name: String,
    val beanWeight: Double,
    val totalWater: Double,
    val ratio: Double,         // 水:粉，如 15.0 表示 1:15
    val waterTemp: Int,
    val grindSize: GrindSize,
    val difficulty: Difficulty?,
    val source: String,
    val steps: List<BuiltinStep>
)

fun BuiltinRecipe.toRecipeEntity(id: String): RecipeEntity = RecipeEntity(
    id = id,
    name = name,
    device = "V60",
    waterTemp = waterTemp,
    beanWeight = beanWeight,
    grindSize = grindSize,
    totalWater = totalWater,
    ratio = ratio,
    isPreset = true,
    difficulty = difficulty,
    source = source
)

fun BuiltinRecipe.toStepEntities(recipeId: String): List<RecipeStepEntity> =
    steps.mapIndexed { index, s ->
        RecipeStepEntity(
            recipeId = recipeId,
            order = index + 1,
            actionType = s.actionType,
            waterAmount = s.waterAmount,
            duration = s.duration
        )
    }

val BUILTIN_RECIPES: List<BuiltinRecipe> = listOf(
    BuiltinRecipe(
        key = "kasuya_46_standard", name = "粕谷哲 4:6（标准）",
        beanWeight = 20.0, totalWater = 300.0, ratio = 15.0,
        waterTemp = 92, grindSize = GrindSize.MEDIUM_FINE, difficulty = Difficulty.MEDIUM,
        source = "Tetsu Kasuya 4:6 Method",
        steps = listOf(
            BuiltinStep(StepActionType.BLOOM, 50.0, 13),
            BuiltinStep(StepActionType.WAIT, 0.0, 32),
            BuiltinStep(StepActionType.POUR, 70.0, 18),
            BuiltinStep(StepActionType.WAIT, 0.0, 27),
            BuiltinStep(StepActionType.POUR, 60.0, 15),
            BuiltinStep(StepActionType.WAIT, 0.0, 30),
            BuiltinStep(StepActionType.POUR, 60.0, 15),
            BuiltinStep(StepActionType.WAIT, 0.0, 30),
            BuiltinStep(StepActionType.POUR, 60.0, 15),
            BuiltinStep(StepActionType.WAIT, 0.0, 30)
        )
    ),
    BuiltinRecipe(
        key = "kasuya_46_sweet", name = "粕谷哲 4:6（偏甜）",
        beanWeight = 20.0, totalWater = 300.0, ratio = 15.0,
        waterTemp = 92, grindSize = GrindSize.MEDIUM_FINE, difficulty = Difficulty.MEDIUM,
        source = "Tetsu Kasuya 4:6 Method（偏甜调整）",
        steps = listOf(
            BuiltinStep(StepActionType.BLOOM, 40.0, 10),
            BuiltinStep(StepActionType.WAIT, 0.0, 35),
            BuiltinStep(StepActionType.POUR, 80.0, 20),
            BuiltinStep(StepActionType.WAIT, 0.0, 25),
            BuiltinStep(StepActionType.POUR, 60.0, 15),
            BuiltinStep(StepActionType.WAIT, 0.0, 30),
            BuiltinStep(StepActionType.POUR, 60.0, 15),
            BuiltinStep(StepActionType.WAIT, 0.0, 30),
            BuiltinStep(StepActionType.POUR, 60.0, 15),
            BuiltinStep(StepActionType.WAIT, 0.0, 30)
        )
    ),
    BuiltinRecipe(
        key = "qianjie_3stage", name = "前街三段式",
        beanWeight = 15.0, totalWater = 225.0, ratio = 15.0,
        waterTemp = 90, grindSize = GrindSize.MEDIUM, difficulty = Difficulty.EASY,
        source = "前街咖啡",
        steps = listOf(
            BuiltinStep(StepActionType.BLOOM, 30.0, 8),
            BuiltinStep(StepActionType.WAIT, 0.0, 22),
            BuiltinStep(StepActionType.POUR, 95.0, 24),
            BuiltinStep(StepActionType.WAIT, 0.0, 6),
            BuiltinStep(StepActionType.POUR, 100.0, 25),
            BuiltinStep(StepActionType.WAIT, 0.0, 5)
        )
    ),
    BuiltinRecipe(
        key = "v60_4stage", name = "经典 V60 四段式",
        beanWeight = 20.0, totalWater = 300.0, ratio = 15.0,
        waterTemp = 90, grindSize = GrindSize.MEDIUM, difficulty = Difficulty.MEDIUM,
        source = "通用 V60 四段注水",
        steps = listOf(
            BuiltinStep(StepActionType.BLOOM, 40.0, 10),
            BuiltinStep(StepActionType.WAIT, 0.0, 20),
            BuiltinStep(StepActionType.POUR, 110.0, 28),
            BuiltinStep(StepActionType.WAIT, 0.0, 2),
            BuiltinStep(StepActionType.POUR, 70.0, 18),
            BuiltinStep(StepActionType.WAIT, 0.0, 12),
            BuiltinStep(StepActionType.POUR, 80.0, 20),
            BuiltinStep(StepActionType.WAIT, 0.0, 10)
        )
    ),
    BuiltinRecipe(
        key = "light_single", name = "浅烘一刀流",
        beanWeight = 15.0, totalWater = 250.0, ratio = 16.7,
        waterTemp = 92, grindSize = GrindSize.MEDIUM_FINE, difficulty = Difficulty.HARD,
        source = "浅烘一刀流",
        steps = listOf(
            BuiltinStep(StepActionType.BLOOM, 30.0, 8),
            BuiltinStep(StepActionType.WAIT, 0.0, 22),
            BuiltinStep(StepActionType.POUR, 220.0, 55),
            BuiltinStep(StepActionType.WAIT, 0.0, 65)
        )
    ),
    BuiltinRecipe(
        key = "dark_3stage", name = "深烘三段式",
        beanWeight = 15.0, totalWater = 225.0, ratio = 15.0,
        waterTemp = 88, grindSize = GrindSize.MEDIUM_COARSE, difficulty = Difficulty.EASY,
        source = "深烘三段式",
        steps = listOf(
            BuiltinStep(StepActionType.BLOOM, 30.0, 8),
            BuiltinStep(StepActionType.WAIT, 0.0, 22),
            BuiltinStep(StepActionType.POUR, 65.0, 16),
            BuiltinStep(StepActionType.WAIT, 0.0, 24),
            BuiltinStep(StepActionType.POUR, 65.0, 16),
            BuiltinStep(StepActionType.WAIT, 0.0, 24),
            BuiltinStep(StepActionType.POUR, 65.0, 16),
            BuiltinStep(StepActionType.WAIT, 0.0, 24)
        )
    ),
    BuiltinRecipe(
        key = "hoffmann_ultimate", name = "James Hoffmann 终极 V60 法",
        beanWeight = 15.0, totalWater = 250.0, ratio = 16.7,
        waterTemp = 100, grindSize = GrindSize.MEDIUM_FINE, difficulty = Difficulty.MEDIUM,
        source = "James Hoffmann",
        steps = listOf(
            BuiltinStep(StepActionType.BLOOM, 30.0, 45),
            BuiltinStep(StepActionType.STIR, 0.0, 5),
            BuiltinStep(StepActionType.POUR, 120.0, 30),
            BuiltinStep(StepActionType.POUR, 100.0, 35),
            BuiltinStep(StepActionType.STIR, 0.0, 5)
        )
    ),
    BuiltinRecipe(
        key = "scott_rao_spin", name = "Scott Rao / Rao Spin",
        beanWeight = 20.0, totalWater = 340.0, ratio = 17.0,
        waterTemp = 96, grindSize = GrindSize.MEDIUM, difficulty = Difficulty.HARD,
        source = "Scott Rao",
        steps = listOf(
            BuiltinStep(StepActionType.BLOOM, 60.0, 40),
            BuiltinStep(StepActionType.STIR, 0.0, 5),
            BuiltinStep(StepActionType.POUR, 140.0, 50),
            BuiltinStep(StepActionType.POUR, 140.0, 35),
            BuiltinStep(StepActionType.STIR, 0.0, 5)
        )
    ),
    BuiltinRecipe(
        key = "perger_stir", name = "Matt Perger 搅拌法",
        beanWeight = 12.0, totalWater = 200.0, ratio = 16.0,
        waterTemp = 93, grindSize = GrindSize.MEDIUM_FINE, difficulty = Difficulty.HARD,
        source = "Matt Perger",
        steps = listOf(
            BuiltinStep(StepActionType.BLOOM, 50.0, 30),
            BuiltinStep(StepActionType.STIR, 0.0, 5),
            BuiltinStep(StepActionType.POUR, 50.0, 30),
            BuiltinStep(StepActionType.POUR, 100.0, 80)
        )
    ),
    BuiltinRecipe(
        key = "emi_gina", name = "Emi Fukahori GINA（WBC 2018）",
        beanWeight = 17.0, totalWater = 220.0, ratio = 12.9,
        waterTemp = 80, grindSize = GrindSize.COARSE, difficulty = Difficulty.MEDIUM,
        source = "Emi Fukahori（温度切换 80→95→80°C）",
        steps = listOf(
            BuiltinStep(StepActionType.BLOOM, 50.0, 45),
            BuiltinStep(StepActionType.POUR, 100.0, 60),
            BuiltinStep(StepActionType.POUR, 70.0, 45)
        )
    )
)
