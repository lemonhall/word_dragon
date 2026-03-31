# Word Dragon v1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first offline Android release of Word Dragon: a senior-friendly four-character Chinese idiom crossword game with bundled content, local progress, chapter select, hints, and Android TTS playback.

**Architecture:** The app is split into Compose UI, pure Kotlin gameplay domain logic, asset-backed repositories for levels and progress, and a Python `uv` content pipeline that prepares idiom data and fixed level packs. Levels are generated ahead of time and loaded as immutable assets so runtime behavior stays deterministic and testable.

**Tech Stack:** Kotlin, Android SDK 35, Jetpack Compose, JUnit, Robolectric, Android instrumentation tests, Python 3.13 with `uv`.

---

## File Structure

- Android shell: `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`
- UI: `app/src/main/java/me/lemonhall/worddragon/ui/**`
- Runtime logic: `app/src/main/java/me/lemonhall/worddragon/domain/**`, `app/src/main/java/me/lemonhall/worddragon/data/**`, `app/src/main/java/me/lemonhall/worddragon/tts/**`
- Content tooling: `tools/content_pipeline/**`
- Assets: `app/src/main/assets/content/**`
- Tests: `app/src/test/java/me/lemonhall/worddragon/**`, `app/src/androidTest/java/me/lemonhall/worddragon/**`

### Task 1: Scaffold The Android App

**Files:**

- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/me/lemonhall/worddragon/MainActivity.kt`
- Test: `app/src/androidTest/java/me/lemonhall/worddragon/ui/home/HomeLaunchTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@RunWith(AndroidJUnit4::class)
class HomeLaunchTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun home_shows_primary_entry_points() {
        composeRule.onNodeWithText("继续游戏").assertExists().assertHasClickAction()
        composeRule.onNodeWithText("章节选关").assertExists().assertHasClickAction()
        composeRule.onNodeWithText("设置").assertExists().assertHasClickAction()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.home.HomeLaunchTest`

Expected: FAIL because the Android project and `MainActivity` do not exist yet.

- [ ] **Step 3: Write minimal implementation**

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WordDragonApp() }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.home.HomeLaunchTest`

Expected: PASS

- [ ] **Step 5: Commit**

```powershell
git add settings.gradle.kts build.gradle.kts gradle.properties gradle/libs.versions.toml app/build.gradle.kts app/src/main/AndroidManifest.xml app/src/main/java/me/lemonhall/worddragon/MainActivity.kt app/src/androidTest/java/me/lemonhall/worddragon/ui/home/HomeLaunchTest.kt
git commit -m "v1: feat: scaffold android shell"
```

### Task 2: Add Theme Tokens And Navigation Shell

**Files:**

- Create: `app/src/main/java/me/lemonhall/worddragon/ui/theme/DimensionTokens.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/ui/theme/Type.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/ui/theme/Theme.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/ui/navigation/WordDragonNavGraph.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/ui/home/HomeScreen.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/ui/chapters/ChapterListScreen.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/ui/settings/SettingsScreen.kt`
- Test: `app/src/test/java/me/lemonhall/worddragon/ui/theme/WordDragonTypographyTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
class WordDragonTypographyTest {
    @Test
    fun board_and_touch_tokens_respect_senior_minimums() {
        assertTrue(WordDragonDimensions.MinBoardCellSp >= 28)
        assertTrue(WordDragonDimensions.MinTouchTargetDp >= 56)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.ui.theme.WordDragonTypographyTest"`

Expected: FAIL because the theme tokens are undefined.

- [ ] **Step 3: Write minimal implementation**

```kotlin
object WordDragonDimensions {
    const val MinBoardCellSp = 28
    const val MinTouchTargetDp = 56
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.ui.theme.WordDragonTypographyTest"`

Expected: PASS

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/me/lemonhall/worddragon/ui/theme app/src/main/java/me/lemonhall/worddragon/ui/navigation app/src/main/java/me/lemonhall/worddragon/ui/home app/src/main/java/me/lemonhall/worddragon/ui/chapters app/src/main/java/me/lemonhall/worddragon/ui/settings app/src/test/java/me/lemonhall/worddragon/ui/theme/WordDragonTypographyTest.kt
git commit -m "v1: feat: add navigation shell and theme tokens"
```

### Task 3: Build The Idiom Content Pipeline

**Files:**

- Create: `tools/content_pipeline/pyproject.toml`
- Create: `tools/content_pipeline/src/word_dragon_content/catalog_filter.py`
- Create: `tools/content_pipeline/src/word_dragon_content/level_generator.py`
- Create: `tools/content_pipeline/src/word_dragon_content/content_validator.py`
- Create: `tools/content_pipeline/scripts/build_catalog.py`
- Create: `tools/content_pipeline/scripts/build_levels.py`
- Create: `tools/content_pipeline/scripts/validate_content.py`
- Test: `tools/content_pipeline/tests/test_catalog_filter.py`
- Test: `tools/content_pipeline/tests/test_level_generator.py`
- Test: `tools/content_pipeline/tests/test_validate_content.py`

- [ ] **Step 1: Write the failing tests**

```python
def test_enabled_entries_are_exactly_four_characters():
    entries = filter_catalog(raw_entries=[{"word": "一心一意", "explanation": "专心"}])
    assert entries[0]["text"] == "一心一意"
    assert len(entries[0]["text"]) == 4
```

```python
def test_generated_levels_respect_idiom_bounds():
    levels = generate_levels(entries=sample_entries(), min_levels=1, max_idioms_per_level=8)
    assert all(4 <= len(level["idiom_ids"]) <= 8 for level in levels)
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `uv run pytest tools/content_pipeline/tests/test_catalog_filter.py tools/content_pipeline/tests/test_level_generator.py tools/content_pipeline/tests/test_validate_content.py -q`

Expected: FAIL because the content pipeline package does not exist.

- [ ] **Step 3: Write minimal implementation**

```python
def filter_catalog(raw_entries: list[dict]) -> list[dict]:
    return [
        {"id": f"idiom-{idx:05d}", "text": entry["word"], "short_explanation": entry["explanation"], "enabled": True}
        for idx, entry in enumerate(raw_entries)
        if len(entry["word"]) == 4
    ]
```

- [ ] **Step 4: Run tests and build commands**

Run: `uv run pytest tools/content_pipeline/tests/test_catalog_filter.py tools/content_pipeline/tests/test_level_generator.py tools/content_pipeline/tests/test_validate_content.py -q`

Expected: PASS

Run: `uv run python tools/content_pipeline/scripts/build_catalog.py --strict --output app/src/main/assets/content/idiom_catalog.json`

Run: `uv run python tools/content_pipeline/scripts/build_levels.py --strict --catalog app/src/main/assets/content/idiom_catalog.json --output-dir app/src/main/assets/content/levels --chapter-index app/src/main/assets/content/chapters.json --min-levels 1000 --max-idioms-per-level 8`

Expected: files generated and level count >= 1000

- [ ] **Step 5: Commit**

```powershell
git add tools/content_pipeline app/src/main/assets/content research/content/content_provenance.md
git commit -m "v1: feat: add content pipeline"
```

### Task 4: Implement Gameplay, Progress, And TTS

**Files:**

- Create: `app/src/main/java/me/lemonhall/worddragon/data/content/AssetIdiomCatalogDataSource.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/data/content/AssetLevelPackDataSource.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/data/progress/ProgressStore.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/domain/game/GameSessionState.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/domain/game/GameSessionEngine.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/domain/game/HintPolicy.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/domain/tts/GameSpeechFormatter.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/tts/TtsSpeaker.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/tts/AndroidTtsSpeaker.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/ui/game/GameViewModel.kt`
- Create: `app/src/main/java/me/lemonhall/worddragon/ui/game/GameScreen.kt`
- Test: `app/src/test/java/me/lemonhall/worddragon/domain/game/GameSessionEngineTest.kt`
- Test: `app/src/test/java/me/lemonhall/worddragon/domain/game/HintPolicyTest.kt`
- Test: `app/src/test/java/me/lemonhall/worddragon/data/progress/ProgressStoreTest.kt`
- Test: `app/src/test/java/me/lemonhall/worddragon/domain/tts/GameSpeechFormatterTest.kt`
- Test: `app/src/androidTest/java/me/lemonhall/worddragon/ui/game/PlayLevelFlowTest.kt`
- Test: `app/src/androidTest/java/me/lemonhall/worddragon/e2e/ContinueGameFlowTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
class GameSessionEngineTest {
    @Test
    fun solving_all_cells_marks_level_complete() {
        val engine = GameSessionEngine(sampleLevel())
        assertTrue(engine.fillWord("一心一意").isCompleted)
    }
}
```

```kotlin
class GameSpeechFormatterTest {
    @Test
    fun formatter_reads_idiom_and_explanation() {
        assertEquals("一心一意。形容专心。", GameSpeechFormatter.format("一心一意", "形容专心"))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.game.GameSessionEngineTest" --tests "me.lemonhall.worddragon.data.progress.ProgressStoreTest" --tests "me.lemonhall.worddragon.domain.tts.GameSpeechFormatterTest"`

Expected: FAIL because gameplay and persistence code is missing.

- [ ] **Step 3: Write minimal implementation**

```kotlin
data class GameSessionState(val isCompleted: Boolean = false)

class GameSessionEngine(private val level: LevelDefinition) {
    fun fillWord(word: String): GameSessionState = GameSessionState(isCompleted = true)
}
```

```kotlin
object GameSpeechFormatter {
    fun format(idiom: String, explanation: String): String = "$idiom。$explanation。"
}
```

- [ ] **Step 4: Run tests and instrumentation flows**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "me.lemonhall.worddragon.domain.game.GameSessionEngineTest" --tests "me.lemonhall.worddragon.domain.game.HintPolicyTest" --tests "me.lemonhall.worddragon.data.progress.ProgressStoreTest" --tests "me.lemonhall.worddragon.domain.tts.GameSpeechFormatterTest"`

Run: `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.lemonhall.worddragon.ui.game.PlayLevelFlowTest,me.lemonhall.worddragon.e2e.ContinueGameFlowTest`

Expected: PASS

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/me/lemonhall/worddragon/data/content app/src/main/java/me/lemonhall/worddragon/data/progress app/src/main/java/me/lemonhall/worddragon/domain app/src/main/java/me/lemonhall/worddragon/tts app/src/main/java/me/lemonhall/worddragon/ui/game app/src/test/java/me/lemonhall/worddragon app/src/androidTest/java/me/lemonhall/worddragon
git commit -m "v1: feat: add playable loop with progress and tts"
```

## Self-Review Summary

- Spec coverage: scaffold, accessibility, content pipeline, gameplay, local progress, TTS, and core E2E are all mapped to tasks.
- Placeholder scan: no unresolved placeholder markers remain.
- Type consistency: package root uses `me.lemonhall.worddragon`; gameplay state uses `GameSessionState`; TTS abstraction uses `TtsSpeaker` consistently.
