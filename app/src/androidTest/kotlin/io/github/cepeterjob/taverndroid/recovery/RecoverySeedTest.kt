package io.github.cepeterjob.taverndroid.recovery

import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import io.github.cepeterjob.taverndroid.MainActivity
import io.github.cepeterjob.taverndroid.TavernDroidApplication
import io.github.cepeterjob.taverndroid.TestApplication
import io.github.cepeterjob.taverndroid.engine.Admission
import io.github.cepeterjob.taverndroid.engine.CharacterInput
import io.github.cepeterjob.taverndroid.engine.OpState
import java.io.File
import java.util.UUID
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

class RecoverySeedTest {
    @Test fun checkpointReadyForHostKill() = runBlocking<Unit> {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue(arguments.getString("phase") == "seed")
        val runId = UUID.fromString(arguments.getString("recoveryRunId")).toString()
        val mode = arguments.getString("mode")!!
        require(mode in setOf("queued", "streaming", "completed"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val container = (context.applicationContext as TavernDroidApplication).container
        ActivityScenario.launch(MainActivity::class.java)
        container.coordinator.initialize()
        container.coordinator.onForeground()
        TestApplication.invocations.set(0)
        TestApplication.holdStream = true
        TestApplication.checkpointStream = true
        val character = container.repository.createCharacter(CharacterInput("Recovery $runId"))
        val session = container.repository.startSession(character.id)
        val admission = if (mode == "streaming") {
            container.coordinator.send(session.identity.sessionId, 0, runId, "Synthetic recovery")
        } else {
            container.generations.admitSend(
                session.identity.sessionId,
                0,
                runId,
                "Synthetic recovery"
            )
        } as Admission.Admitted
        if (mode == "completed") {
            container.generations.complete(admission.snapshot, "Synthetic committed reply")
        }
        val operation = withTimeout(15000) {
            container.generations.observeLatestOperation(session.identity.sessionId).first {
                it != null && when (mode) {
                    "streaming" -> it.state == OpState.Streaming && it.draft.endsWith(" checkpoint")
                    "completed" -> it.state == OpState.Completed
                    else -> it.state == OpState.Queued
                }
            }!!
        }
        assertEquals(if (mode == "streaming") 1 else 0, TestApplication.invocations.get())
        // This marker is published only after observing Room's durable checkpoint.
        val marker = JSONObject().put("runId", runId).put("mode", mode)
            .put("sessionId", session.identity.sessionId)
            .put("operationId", operation.snapshot.operationId).put("draft", operation.draft)
        val temporary = File(context.filesDir, "recovery-$runId.tmp")
        val ready = File(context.filesDir, "recovery-$runId.json")
        temporary.writeText(marker.toString())
        check(temporary.renameTo(ready))
        awaitCancellation()
    }
}
