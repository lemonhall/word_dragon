# v1 Bootstrap Foundation

## Goal

建立 `word_dragon` 的 Android 项目骨架、首页导航、章节页骨架和老年友好视觉基线，为后续内容链路和玩法实现提供稳定入口。

## PRD Trace

- `REQ-0001-001`
- `REQ-0001-007`
- `REQ-0001-008`

## Scope

- 创建 Android 项目、`app` 模块、Compose 基础依赖和导航壳层
- 建立大字号 typography、触控尺寸 token 和基础配色
- 完成首页、章节页、设置页占位但可导航的真实 UI
- 建立首页入口自动化验证

## Out Of Scope

- 不实现真实关卡数据读取
- 不实现关卡页填词逻辑
- 不实现 TTS 业务联动
- 不生成任何正式内容包

## Acceptance

- `.\gradlew.bat :app:assembleDebug` 退出码为 `0`
- `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.ui.theme.WordDragonTypographyTest"` 退出码为 `0`
- `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.home.HomeLaunchTest` 退出码为 `0`
- `HomeLaunchTest` 必须验证首页上真实可点击的 `继续游戏`、`章节选关`、`设置` 三个入口；只渲染占位文字但没有点击行为不算通过

## Files

- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/me/lemonhall/worddragon/MainActivity.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/WordDragonApp.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/ui/navigation/WordDragonNavGraph.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/ui/home/HomeScreen.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/ui/chapters/ChapterListScreen.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/ui/settings/SettingsScreen.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/ui/theme/Color.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/ui/theme/Theme.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/ui/theme/Type.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/ui/theme/DimensionTokens.kt`
- Test: `app/src/test/java/me/lemonhall/worddragon/ui/theme/WordDragonTypographyTest.kt`
- Test: `app/src/androidTest/java/me/lemonhall/worddragon/ui/home/HomeLaunchTest.kt`

## Steps

1. 写 `WordDragonTypographyTest` 和 `HomeLaunchTest` 两个失败测试，先把大字号 token、首页入口文案和点击导航约束写成断言。
2. 运行 `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.ui.theme.WordDragonTypographyTest"`，预期失败原因是项目与测试类尚不存在。
3. 运行 `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.home.HomeLaunchTest`，预期失败原因是 `app` 模块和仪器测试环境尚未建立。
4. 创建 Android 项目骨架、Compose 主题、导航壳层和首页/章节/设置真实入口，实现最小可运行 UI。
5. 再次运行 `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.ui.theme.WordDragonTypographyTest"`，预期通过并验证字号与触控 token 下限。
6. 再次运行 `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.home.HomeLaunchTest`，预期通过并验证首页真实导航。
7. 进行必要重构，确保主题 token 与导航定义分离，仍保持上述测试通过。
8. 运行一次手工辅助验收：`.\gradlew.bat :app:installDebug` 后在设备或模拟器中打开 App，确认首页不出现英文占位或空白页面。

## Risks

- 参考工程混合了 ViewBinding 与 Compose；`word_dragon` 在 `v1` 中明确以 Compose 为主，避免不必要的双栈复杂度。
- Android 仪器测试环境准备时间可能较长，需要尽早建立稳定的 emulator 运行路径。
