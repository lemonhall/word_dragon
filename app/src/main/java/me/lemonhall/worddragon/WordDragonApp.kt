package me.lemonhall.worddragon

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.lemonhall.worddragon.ui.navigation.WordDragonNavGraph
import me.lemonhall.worddragon.ui.theme.WordDragonTheme

@Composable
fun WordDragonApp() {
    WordDragonTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            WordDragonNavGraph()
        }
    }
}
