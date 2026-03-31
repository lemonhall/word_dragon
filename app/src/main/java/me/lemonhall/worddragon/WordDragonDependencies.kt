package me.lemonhall.worddragon

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import me.lemonhall.worddragon.audio.AndroidErrorSoundPlayer
import me.lemonhall.worddragon.audio.ErrorSoundPlayer
import me.lemonhall.worddragon.data.content.AssetIdiomCatalogDataSource
import me.lemonhall.worddragon.data.content.AssetLevelPackDataSource
import me.lemonhall.worddragon.data.content.IdiomCatalogDataSource
import me.lemonhall.worddragon.data.content.LevelPackDataSource
import me.lemonhall.worddragon.data.progress.ProgressStore
import me.lemonhall.worddragon.tts.AndroidTtsSpeaker
import me.lemonhall.worddragon.tts.TtsSpeaker

data class WordDragonDependencies(
    val idiomCatalogDataSource: IdiomCatalogDataSource,
    val levelPackDataSource: LevelPackDataSource,
    val progressStore: ProgressStore,
    val ttsSpeaker: TtsSpeaker,
    val errorSoundPlayer: ErrorSoundPlayer,
)

@Composable
fun rememberWordDragonDependencies(): WordDragonDependencies {
    val context = LocalContext.current.applicationContext
    return remember(context) { context.buildWordDragonDependencies() }
}

fun Context.buildWordDragonDependencies(): WordDragonDependencies =
    WordDragonDependencies(
        idiomCatalogDataSource = AssetIdiomCatalogDataSource(this),
        levelPackDataSource = AssetLevelPackDataSource(this),
        progressStore = ProgressStore(this),
        ttsSpeaker = AndroidTtsSpeaker(this),
        errorSoundPlayer = AndroidErrorSoundPlayer(this),
    )
