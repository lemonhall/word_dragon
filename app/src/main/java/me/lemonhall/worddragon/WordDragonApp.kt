package me.lemonhall.worddragon

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import me.lemonhall.worddragon.ui.navigation.WordDragonNavGraph
import me.lemonhall.worddragon.ui.theme.WordDragonTheme

@Composable
fun WordDragonApp(
    dependencies: WordDragonDependencies = rememberWordDragonDependencies(),
) {
    DisposableEffect(dependencies.ttsSpeaker) {
        onDispose {
            dependencies.ttsSpeaker.release()
        }
    }
    WordDragonTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            WordDragonNavGraph(dependencies = dependencies)
        }
    }
}
