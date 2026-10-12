package io.github.cepeterjob.taverndroid.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.cepeterjob.taverndroid.engine.ThemeChoice

@Composable
fun TavernTheme(choice: ThemeChoice, content: @Composable () -> Unit) {
    val dark = choice == ThemeChoice.Dark ||
        (choice == ThemeChoice.System && isSystemInDarkTheme())
    val colors = if (dark) {
        darkColorScheme(
            primary = Color(0xFFE7BC86),
            surface = Color(0xFF191611),
            background = Color(0xFF191611),
            onSurface = Color(0xFFECE2D3)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF775127),
            surface = Color(0xFFFFF8EF),
            background = Color(0xFFFFF8EF),
            onSurface = Color(0xFF251F17)
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
