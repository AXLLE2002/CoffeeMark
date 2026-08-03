# CoffeeMark UI 高级感升级计划书（Luxe Refresh）

> 目标：在「好看、暖、舒服」的基础上，补齐「高级 / 奢华 / 精致」三种质感。
> 定位：原生 Android（Material 3）+ 中文 UI 的 *Operate* 模式（工具型 App）。品牌活在细节里，不靠炫技。
> 引用方法论：impeccable（critique / bolder / typeset / layout / animate / delight）、frontend-design（token + signature）、design-taste-frontend（premium-consumer 调参 + AI-Tell 禁忌）。

---

## 0. 设计读（Design Read）

> 读为：一款中文手冲咖啡工具 App，受众是讲究器具、风味与仪式感的重度咖啡用户，语言走「 café noir / 精品咖啡馆」式的克制奢华，倾向 Material 3 体系内的精致演化，而非另起炉灶的视觉革命。

一句话美学方向：**Café Noir（咖啡馆夜色）**。以现有的暖奶油底为日常画布，引入一层「深咖近黑」的奢华强调面，单点保留 Caramel 焦糖作为唯一高饱和点缀，配高对比衬线标题 + 暖色调柔和投影 + 克制而有节奏的动效。把「高级感」押在排版、材质深度与签名母题三件事上，其余一律安静。

---

## 1. 为什么现在「好看但不高级」

对照 impeccable 的 *critique* 与 taste 的 premium 调参，当前问题集中在五点：

1. **字体没有性格**。全站 Roboto（Material 默认），标题与正文同族同感，无衬线对比、无字距经营，这是「便宜感」的最大来源（taste Levers 1：排版是性价比最高的杠杆）。
2. **强调色太「安卓默认」**。Caramel `#FF8A65` 是偏亮的珊瑚橙，友好但不够贵。它出现在每一个按钮、标签、进度上，全站只有这一种强调，缺少「深」的那一层来托住它。
3. **卡片是扁平实色 + 描边**。`CoffeeCard` 用 `outlineVariant` 描边 + 6dp 硬投影，所有卡长得一样、平铺，没有层级与材质语言。
4. **没有呼吸感与节奏**。列表与详情页间距偏功能化，缺少刻意留白与节奏（taste：奢华来自克制与空间）。
5. **没有签名母题**。缺少一个让人记住的、只属于 CoffeeMark 的视觉符号（frontend-design：把大胆押在一处）。

---

## 2. 不可逾越的约定（沿用你已敲定的决定）

- **第八阶段（玻璃质感 / 强制深色 / 深色色板微调）已永久放弃**。本次**不引入任何 glassmorphism**，不做强制深色，**不改深色模式色板**（含 V1.2.0 的 Stage I）。深色模式继续跟随系统，复用现有 `Dark*` token。
- **AmbientBackground 暖色渐变背景保留**，`CoffeeCard` 保持实色卡（不透明）。
- **Caramel 品牌色保留为唯一强调色**（你已否决换成铜色）。下文如需微调其色相，会明确标注「需你确认」，默认不换。
- 一次只换一个系统：继续以 Material 3 为基座，不混用其它设计体系。

---

## 3. 美学方向与签名元素（Signature）

**签名母题：Coffee Ring（咖啡渍圆环）**。一个极简的双描边圆环，内嵌一颗咖啡豆点。它只出现在少数几处高价值位置：App 启动 / 品牌动画的收尾、空状态中心、详情页大标题前的装饰、加载骨架的占位。把「大胆」只花在这一处，其余界面保持安静（frontend-design：spend your boldness in one place）。

**奢华感的三个支柱（按性价比排序）：**
1. 排版（衬线标题 + 精确字距 + 更大级差）
2. 材质深度（暖色柔影 + 顶部高光 + 一致的圆角尺度）
3. 节奏与动效（留白 + 弹簧物理 + 错落入场）

---

## 4. 设计 Token 提案

> 只扩不砍：在现有 `Color.kt` / `Type.kt` 上增量补充，复用已有 `Dark*` 与 `Caramel` 系列。

### 4.1 颜色（Color.kt 增量）

| Token | 值 | 用途 |
|---|---|---|
| `Noir` | `#211713`（暖近黑） | 奢华强调面：启动页、详情页头图、 hero 卡、空状态底 |
| `OnNoir` | `#F3E9DF` | Noir 面上的文字 |
| `NoirSurfaceVariant` | `#2E231D` | Noir 面上的次级面 |
| `Hairline` | `#E7DDD3`（亮）/ `#3D3835`（暗） | 发丝级分隔线，取代粗描边 |
| `SurfaceTint` | `#F1E9E2` | 分组背景、被选中项的淡底 |
| `Caramel`（保留） | `#FF8A65` | 唯一高饱和强调色，锁死 |
| `CaramelDeep`（**需确认**） | `#F2774E` | 可选：把焦糖压深一档更显贵；默认不启用 |

阴影改为**暖色投影**（非纯黑）：`shadowColor = CoffeeBrown.copy(alpha = 0.18)`，配 `ambient` + `spot` 双层。

> 为什么不是「米色+黄铜」那套 AI 默认奢华色：taste 明确禁用 beige+brass+espresso 作为默认。本方案走 taste 允许的 **Black and Tan** 变体（暖近黑 + 单一暖强调、强对比、无米色），且主体确为咖啡题材，色板有正当性，不是偷懒的默认。

### 4.2 字体（Type.kt，最大杠杆）

推荐 **A 方案（衬线标题）**，需新增字体资源并做中文子集化（见风险）：
- 标题 / 大数字：`Noto Serif SC`（思源宋体）用于中文标题；Latin 与计时数字用高对比衬线（如 Cormorant / Playfair Display，避开被 bans 的 Fraunces / Instrument_Serif）。
- 正文 / 标签：保留无衬线（Noto Sans SC 或系统），但收紧字距、拉开行高。

若不想加字体包，用 **B 方案（纯无衬线奢华）**。不引入新字体，靠「更大标题级差 + 更重字重 + 负字距 + 数字等宽（tabular figures）」做出高级感，零资源成本。

统一字号级差（示例）：
- `displayLarge` 40.sp / `letterSpacing = -0.5.sp`（hero 标题）
- `headlineMedium` 26.sp / `letterSpacing = -0.3.sp`
- `titleLarge` 20.sp / 字重 Medium
- 正文 `bodyLarge` 16.sp / `lineHeight = 26.sp`（更松）
- 小标签 `labelMedium` 12.sp / `letterSpacing = 0.08.em` 仅用于极少数 eyebrow，全站不超过 3 处

### 4.3 圆角 / 间距 / 阴影（Shape & Spacing 尺度）

- 圆角一致性锁定：卡 16dp、hero/feature 24dp、按钮与 FAB 全圆角（pill）、输入 12dp。
- 间距尺度：4 / 8 / 12 / 16 / 24 / 32 / 48 / 64 dp，全站只用这套。
- 投影：默认卡 `elevation 1dp + 暖色柔影`；hero/Noir 卡 `elevation 3dp + 更大暖影 + 顶部 1px 高光内描边`。

### 4.4 动效（MotionTokens）

- 入场：弹簧 `stiffness 380 / damping 30`，时长 320-480ms，列表项错落 `stagger 48ms`。
- 按压：按钮/卡片 `scale 0.98` + 轻微上移，物理反馈。
- 页面切换：共享元素式淡入上移（非横向滑动）。
- **必须**尊重 `rememberReduceMotion()`（项目已有此工具），降运动时全部退化为瞬时。

---

## 5. 分阶段实施计划

### Phase 0：设计系统锚定（先把 Token 立住）
- 改 `ui/theme/Color.kt`：增 `Noir / OnNoir / NoirSurfaceVariant / Hairline / SurfaceTint`（亮暗两套）。
- 改 `ui/theme/Type.kt`：按 4.2 调整级差与字距；如选 A 方案，接入 `Noto Serif SC` + 衬线 Latin，并用 `FontFamily` 绑定。
- 新增 `ui/theme/Shape.kt`、`ui/theme/Elevation.kt`、`ui/theme/Motion.kt`（圆角尺度 / 暖色投影 / 弹簧参数）。
- 改 `CoffeeCard.kt`：从「描边+硬影」升级为「实色面 + 暖色柔影 + 顶部高光内描边 + 圆角 16dp」，保留实色（不透明）。

### Phase 1：排版升级（typeset，感知提升最大）
- 全站标题改用衬线（A 方案）或负字距大级差（B 方案）。
- 计时数字（冲煮引导 `02:30`、用量）启用等宽数字，显贵。
- 详情页大标题用 display 级，配小号 eyebrow（如「豆仓 / 方案 / 记录」分类标签），全站 eyebrow 不超过 3 处。

### Phase 2：材质与深度（CoffeeCard + AmbientBackground）
- `AmbientBackground` 叠加一层极淡颗粒/噪点（固定、不可点击、`pointer-events-none` 等价实现），做出「纸感/哑光」奢华质感，不破坏暖渐变。
- 列表卡与详情卡统一圆角与暖影；选中/分组项改用 `SurfaceTint` 淡底而非描边。
- 分隔线从粗描边改为 `Hairline` 发丝线，仅在必要时出现。

### Phase 3：列表与卡片精致化（layout / distill）
- 三个列表页（Bean / Recipe / BrewLog）：行改为「编辑式」排版：名称更大更紧，左侧加 **Bean Monogram**（圆形首字 / 小圆环显示剩余比例），右侧次要信息收敛。
- FAB：由圆形 `+` 改为 **扩展药丸 FAB**（图标 + 文案，如「加豆」「记一杯」），软影 + 按压回弹。
- 详情页：杂志式头图（Noir 面 + 衬线大标题 + 关键指标一行），内容即界面。

### Phase 4：动效（animate / delight）
- 列表入场错落（stagger 48ms，弹簧）。
- 按钮/卡片按压 `scale 0.98`。
- 页面间淡入上移切换；冲煮引导 hero 已有沉浸光晕， refinement 其弧光为更克制的扫光。
- 全程接 `rememberReduceMotion()`。

### Phase 5：签名母题与空状态（signature / onboard）
- 落地 **Coffee Ring** 母题：启动品牌动画收尾、空状态中心、详情页标题前装饰、骨架屏占位。
- 空状态从「图标+一句话」升级为构图式（Noir 或 Cream 底 + Coffee Ring + 一句引导文案 + 主操作），对齐 taste 的 empty-state 即邀请。
- 错误/加载态同样精致化（骨架形状贴合最终布局，非转圈）。

### Phase 6：深色模式一致性（dark parity）
- 用现有 `Dark*` token 承接 Noir / Hairline / SurfaceTint，确保浅色下的奢华层级在深色里同样成立（对比、品牌识别、WCAG AA）。
- **不新增**深色色板，仅复用。

---

## 6. 风险、验证与取舍

- **字体包体积（A 方案核心风险）**。中文衬线（Noto Serif SC）完整体数 MB，需做 **字体子集化**（仅打包用到的字形 / 或只用于标题少量字）。若接受不了体积，退回 B 方案（纯无衬线奢华），质感仍明显提升。
- **强调色微调需你拍板**。`CaramelDeep` 与是否引入 Noir 面，属品牌表达，默认保留 Caramel、默认加 Noir 面；如不想加深层面，可只做排版+材质+动效三件事，同样能「高级」。
- **验证方式**。本机 Gradle Wrapper 不完整，需在 Android Studio Build + 真机（浅色 / 深色 / 降运动）三态走查；重点看列表错落、按压反馈、Noir 面对比、Coffee Ring 母题是否「只出现在一处」不过度。
- **AI-Tell 自检**（taste Pre-Flight）：全站零 em-dash；单一强调色；单一圆角体系；无装饰性状态点滥用；无版本标签 / 滚动提示 /  locale 条；空状态有真实引导；动效皆「有动机」。

---

## 7. 明确不在本次范围

- 玻璃质感 / Liquid Glass（已放弃的第八阶段）。
- 强制深色模式、深色色板微调（已放弃的第八阶段 / V1.2.0 Stage I）。
- 把 Caramel 换成铜色等其它强调色（你已否决）。
- 任何改变导航标签、路由、表单字段名、SEO/统计事件的改动（不在 UI 质感范畴）。
- 功能新增（排序、预填等已有功能不在本次动刀范围，除非视觉需要适配）。

---

## 8. 建议落地节奏

按 impeccable 的 *refinement preserves* 原则：**Phase 0 → 1 → 2 → 3** 即可拿到 70% 的「高级感」且风险最低；**Phase 4 → 5** 是「奢华 / 精致」的临门一脚；**Phase 6** 收尾。每一阶段独立可编译、可回退。
