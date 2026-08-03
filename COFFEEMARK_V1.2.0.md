# CoffeeMark V1.2.0 计划书

> 本版本 = **2 个功能 bug 修复（阶段 A/B）** + **高级感优化（第二部分，合并自 V1.3.0 竞品调研）**。无数据库迁移；字体/涟漪为新引入资源但无新增依赖。
> UI overhaul 总计划见 `COFFEEMARK_UI_OVERHAUL.md`；玻璃质感已在 V1.1.x 正式放弃，本文不涉玻璃。
> 文档独立成文，体积适中，WorkBuddy 预览不会卡；白天用 Typora 看，晚上把本文档交给开发机 agent 照执行即可。

---

<a id="toc"></a>
## 目录
- [阶段 A：冲煮准备粉重未传递到引导页（P0 功能 bug）](#secA)
- [阶段 B：冲煮准备界面输入框被输入法遮挡（P1）](#secB)
- [第二部分：高级感优化（合并自 V1.3.0 竞品调研）](#sec2)
  - [设计方向 token（全局参考）](#sec2tok)
  - [阶段 C：字体层级升级（P0）](#secC)
  - [阶段 D：分层软阴影深度系统（P0）](#secD)
  - [阶段 E：间距与留白纪律（P0）](#secE)
  - [阶段 F：Pour Ripple 签名元素（P1）](#secF)
  - [阶段 G：克制而一致的微交互（P1）](#secG)
  - [阶段 H：冲煮引导英雄打磨（P1）](#secH)
  - [阶段 I：深色模式高级化（P2）](#secI)
  - [阶段 J：数据可视化精致化（P2）](#secJ)
  - [阶段 K：空状态与文案（P2）](#secK)
  - [阶段 L：首次启动微引导（P2）](#secL)
- [执行清单总表](#sectotal)

---

<a id="secA"></a>
## 阶段 A：冲煮准备粉重未传递到引导页（P0 功能 bug）

### A.1 现象
在「冲煮准备」界面修改豆量（粉重）后，输入框下方的预览文字「总注水 ≈ XXXg · 各阶段按粉水比自动缩放」会跟着变；但点「开始」进入引导页后，各阶段提示加水量仍是**模板默认水量**，粉重调整没有生效。

### A.2 根因（已读源码确认）
数据流断在 `MainScreen.kt` 的导航衔接，**不是** ViewModel 或缩放算法的问题：

1. `BrewPrepareScreen` 点「开始」调用 `onStart(dose)`，把用户输入的 `dose: Double?` 传了出来（BrewPrepareScreen.kt:137 `onStart(dose)`）。
2. 但 `MainScreen.kt:395` 的 `onStart` lambda **把 dose 参数丢弃了**：
   ```kotlin
   onStart = { navController.navigate(Routes.brewGuide(recipeId)) }  // ← dose 没传
   ```
3. 而且 BREW_GUIDE 的 composable（`MainScreen.kt:417-422`）只从路由取了 `recipeId`，**没有取 `dose`** 传给 `BrewGuideScreen`，于是 `BrewGuideScreen` 的 `dose` 一直走默认值 `null`。
4. `BrewGuideViewModel.scaleToDose()` 收到 `dose == null` → 直接返回未缩放的模板基准（`BrewGuideViewModel.kt:100-102`）。所以引导页水量不变。

**好消息**：路由与缩放能力本来就支持 dose——
- `Routes.brewGuide(recipeId, dose)` 已能拼出 `brew/guide/{recipeId}?dose={dose}`（NavGraph.kt:88-89）
- 路由声明已含 `?dose={dose}`（NavGraph.kt:84）
- `BrewGuideScreen` / `BrewGuideViewModel` 都已接收 `dose` 并正确缩放

所以**只需改 `MainScreen.kt` 两处**，把 dose 接住、传下去即可，ViewModel 和缩放算法一行都不用动。

### A.3 修改点（仅 1 个文件：`ui/MainScreen.kt`）

**改动 1 — 准备页「开始」时把 dose 带进路由**（约 395 行）：
```kotlin
// 改前
onStart = { navController.navigate(Routes.brewGuide(recipeId)) },
// 改后
onStart = { dose -> navController.navigate(Routes.brewGuide(recipeId, dose)) },
```

**改动 2 — BREW_GUIDE 路由声明里补上 dose 参数**（约 401-402 行）：
```kotlin
// 改前
route = Routes.BREW_GUIDE,
arguments = listOf(navArgument("recipeId") { type = NavType.StringType }),
// 改后
route = Routes.BREW_GUIDE,
arguments = listOf(
    navArgument("recipeId") { type = NavType.StringType },
    navArgument("dose") {
        type = NavType.StringType
        nullable = true
        defaultValue = null
    }
),
```
> 说明：`dose` 以字符串形式在 URL 里传递，下一步再 `toDoubleOrNull()` 解析，避免 Float 精度问题。`NavType` 在本文件已导入，无需加 import。

**改动 3 — composable 内取出 dose 并传给 BrewGuideScreen**（约 417-422 行）：
```kotlin
// 改前
) { backStackEntry ->
    val recipeId = backStackEntry.arguments?.getString("recipeId") ?: return@composable
    BrewGuideScreen(
        recipeId = recipeId,
        onFinished = { navController.navigate(Routes.brewComplete(recipeId)) }
    )
}
// 改后
) { backStackEntry ->
    val recipeId = backStackEntry.arguments?.getString("recipeId") ?: return@composable
    val dose = backStackEntry.arguments?.getString("dose")?.toDoubleOrNull()
    BrewGuideScreen(
        recipeId = recipeId,
        dose = dose,
        onFinished = { navController.navigate(Routes.brewComplete(recipeId)) }
    )
}
```

### A.4 不需要改的（已确认正确，别重复改）
- `BrewGuideViewModel.scaleToDose()`：收到非 null dose 时 `factor = dose / 基准粉量`，各阶段 `waterAmount` 等比缩放、`totalWater` 同步缩放、ratio 保持（BrewGuideViewModel.kt:95-111）。逻辑正确。
- `Routes.brewGuide` / 路由声明中的 `?dose={dose}`：已就绪。
- `BrewPrepareScreen` 的 dose 计算与预览文字：已正确，无需动。

### A.5 验收标准
1. 打开任一方案 → 冲煮准备 → 豆量框输入 `15`（原模板 20）→ 预览文字显示「总注水 ≈ 225g」（20g→300g，15g→225g）。
2. 点「开始」进入引导页，首步提示加水量应为缩放后的值（如 225g 对应各阶段按比例缩小），**不再是模板默认 300g**。
3. 不改豆量直接开始 → `dose` 为 null → 引导页行为与之前一致（用模板基准），不报错。
4. 各阶段水量**占比不变**，仅总量随粉重线性缩放；粉水比徽标 1:15 不变。

---

<a id="secB"></a>
## 阶段 B：冲煮准备界面输入框被输入法遮挡（P1）

### B.1 现象
「冲煮准备」界面的豆量输入框，点击后输入法弹出，但输入框**无法完全上移**，仍被输入法遮挡，看不到输入内容（其余 3 个编辑页在 V1.1.x 已修好，唯独这个准备页没修彻底）。

### B.2 根因
`BrewPrepareScreen.kt` 的内容容器是 `Column`，已加了 `imePadding()`，但同时还用了 `verticalArrangement = Arrangement.Center`：

```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .consumeWindowInsets(innerPadding)
        .imePadding()
        .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center   // ← 问题所在
) {
```

`imePadding()` 确实为键盘留出了空间，但**居中布局**会把整块内容在「屏幕高度 − 键盘高度」的压缩可视区里居中。本页内容偏高（图标 + 名称 + 比例徽标 + 信息卡 + 输入框 + 提示 + 按钮），当键盘弹出后可用高度变小，居中会让输入框被压到键盘上沿或超出可视区被裁切，于是「弹不上去」。

### B.3 修改点（仅 1 个文件：`ui/brewguide/BrewPrepareScreen.kt`）

**改动 1 — 让内容可滚动 + 顶部对齐**（约 66-75 行）：
```kotlin
// 改前
Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .consumeWindowInsets(innerPadding)
        .imePadding()
        .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
) {
// 改后
Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .consumeWindowInsets(innerPadding)
        .verticalScroll(rememberScrollState())
        .imePadding()
        .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Top
) {
```

**改动 2 — 补两个 import**（文件顶部，约第 3 行 `androidx.compose.foundation.layout.*` 附近）：
```kotlin
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
```
> 注意：`verticalScroll` / `rememberScrollState` 在 `androidx.compose.foundation`（非 `.layout`），需显式导入，不能靠通配导入覆盖。

### B.4 说明
- 改为 `Arrangement.Top` + `verticalScroll` 后，无键盘时内容从顶部排布（原居中观感略有变化，属可接受的功能性取舍）；键盘弹出时滚动容器自动收缩在键盘之上，且 Compose 的 TextField 在获得焦点时会自动 `bringIntoView`，输入框必能完整显示。
- 这是 V1.1.x「第十阶段 IME 适配」在准备页的**补全**（那次只加了 `imePadding()`，没解决居中导致的遮挡），与另外 3 个编辑页的修法一致。

### B.5 验收标准
1. 进入冲煮准备，点豆量输入框 → 输入法弹出，输入框完整显示在键盘上方、可见光标与输入内容。
2. 键盘收起后内容回到正常排布，无多余留白、无双倍 inset。
3. 在内容超高（小屏手机）情况下也能滚动查看「开始」按钮，不被键盘挡死。

---

<a id="sec2"></a>
## 第二部分：高级感优化（合并自 V1.3.0 竞品调研）

> 来源：anysearch 竞品扫描（Timer.Coffee / FourSix / Pareto / Brygge）+ frontend-design 方法论。
> 目标：让 App「看起来更高级」，且**不重试玻璃**、**不堆动画**、**不落 AI 默认奶油+衬线+赤陶俗套**。
> 设计原则：一处大胆、其余克制；全 App 只押一个签名元素——**注水涟漪（Pour Ripple）**。

### 设计方向 token（全局参考）<a id="sec2tok"></a>

**Color（亮色）**
| 角色 | Hex | 说明 |
|---|---|---|
| 背景 Base | `#F6F1E9` | 暖奶油（沿用品牌 OnCream 调性） |
| 表面 Surface | `#FFFCF7` | 卡面，略亮于底 |
| 墨色 Ink | `#2A2018` | 深咖近黑，不用纯黑 |
| 主强调 Accent | `#BE6E2C` | 精炼铜琥珀（非赤陶），一处大胆 |
| 次强调 | `#EFE3D2` | crema 米色，仅分隔/底色 |
| 描边 Hairline | `rgba(42,32,24,0.08)` | 1px 细线 |
| 阴影 | `0 1px 2px` / `0 8px 24px`（暖色） | 分层软阴影 |

**Color（深色）**：背景 `#16110D`、表面 `#1F1813`/`#241C16`、墨 `#F3ECE2`、强调 `#E0913F`。
**Type**：Display=`Fraunces`(light，仅英雄数字/大标题)；UI/Body=`Manrope` 或 `Inter`；计时器用 tabular 数字。
**Signature**：Pour Ripple——冲煮引导径向进度外叠加同心涟漪，每进入注水阶段从中心泛起一圈淡琥珀环。
> 文档中 `Accent` / `Ink` / `Hairline` 等名称对应本 App `ui/theme/Color.kt` 里的主强调/墨色/描边常量，**以 Color.kt 实际命名为准**，不要照抄字面。

---

<a id="secC"></a>
## 阶段 C：字体层级升级（P0）

### C.1 目标
现在的 Material 默认字阶让标题/数字/正文一个调性，缺「贵气」。引入 `Fraunces`(Display) + `Manrope`(UI)，让大标题与计时数字有编辑感，正文干净几何。

### C.2 准备（开发机执行）
从 Google Fonts 下载 `Fraunces`(Light/Regular) 与 `Manrope`(Regular/Medium/SemiBold) 的 `.ttf`，放入 `app/src/main/res/font/`：
- `fraunces_light.ttf` / `fraunces_regular.ttf`
- `manrope_regular.ttf` / `manrope_medium.ttf` / `manrope_semibold.ttf`

### C.3 修改点（新文件 `ui/theme/Type.kt`）
```kotlin
package com.coffeemark.app.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle

val Fraunces = FontFamily(
    Font(R.font.fraunces_light, FontWeight.Light),
    Font(R.font.fraunces_regular, FontWeight.Normal)
)
val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold)
)

val AppTypography = Typography(
    displayLarge  = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Light, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Light, fontSize = 45.sp),
    headlineMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
    titleLarge   = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    bodyLarge    = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium   = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge   = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 14.sp) // 按钮文字
)
```

### C.4 接入 Theme
在 `ui/theme/Theme.kt` 的 `CoffeeMarkTheme` 里，把 `typography = Typography` 改为 `typography = AppTypography`；计时器数字加 tabular：
```kotlin
Text(
    text = "02:30",
    style = AppTypography.displayMedium,
    fontFeatures = FontFeatureSettings("tnum")   // 数字等宽，跳动更稳
)
```
> `FontFeatureSettings` 在 `androidx.compose.ui.text` 随 Compose 1.6 已提供，无需新依赖。

### C.5 验收
1. 冲煮引导的大计时数字变为 Fraunces 细体，明显比之前「贵」。
2. 全 App 正文为 Manrope，清爽；计时数字不抖动（tabular）。
3. 深色模式下字体清晰，无纯黑标题。

---

<a id="secD"></a>
## 阶段 D：分层软阴影深度系统（P0）

### D.1 目标
卡片偏平、缺悬浮感。用**多层柔影 + 顶部高光**替代玻璃（对标 FourSix 的 Liquid Glass 观感，但不掉帧）。

### D.2 修改点
**新文件 `ui/theme/Elevation.kt`**：
```kotlin
import androidx.compose.ui.unit.dp
object Elevation {
    val sm = 2.dp
    val md = 6.dp
    val lg = 12.dp
}
```
**改 `ui/theme/CoffeeCard.kt`**（`CoffeeCard` 的 Modifier）：
```kotlin
// 改前（示意）
Modifier.shadow(6.dp, shape)
// 改后：多层暖色柔影 + 顶部 1dp 高光描边
Modifier
    .shadow(Elevation.lg, shape, spotColor = Accent.copy(alpha = 0.16f), ambientColor = Ink.copy(alpha = 0.10f))
    .shadow(Elevation.sm, shape, spotColor = Ink.copy(alpha = 0.08f))
    .border(1.dp, Hairline, shape)
```
> `shadow` 第二参数 `shape` 沿用 CoffeeCard 现有 shape；`spotColor/ambientColor` 在 Compose 1.6 的 `Modifier.shadow` 已支持。

### D.3 验收
1. 卡片有「浮起来」的层次感，柔和不生硬。
2. 滚动时阴影不闪烁、中端机也不掉帧。
3. 深色模式下阴影更隐、高光描边给出边缘。

---

<a id="secE"></a>
## 阶段 E：间距与留白纪律（P0）

### E.1 目标
统一 8dp 栅格；主按钮高度 ≥56dp；减少分割线，改用留白分区。

### E.2 修改点（散布各 Screen）
- 列表/卡片内边距：从 `16.dp` 提到 `20~24.dp`；卡片间距从 `12.dp` 提到 `16.dp`。
- 主操作按钮（开始冲煮、保存）：`Modifier.height(56.dp).fillMaxWidth()`，圆角 `16.dp`。
- 列表项之间用 `16.dp` 留白代替 `Divider`，仅分组边界保留 1px Hairline。

> 一致性改动，不必逐文件列代码；开发机 agent 按上述规则扫一遍 beans/recipes/brewlogs/brewguide 各 Screen 的 padding 即可。重点：**不要引入新分割线**。

### E.3 验收
1. 各页面呼吸感明显，不再拥挤。
2. 主按钮够大、好点；圆角统一。

---

<a id="secF"></a>
## 阶段 F：Pour Ripple 签名元素（P1）

### F.1 目标
全 App 唯一的「大胆点」：冲煮引导的径向进度外，叠加**同心涟漪**——每次进入注水阶段，从中心泛起一圈淡琥珀环（画圈注水 + 水面波纹）。

### F.2 修改点（改 `ui/brewguide/BrewGuideScreen.kt` 的进度区）
在径向进度 `Canvas` 之外叠一层涟漪 `Canvas`，用 `Animatable` 驱动：
```kotlin
val ripple = remember { Animatable(0f) }
LaunchedEffect(currentStageIsPour) {
    if (currentStageIsPour) {
        ripple.snapTo(0f)
        ripple.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))
    }
}
Canvas(Modifier.size(240.dp)) {
    val t = ripple.value
    val maxR = size.minDimension / 2
    listOf(0f, 0.35f).forEach { offset ->
        val local = (t - offset).coerceIn(0f, 1f)
        drawCircle(
            color = Accent.copy(alpha = (1f - local) * 0.35f),
            radius = local * maxR,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
```
> 仅在「注水阶段」触发（`currentStageIsPour` 由阶段类型判断），其余阶段安静。遵守 reduceMotion：若系统开启「减少动态效果」则不播（画一圈静态即止）。

### F.3 验收
1. 点开始冲煮 → 进入首次注水，中心泛起 1~2 圈琥珀涟漪，自然消散。
2. 非注水阶段无涟漪，界面安静。
3. 系统开启「减少动态效果」时，涟漪不播、不报错。

---

<a id="secG"></a>
## 阶段 G：克制而一致的微交互（P1）

### G.1 目标
保存成功 ✓ 形变动画、步骤切换柔脉冲、按钮按压调优、开始冲煮「bloom」展开；**全局遵守 reduceMotion**。

### G.2 修改点（原则，开发机落地）
- 保存/新增成功：用 `AnimatedVisibility` + 勾选图标缩放淡入（已有 `AnimatedVisibility` 基础，补图标形变）。
- 步骤切换：`BrewGuideScreen` 阶段卡片切换加 `Crossfade` 或 `AnimatedContent`（300ms，easeOut）。
- 所有动画包在 `if (!reduceMotion) ... else 静态` 分支。
- 不新增散点动画；只在「保存反馈」「阶段切换」「开始 bloom」三处。

### G.3 验收
1. 保存记录后看到轻量 ✓ 反馈。
2. 步骤切换平滑不突兀。
3. 开启减少动态效果后，所有动效降级为即时切换，无跳动。

---

<a id="secH"></a>
## 阶段 H：冲煮引导英雄打磨（P1）

### H.1 目标
把引导页中心做成「英雄时刻」：随注水填充的扫光（沿用 stage6 径向）+ 阶段 F 涟漪 + 中心大数字用 Display 字体（Fraunces）。

### H.2 修改点
- 引导页中心 `Text` 计时数字改用 `AppTypography.displayMedium` + tabular。
- 径向进度 `Brush.sweepGradient`（已有）叠加阶段 F 涟漪 Canvas。
- 「注水进度」用 accent 弧填充，背景弧用 Hairline。

### H.3 验收
1. 引导页中心是视觉锚点，数字大而精致。
2. 注水时弧与涟漪同步生长，有「注入感」。

---

<a id="secI"></a>
## 阶段 I：深色模式高级化（P2）

- 深色表面用 `#1F1813`/`#241C16`（深 espresso，非纯灰），`AmbientBackground` 暗色光斑更收敛高级。
- 暗底强调色 `#E0913F` 更跳；检查各 Screen 暗色下对比度（尤其 Caption 文字）。
- 已有 `isSystemInDarkTheme()` 跟随，I 阶段只调色板与光斑参数。

---

<a id="secJ"></a>
## 阶段 J：数据可视化精致化（P2）

- `BeanUsagePieCard` 中心显示「总冲煮数」+ 一句洞察（如「最爱：耶加雪菲」），用 `AppTypography` 小字。
- 图例已可滑动（之前已完成），此处仅中心加洞察行，不改滚动逻辑。

---

<a id="secK"></a>
## 阶段 K：空状态与文案（P2）

- 空列表/空记录屏用「行动邀请」替代空占位：豆库空 →「还没有豆子，添加第一支吧」；记录空 →「今天来一杯？」。
- 用品牌语气重写占位文案，统一在 `ui/theme/Strings.kt` 或各 Screen 常量。

---

<a id="secL"></a>
## 阶段 L：首次启动微引导（P2）

- 首次启动轻柔欢迎淡入 + 单条引导提示（非重型 tour）。
- 遵守 reduceMotion；只出现一次（用 `DataStore` 的 `firstLaunch` 布尔标记，若已有则复用，否则新增），可跳过。

---

<a id="sectotal"></a>
## 执行清单总表

| 阶段 | 优先级 | 文件 | 改动量 | 是否需要迁移 |
|---|---|---|---|---|
| A 粉重传递到引导页 | P0 | `ui/MainScreen.kt`（3 处） | ~10 行 | 否 |
| B 准备页输入框防遮挡 | P1 | `ui/brewguide/BrewPrepareScreen.kt`（2 处 + 2 import） | ~6 行 | 否 |
| C 字体层级升级 | P0 | `ui/theme/Type.kt`(新) + `Theme.kt` | 中（含字体资源） | 否 |
| D 分层软阴影 | P0 | `ui/theme/Elevation.kt`(新) + `CoffeeCard.kt` | 小 | 否 |
| E 留白纪律 | P0 | 各 Screen padding | 中（散布） | 否 |
| F Pour Ripple 签名 | P1 | `ui/brewguide/BrewGuideScreen.kt` | 中 | 否 |
| G 克制微交互 | P1 | 各 Screen | 小 | 否 |
| H 引导英雄打磨 | P1 | `ui/brewguide/BrewGuideScreen.kt` | 小 | 否 |
| I 深色精修 | P2 | `Color.kt` / `AmbientBackground` | 小 | 否 |
| J 图表精致 | P2 | `ui/beans/BeanListScreen.kt` | 小 | 否 |
| K 空状态文案 | P2 | 各 Screen | 小 | 否 |
| L 首启微引导 | P2 | 新 `firstLaunch` 标记 | 小 | 否（复用 DataStore） |

**总原则**：
- A/B 是纯 UI/导航小修（`BrewGuideViewModel`/`Routes`/数据库不动，编译风险低）。
- C~L 是高级感优化：C（字体）与 F（涟漪动画）涉及新资源/新组件，编译风险中等，需**真机验证**；其余多为散布调整。
- **不引入新依赖**（字体用 `res/font` 本地打包，动画用 Compose 自带 API）。
- **不重试玻璃质感**、**动画集中在签名涟漪一处**、**遵守 reduceMotion**——见第二部分开头原则。
- 白天只给计划书；晚上开发机编译 + Android Studio USB 真机验证（重点验 A 缩放生效、B 键盘不遮挡、C/D 观感、F 涟漪触发）。
