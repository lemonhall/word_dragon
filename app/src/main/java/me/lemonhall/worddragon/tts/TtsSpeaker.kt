package me.lemonhall.worddragon.tts

sealed interface SpeakResult {
    data object Accepted : SpeakResult

    data class Unavailable(
        val reason: String,
    ) : SpeakResult
}

interface TtsSpeaker {
    fun speak(text: String): SpeakResult

    fun stop()

    fun release()
}
