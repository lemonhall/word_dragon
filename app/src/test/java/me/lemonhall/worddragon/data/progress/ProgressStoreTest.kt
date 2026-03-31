package me.lemonhall.worddragon.data.progress

import me.lemonhall.worddragon.domain.game.HintUsage
import me.lemonhall.worddragon.domain.game.LevelProgressSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressStoreTest {
    @Test
    fun savesAndRestoresInProgressBoardSnapshot() {
        val store = ProgressStore(RuntimeEnvironment.getApplication())
        val snapshot =
            LevelProgressSnapshot(
                levelId = "level-0007",
                selectedIdiomId = "idiom-1",
                cellInputs = mapOf("0,0" to "高", "0,1" to "山"),
                hintUsage = HintUsage(revealedChars = 1, revealedIdioms = 0),
            )

        store.saveSnapshot(snapshot)

        val progress = store.readProgress()
        assertEquals("level-0007", progress.currentLevelId)
        assertEquals(snapshot, progress.snapshots["level-0007"])
        assertTrue(progress.autoSpeakEnabled)
    }

    @Test
    fun completingLevelUnlocksNextAndClearsSnapshot() {
        val store = ProgressStore(RuntimeEnvironment.getApplication())
        store.saveSnapshot(
            LevelProgressSnapshot(
                levelId = "level-0001",
                selectedIdiomId = "idiom-1",
                cellInputs = mapOf("0,0" to "高"),
                hintUsage = HintUsage(),
            ),
        )

        store.markLevelCompleted(levelId = "level-0001", nextLevelId = "level-0002")

        val progress = store.readProgress()
        assertTrue(progress.completedLevelIds.contains("level-0001"))
        assertEquals("level-0002", progress.currentLevelId)
        assertNull(progress.snapshots["level-0001"])

        store.setAutoSpeakEnabled(false)
        assertFalse(store.readProgress().autoSpeakEnabled)
    }
}
