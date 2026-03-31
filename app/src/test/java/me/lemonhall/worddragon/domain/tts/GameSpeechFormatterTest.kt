package me.lemonhall.worddragon.domain.tts

import me.lemonhall.worddragon.domain.game.IdiomDefinition
import org.junit.Assert.assertEquals
import org.junit.Test

class GameSpeechFormatterTest {
    @Test
    fun formatsExplanationOnlySoAutoSpeechDoesNotLeakAnswer() {
        val speech =
            GameSpeechFormatter.format(
                IdiomDefinition(
                    id = "idiom-1",
                    text = "高山流水",
                    shortExplanation = "比喻知音难遇。",
                    ttsText = "高山流水。比喻知音难遇。",
                ),
            )

        assertEquals("比喻知音难遇。", speech)
    }

    @Test
    fun fallsBackToGenericHintWhenExplanationMissing() {
        val speech =
            GameSpeechFormatter.format(
                IdiomDefinition(
                    id = "idiom-2",
                    text = "胸有成竹",
                    shortExplanation = "",
                    ttsText = "",
                ),
            )

        assertEquals("请填写一个四字成语。", speech)
    }
}
