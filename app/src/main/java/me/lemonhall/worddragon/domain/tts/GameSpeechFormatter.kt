package me.lemonhall.worddragon.domain.tts

import me.lemonhall.worddragon.domain.game.IdiomDefinition

object GameSpeechFormatter {
    fun format(idiom: IdiomDefinition): String {
        if (idiom.ttsText.isNotBlank()) {
            return idiom.ttsText
        }
        val explanation = idiom.shortExplanation.trim().trimEnd('。', '！', '？')
        return if (explanation.isBlank()) {
            "${idiom.text}。"
        } else {
            "${idiom.text}。$explanation。"
        }
    }
}
