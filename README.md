# ☕ CoffeeMark — 手冲咖啡助手

一杯好咖啡，从精准记录开始。

CoffeeMark 是一款面向手冲咖啡爱好者的 Android 应用，帮助你管理咖啡豆、记录冲煮方案、追踪每一次冲煮过程，并提供分步语音引导。

---

## ✨ 功能概览

| 模块 | 说明 |
|---|---|
| 📖 **方案库** | 创建和管理手冲方案，支持多步骤编辑（闷蒸/注水/搅拌/等待），参数包含粉水比、水温、研磨度、器具等 |
| 🫘 **豆仓** | 管理咖啡豆库存，追踪产地、烘焙度、烘焙日期、开封状态，自动计算每克单价 |
| 📝 **冲煮记录** | 记录每次冲煮的完整数据，支持心情标记和风味笔记，日历视图查看历史 |
| 🎙️ **冲煮引导** | 选择方案后进入分步引导模式，TTS 语音逐步骤播报，解放双手专注冲煮 |

---

## 📱 预览

> *截图待补充 — 运行 App 后截取四个主界面贴在此处*

---

## 🛠️ 技术栈

| 类别 | 技术选型 |
|---|---|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material3 |
| 架构 | MVVM（ViewModel + StateFlow） |
| 数据库 | Room (SQLite) |
| 导航 | Compose Navigation（单 Activity + 底部三 Tab） |
| 语音 | Android TTS (TextToSpeech) |
| 最低版本 | Android 8.0 (API 26) |
| 目标版本 | Android 14 (API 34) |

---

## 🎨 设计风格

**现代暖调精致** — 咖啡棕 `#5D4037` + 奶白 `#FAF7F4` + 焦糖 `#FF8A65`，暗色模式深棕底色，Material Icons Rounded 图标，微动效交互。

详见 [`UI设计规范.md`](UI设计规范.md)

---

## 🏗️ 项目结构

```
app/src/main/java/com/coffeemark/app/
├── navigation/          # 路由定义 & 底部导航
├── data/
│   ├── entity/          # Room 数据表实体
│   ├── dao/             # 数据访问对象
│   ├── enums/           # 枚举类型（研磨度、器具、心情等）
│   ├── converter/       # 类型转换器
│   └── repository/      # 数据仓库层
├── ui/
│   ├── theme/           # 色彩 & 字体 & 主题
│   ├── recipes/         # 方案列表 / 详情 / 编辑
│   ├── beans/           # 豆仓列表 / 详情 / 编辑
│   ├── brewlogs/        # 冲煮记录列表 / 详情 / 编辑
│   ├── brewguide/       # 冲煮引导（准备→引导→完成）
│   └── MainScreen.kt    # Scaffold + 底部导航壳
├── util/                # 工具类
├── MainActivity.kt      # 单 Activity 入口
└── CoffeemarkApp.kt     # Application 类
```

---

## 🚀 如何运行

### 环境要求

- Android Studio Hedgehog (2023.1) 或更新版本
- JDK 17
- Android SDK 34

### 步骤

```bash
# 1. 克隆项目
git clone https://github.com/AXLLE2002/CoffeeMark.git

# 2. 用 Android Studio 打开项目根目录

# 3. 等待 Gradle 同步完成

# 4. 连接设备或启动模拟器，点击 Run
```

> ⚠️ 首次同步可能较慢（需下载 Compose / Room 等依赖）。项目已配置阿里云 Maven 镜像加速。

---

## 📋 开发阶段

| 阶段 | 内容 | 状态 |
|---|---|---|
| Phase 0 | 项目骨架搭建（Gradle + 主题 + 导航壳） | ✅ |
| Phase 1 | 数据层（Room 四表 + DAO） | ✅ |
| Phase 2 | 方案库 CRUD + 步骤编辑器 | ✅ |
| Phase 3 | 豆仓管理 | ✅ |
| Phase 4 | 冲煮记录 | ✅ |
| Phase 5 | 冲煮引导 + TTS 语音 | ✅ |
| Phase 6 | 集成联调 + 底部导航 + 数据联动 | ✅ |
| Phase 7 | 打磨（自动补全、汇总统计等） | 🚧 |

---

## 📄 License

MIT License — 自由使用、修改、分发。

---

## 👤 Author

[AXLLE2002](https://github.com/AXLLE2002)

---

*"Coffee is a language in itself." — Jackie Chan*
