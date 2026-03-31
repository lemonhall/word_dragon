package me.lemonhall.worddragon.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class AndroidTtsSpeaker(
    context: Context,
) : TtsSpeaker {
    private sealed interface Status {
        data object Initializing : Status

        data object Ready : Status

        data class Unavailable(
            val reason: String,
        ) : Status
    }

    private var status: Status = Status.Initializing
    private var pendingText: String? = null
    private var textToSpeech: TextToSpeech? = null

    init {
        val appContext = context.applicationContext
        textToSpeech =
            TextToSpeech(appContext) { initStatus ->
                if (initStatus == TextToSpeech.SUCCESS) {
                    textToSpeech?.language = Locale.CHINA
                    status = Status.Ready
                    pendingText?.also(::speakInternal)
                    pendingText = null
                } else {
                    status = Status.Unavailable("系统语音初始化失败")
                    pendingText = null
                }
            }
    }

    override fun speak(text: String): SpeakResult =
        when (val currentStatus = status) {
            Status.Initializing -> {
                pendingText = text
                SpeakResult.Accepted
            }

            Status.Ready -> {
                speakInternal(text)
                SpeakResult.Accepted
            }

            is Status.Unavailable -> SpeakResult.Unavailable(currentStatus.reason)
        }

    override fun stop() {
        textToSpeech?.stop()
    }

    override fun release() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    private fun speakInternal(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "word-dragon")
    }
}
