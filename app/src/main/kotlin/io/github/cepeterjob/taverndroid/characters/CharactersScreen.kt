package io.github.cepeterjob.taverndroid.characters

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.cepeterjob.taverndroid.engine.CharacterInput
import kotlinx.coroutines.launch

@Composable
fun CharactersScreen(model: CharactersViewModel, onStart: (String) -> Unit) {
    val characters by model.characters.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var greeting by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LazyColumn(
        Modifier.padding(20.dp).testTag("characters-list"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Your characters", style = MaterialTheme.typography.headlineMedium) }
        item {
            Text(
                "Create a character, then start a story. Each story keeps its own character snapshot."
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    name,
                    {
                        name = it
                    },
                    Modifier.fillMaxWidth().testTag("character-name"),
                    label = {
                        Text("Name")
                    },
                    singleLine = true,
                    supportingText = { Text("${name.length}/80") }
                )
                OutlinedTextField(
                    description,
                    { description = it },
                    Modifier.fillMaxWidth(),
                    label = { Text("Description") },
                    minLines = 2,
                    maxLines = 4,
                    supportingText = { Text("${description.length}/16000") }
                )
                OutlinedTextField(
                    greeting,
                    {
                        greeting = it
                    },
                    Modifier.fillMaxWidth().testTag("character-greeting"),
                    label = { Text("Greeting (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    supportingText = { Text("${greeting.length}/8000") }
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = {
                    saving = true
                    scope.launch {
                        try {
                            if (model.create(CharacterInput(name, description, greeting))) {
                                name = ""
                                description = ""
                                greeting = ""
                            }
                        } finally {
                            saving = false
                        }
                    }
                }, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Save character")
                }
            }
        }
        items(characters, key = { it.id }) { character ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(character.content.name, style = MaterialTheme.typography.titleLarge)
                    if (character.content.description.isNotEmpty()) {
                        Text(
                            character.content.description
                        )
                    }
                    Button(
                        { onStart(character.id) },
                        Modifier.heightIn(min = 48.dp)
                            .testTag("start-${character.id}")
                    ) {
                        Text("Start story")
                    }
                }
            }
        }
        if (characters.isEmpty()) item { Text("No saved characters yet.") }
    }
}
