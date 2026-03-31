package me.lemonhall.worddragon.domain.tts

import me.lemonhall.worddragon.domain.game.IdiomDefinition

object GameSpeechFormatter {
    fun format(idiom: IdiomDefinition): String {
        val explanation = idiom.shortExplanation.trim().trimEnd('。', '！', '？')
        return if (explanation.isBlank()) {
            "请填写一个四字成语。"
        } else {
            "$explanation。"
        }
    }
}
