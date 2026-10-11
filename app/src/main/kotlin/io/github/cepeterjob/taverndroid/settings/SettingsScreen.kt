package io.github.cepeterjob.taverndroid.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.cepeterjob.taverndroid.engine.ThemeChoice

@Composable
fun SettingsScreen(theme: ThemeChoice, onTheme: (ThemeChoice) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Text("Appearance", style = MaterialTheme.typography.titleLarge)
        ThemeChoice.entries.forEach { choice ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .selectable(theme == choice, role = Role.RadioButton) { onTheme(choice) }
            ) {
                RadioButton(theme == choice, onClick = null)
                Text(choice.name, Modifier.padding(12.dp))
            }
        }
        Text("Fake provider", style = MaterialTheme.typography.titleLarge)
        Text(
            "Replies say “Synthetic reply:” followed by your message. No internet, account, or API key is needed."
        )
        Text("Stories stay on this device. This build excludes app data from Android backup.")
    }
}
