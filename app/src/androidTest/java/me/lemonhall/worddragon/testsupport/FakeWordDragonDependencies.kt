package me.lemonhall.worddragon.testsupport

import android.content.Context
import me.lemonhall.worddragon.WordDragonDependencies
import me.lemonhall.worddragon.audio.ErrorSoundPlayer
import me.lemonhall.worddragon.data.content.ChapterDefinition
import me.lemonhall.worddragon.data.content.IdiomCatalogDataSource
import me.lemonhall.worddragon.data.content.LevelPackDataSource
import me.lemonhall.worddragon.data.progress.ProgressStore
import me.lemonhall.worddragon.domain.game.IdiomDefinition
import me.lemonhall.worddragon.domain.game.LevelDefinition
import me.lemonhall.worddragon.domain.game.LevelPlacement
import me.lemonhall.worddragon.domain.game.WordOrientation
import me.lemonhall.worddragon.tts.SpeakResult
import me.lemonhall.worddragon.tts.TtsSpeaker

class FakeTtsSpeaker : TtsSpeaker {
    val spokenTexts = mutableListOf<String>()

    override fun speak(text: String): SpeakResult {
        spokenTexts += text
        return SpeakResult.Accepted
    }

    override fun stop() = Unit

    override fun release() = Unit
}

class FakeErrorSoundPlayer : ErrorSoundPlayer {
    var rejectCount: Int = 0

    override fun playReject() {
        rejectCount += 1
    }

    override fun release() = Unit
}

class FakeIdiomCatalogDataSource(
    private val idioms: Map<String, IdiomDefinition>,
) : IdiomCatalogDataSource {
    override fun getIdiom(id: String): IdiomDefinition = requireNotNull(idioms[id])

    override fun getIdioms(ids: List<String>): List<IdiomDefinition> = ids.map(::getIdiom)
}

class FakeLevelPackDataSource(
    private val chapters: List<ChapterDefinition>,
    private val levels: Map<String, LevelDefinition>,
) : LevelPackDataSource {
    private val orderedLevelIds = chapters.flatMap { it.levelIds }

    override fun readChapters(): List<ChapterDefinition> = chapters

    override fun readLevel(levelId: String): LevelDefinition = requireNotNull(levels[levelId])

    override fun firstLevelId(): String? = orderedLevelIds.firstOrNull()

    override fun nextLevelId(levelId: String): String? =
        orderedLevelIds.indexOf(levelId).takeIf { it >= 0 }?.let { index ->
            orderedLevelIds.getOrNull(index + 1)
        }

    override fun allLevelIds(): List<String> = orderedLevelIds
}

fun buildFakeDependencies(
    context: Context,
    prefsName: String,
    speaker: FakeTtsSpeaker = FakeTtsSpeaker(),
    errorSoundPlayer: FakeErrorSoundPlayer = FakeErrorSoundPlayer(),
): WordDragonDependencies {
    val idioms =
        listOf(
            IdiomDefinition(
                id = "idiom-1",
                text = "高山流水",
                shortExplanation = "比喻知音难遇。",
                ttsText = "高山流水。比喻知音难遇。",
            ),
            IdiomDefinition(
                id = "idiom-2",
                text = "水到渠成",
                shortExplanation = "条件成熟后自然成功。",
                ttsText = "水到渠成。条件成熟后自然成功。",
            ),
            IdiomDefinition(
                id = "idiom-3",
                text = "胸有成竹",
                shortExplanation = "做事前已想好办法。",
                ttsText = "胸有成竹。做事前已想好办法。",
            ),
            IdiomDefinition(
                id = "idiom-4",
                text = "竹报平安",
                shortExplanation = "比喻平安家信。",
                ttsText = "竹报平安。比喻平安家信。",
            ),
        ).associateBy { it.id }
    val levels =
        listOf(
            LevelDefinition(
                levelId = "level-0001",
                chapterId = "chapter-001",
                idiomIds = listOf("idiom-1"),
                boardWidth = 4,
                boardHeight = 1,
                candidateChars = listOf('高', '山', '流', '水'),
                placements =
                    listOf(
                        LevelPlacement(
                            idiomId = "idiom-1",
                            orientation = WordOrientation.ACROSS,
                            row = 0,
                            col = 0,
                        ),
                    ),
            ),
            LevelDefinition(
                levelId = "level-0002",
                chapterId = "chapter-001",
                idiomIds = listOf("idiom-3"),
                boardWidth = 4,
                boardHeight = 1,
                candidateChars = listOf('胸', '有', '成', '竹'),
                placements =
                    listOf(
                        LevelPlacement(
                            idiomId = "idiom-3",
                            orientation = WordOrientation.ACROSS,
                            row = 0,
                            col = 0,
                        ),
                    ),
            ),
        ).associateBy { it.levelId }
    val chapters =
        listOf(
            ChapterDefinition(
                chapterId = "chapter-001",
                title = "第一章 常用成语",
                levelIds = listOf("level-0001", "level-0002"),
                levelCount = 2,
                firstLevelId = "level-0001",
            ),
        )
    return WordDragonDependencies(
        idiomCatalogDataSource = FakeIdiomCatalogDataSource(idioms),
        levelPackDataSource = FakeLevelPackDataSource(chapters = chapters, levels = levels),
        progressStore = ProgressStore(context, prefsName = prefsName),
        ttsSpeaker = speaker,
        errorSoundPlayer = errorSoundPlayer,
    )
}
