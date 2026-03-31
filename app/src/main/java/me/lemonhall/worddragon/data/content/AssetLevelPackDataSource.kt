package me.lemonhall.worddragon.data.content

import android.content.Context
import me.lemonhall.worddragon.domain.game.LevelDefinition
import me.lemonhall.worddragon.domain.game.LevelPlacement
import me.lemonhall.worddragon.domain.game.WordOrientation
import org.json.JSONObject

data class ChapterDefinition(
    val chapterId: String,
    val title: String,
    val levelIds: List<String>,
    val levelCount: Int,
    val firstLevelId: String,
)

interface LevelPackDataSource {
    fun readChapters(): List<ChapterDefinition>

    fun readLevel(levelId: String): LevelDefinition

    fun firstLevelId(): String?

    fun nextLevelId(levelId: String): String?

    fun allLevelIds(): List<String>
}

class AssetLevelPackDataSource(
    private val context: Context,
) : LevelPackDataSource {
    private val chapters by lazy(LazyThreadSafetyMode.NONE) { loadChapters() }
    private val levelCache = mutableMapOf<String, LevelDefinition>()

    override fun readChapters(): List<ChapterDefinition> = chapters

    override fun readLevel(levelId: String): LevelDefinition = levelCache.getOrPut(levelId) { loadLevel(levelId) }

    override fun firstLevelId(): String? = allLevelIds().firstOrNull()

    override fun nextLevelId(levelId: String): String? {
        val levelIds = allLevelIds()
        val index = levelIds.indexOf(levelId)
        return if (index >= 0) levelIds.getOrNull(index + 1) else null
    }

    override fun allLevelIds(): List<String> = chapters.flatMap { it.levelIds }

    private fun loadChapters(): List<ChapterDefinition> {
        val payload = context.assets.open(CHAPTER_ASSET_PATH).bufferedReader().use { it.readText() }
        val chapterArray = JSONObject(payload).getJSONArray("chapters")
        return buildList(chapterArray.length()) {
            repeat(chapterArray.length()) { index ->
                val chapter = chapterArray.getJSONObject(index)
                add(
                    ChapterDefinition(
                        chapterId = chapter.getString("chapter_id"),
                        title = chapter.getString("title"),
                        levelIds =
                            buildList {
                                val ids = chapter.getJSONArray("level_ids")
                                repeat(ids.length()) { idIndex ->
                                    add(ids.getString(idIndex))
                                }
                            },
                        levelCount = chapter.getInt("level_count"),
                        firstLevelId = chapter.getString("first_level_id"),
                    ),
                )
            }
        }
    }

    private fun loadLevel(levelId: String): LevelDefinition {
        val payload =
            context.assets.open("content/levels/$levelId.json").bufferedReader().use { it.readText() }
        val json = JSONObject(payload)
        return LevelDefinition(
            levelId = json.getString("level_id"),
            chapterId = json.getString("chapter_id"),
            idiomIds =
                buildList {
                    val idiomIds = json.getJSONArray("idiom_ids")
                    repeat(idiomIds.length()) { index ->
                        add(idiomIds.getString(index))
                    }
                },
            boardWidth = json.getInt("board_width"),
            boardHeight = json.getInt("board_height"),
            candidateChars =
                buildList {
                    val chars = json.getJSONArray("candidate_chars")
                    repeat(chars.length()) { index ->
                        add(chars.getString(index).first())
                    }
                },
            placements =
                buildList {
                    val placements = json.getJSONArray("placements")
                    repeat(placements.length()) { index ->
                        val placement = placements.getJSONObject(index)
                        add(
                            LevelPlacement(
                                idiomId = placement.getString("idiom_id"),
                                orientation =
                                    when (placement.getString("orientation")) {
                                        "across" -> WordOrientation.ACROSS
                                        "down" -> WordOrientation.DOWN
                                        else -> error("Unknown orientation: ${placement.getString("orientation")}")
                                    },
                                row = placement.getInt("row"),
                                col = placement.getInt("col"),
                            ),
                        )
                    }
                },
            layoutProfile = json.optString("layout_profile"),
        )
    }

    private companion object {
        const val CHAPTER_ASSET_PATH = "content/chapters.json"
    }
}
