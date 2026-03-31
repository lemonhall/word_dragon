package me.lemonhall.worddragon.audio

import android.content.Context
import android.media.MediaPlayer
import me.lemonhall.worddragon.R

class AndroidErrorSoundPlayer(
    context: Context,
) : ErrorSoundPlayer {
    private val appContext = context.applicationContext

    override fun playReject() {
        MediaPlayer.create(appContext, R.raw.error_reject)?.apply {
            setOnCompletionListener { player ->
                player.release()
            }
            start()
        }
    }

    override fun release() = Unit
}
