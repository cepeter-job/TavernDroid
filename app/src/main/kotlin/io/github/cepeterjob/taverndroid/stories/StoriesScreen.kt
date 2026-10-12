package io.github.cepeterjob.taverndroid.stories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.cepeterjob.taverndroid.engine.SessionSnapshot

@Composable
fun StoriesScreen(
    sessions: List<SessionSnapshot>,
    onOpen: (String) -> Unit,
    onCharacters: () -> Unit
) {
    LazyColumn(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Your stories", style = MaterialTheme.typography.headlineMedium) }
        item { Text("Saved on this device. Replies in this build are synthetic and work offline.") }
        item { Button(onCharacters, Modifier.heightIn(min = 48.dp)) { Text("Create a character") } }
        if (sessions.isEmpty()) item { Text("Start your first story from Characters.") }
        items(sessions, key = { it.identity.sessionId }) { session ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(session.character.name, style = MaterialTheme.typography.titleLarge)
                    Button(
                        { onOpen(session.identity.sessionId) },
                        Modifier.heightIn(min = 48.dp)
                            .testTag("open-${session.identity.sessionId}")
                    ) {
                        Text("Open story")
                    }
                }
            }
        }
    }
}
