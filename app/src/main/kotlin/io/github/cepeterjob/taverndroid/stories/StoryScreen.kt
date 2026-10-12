package io.github.cepeterjob.taverndroid.stories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.cepeterjob.taverndroid.engine.OpState
import io.github.cepeterjob.taverndroid.engine.TurnRole
import io.github.cepeterjob.taverndroid.engine.isTerminal
import kotlinx.coroutines.launch

@Composable
fun StoryScreen(model: StoryViewModel, onBack: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    val active = state.operation?.state?.isTerminal == false
    val retryable =
        state.operation?.state in setOf(OpState.Cancelled, OpState.Failed, OpState.Interrupted)
    LaunchedEffect(state.turns.size, state.draft.length, state.operation?.state) {
        if (list.layoutInfo.totalItemsCount >
            0
        ) {
            list.scrollToItem(list.layoutInfo.totalItemsCount - 1)
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onBack, Modifier.heightIn(min = 48.dp)) { Text("Stories") }
            Text(
                state.session?.character?.name.orEmpty(),
                Modifier.padding(12.dp),
                style = MaterialTheme.typography.titleLarge
            )
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().testTag("dialogue"),
            state = list,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.turns, key = { it.id }) { turn ->
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            if (turn.role ==
                                TurnRole.User
                            ) {
                                "You"
                            } else {
                                state.session?.character?.name ?: "Character"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(turn.text)
                    }
                }
            }
            state.operation?.let { operation ->
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp).testTag("operation"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            operation.state.name,
                            Modifier.semantics {
                                liveRegion =
                                    LiveRegionMode.Polite
                            }
                        )
                        if (state.draft.isNotEmpty()) {
                            Text("Unfinished draft", style = MaterialTheme.typography.titleMedium)
                            Text(state.draft, Modifier.testTag("draft"))
                        }
                        if (retryable) {
                            Text(
                                "This attempt did not complete. Retry uses the same saved message."
                            )
                            Button(
                                { scope.launch { model.retry(operation.snapshot.operationId) } },
                                Modifier.heightIn(min = 48.dp),
                                enabled = !state.recoveryRequired
                            ) {
                                Text("Retry")
                            }
                            if (state.draft.isNotEmpty()) {
                                TextButton(
                                    {
                                        scope.launch {
                                            model.discardDraft(operation.snapshot.operationId)
                                        }
                                    },
                                    Modifier.heightIn(min = 48.dp)
                                ) { Text("Discard draft") }
                            }
                        }
                    }
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.recoveryRequired) {
                Text(
                    "Saving failed. Recover the last saved state to continue.",
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (state.recoveryRequired || state.error?.startsWith("Could not save") == true) {
                Button({ scope.launch { model.recover() } }, Modifier.heightIn(min = 48.dp)) {
                    Text("Recover saved state")
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedTextField(
                input,
                { input = it },
                Modifier.fillMaxWidth().testTag("message"),
                label = { Text("Message") },
                maxLines = 4,
                enabled = !state.commandPending,
                supportingText = { Text("${input.length}/16000 · synthetic replies") },
                isError = input.length > 16000
            )
            if (active) {
                Button({
                    scope.launch { model.stop() }
                }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Stop")
                }
            } else {
                Button(
                    { scope.launch { if (model.send(input)) input = "" } },
                    Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    enabled = !state.commandPending && !state.recoveryRequired &&
                        input.isNotBlank() && input.length <= 16000
                ) { Text("Send") }
            }
        }
    }
}
