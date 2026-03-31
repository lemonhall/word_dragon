package me.lemonhall.worddragon.data.progress

import android.content.Context
import me.lemonhall.worddragon.domain.game.HintUsage
import me.lemonhall.worddragon.domain.game.LevelProgressSnapshot
import org.json.JSONArray
import org.json.JSONObject

data class StoredProgress(
    val currentLevelId: String? = null,
    val completedLevelIds: Set<String> = emptySet(),
    val snapshots: Map<String, LevelProgressSnapshot> = emptyMap(),
    val autoSpeakEnabled: Boolean = true,
)

class ProgressStore(
    context: Context,
    prefsName: String = DEFAULT_PREFS_NAME,
) {
    private val prefs = context.applicationContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun readProgress(): StoredProgress {
        val payload = prefs.getString(KEY_PROGRESS_JSON, null) ?: return StoredProgress()
        if (payload.isBlank()) {
            return StoredProgress()
        }
        return parseProgress(JSONObject(payload))
    }

    fun saveSnapshot(snapshot: LevelProgressSnapshot) {
        val current = readProgress()
        val nextSnapshots = current.snapshots.toMutableMap()
        nextSnapshots[snapshot.levelId] = snapshot
        writeProgress(
            current.copy(
                currentLevelId = snapshot.levelId,
                snapshots = nextSnapshots,
            ),
        )
    }

    fun markLevelCompleted(
        levelId: String,
        nextLevelId: String?,
    ) {
        val current = readProgress()
        val nextSnapshots = current.snapshots.toMutableMap()
        nextSnapshots.remove(levelId)
        writeProgress(
            current.copy(
                currentLevelId = nextLevelId,
                completedLevelIds = current.completedLevelIds + levelId,
                snapshots = nextSnapshots,
            ),
        )
    }

    fun setAutoSpeakEnabled(enabled: Boolean) {
        val current = readProgress()
        writeProgress(current.copy(autoSpeakEnabled = enabled))
    }

    fun clearAll() {
        prefs.edit().remove(KEY_PROGRESS_JSON).apply()
    }

    private fun writeProgress(progress: StoredProgress) {
        prefs.edit().putString(KEY_PROGRESS_JSON, progress.toJson().toString()).apply()
    }

    private fun parseProgress(json: JSONObject): StoredProgress {
        val snapshotObject = json.optJSONObject("snapshots") ?: JSONObject()
        val snapshots =
            snapshotObject.keys().asSequence().mapNotNull { levelId ->
                val snapshotJson = snapshotObject.optJSONObject(levelId) ?: return@mapNotNull null
                levelId to snapshotJson.toSnapshot(levelId)
            }.toMap()
        val completedLevelIds =
            buildSet {
                val array = json.optJSONArray("completed_level_ids") ?: JSONArray()
                repeat(array.length()) { index ->
                    add(array.optString(index))
                }
            }.filter { it.isNotBlank() }.toSet()
        return StoredProgress(
            currentLevelId = json.optString("current_level_id").takeIf { it.isNotBlank() },
            completedLevelIds = completedLevelIds,
            snapshots = snapshots,
            autoSpeakEnabled = json.optBoolean("auto_speak_enabled", true),
        )
    }

    private fun StoredProgress.toJson(): JSONObject =
        JSONObject().apply {
            put("current_level_id", currentLevelId)
            put(
                "completed_level_ids",
                JSONArray().apply {
                    completedLevelIds.sorted().forEach(::put)
                },
            )
            put("auto_speak_enabled", autoSpeakEnabled)
            put(
                "snapshots",
                JSONObject().apply {
                    snapshots.forEach { (levelId, snapshot) ->
                        put(levelId, snapshot.toJson())
                    }
                },
            )
        }

    private fun LevelProgressSnapshot.toJson(): JSONObject =
        JSONObject().apply {
            put("selected_idiom_id", selectedIdiomId)
            put("focused_cell_key", focusedCellKey)
            put("is_completed", isCompleted)
            put(
                "cell_inputs",
                JSONObject().apply {
                    cellInputs.toSortedMap().forEach { (coordinate, value) ->
                        put(coordinate, value)
                    }
                },
            )
            put(
                "hint_usage",
                JSONObject().apply {
                    put("revealed_chars", hintUsage.revealedChars)
                    put("revealed_idioms", hintUsage.revealedIdioms)
                },
            )
        }

    private fun JSONObject.toSnapshot(levelId: String): LevelProgressSnapshot =
        LevelProgressSnapshot(
            levelId = levelId,
            selectedIdiomId = optString("selected_idiom_id"),
            focusedCellKey = optString("focused_cell_key").takeIf { it.isNotBlank() },
            cellInputs =
                optJSONObject("cell_inputs")
                    ?.keys()
                    ?.asSequence()
                    ?.associateWith { key -> optJSONObject("cell_inputs")!!.optString(key) }
                    ?.filterValues { it.isNotBlank() }
                    ?: emptyMap(),
            hintUsage =
                optJSONObject("hint_usage")
                    ?.let { hintJson ->
                        HintUsage(
                            revealedChars = hintJson.optInt("revealed_chars", 0),
                            revealedIdioms = hintJson.optInt("revealed_idioms", 0),
                        )
                    } ?: HintUsage(),
            isCompleted = optBoolean("is_completed", false),
        )

    private companion object {
        const val DEFAULT_PREFS_NAME = "word_dragon_progress"
        const val KEY_PROGRESS_JSON = "progress_json"
    }
}
