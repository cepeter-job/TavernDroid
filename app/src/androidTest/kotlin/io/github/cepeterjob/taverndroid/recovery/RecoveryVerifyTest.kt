package io.github.cepeterjob.taverndroid.recovery

import androidx.test.platform.app.InstrumentationRegistry
import io.github.cepeterjob.taverndroid.TavernDroidApplication
import io.github.cepeterjob.taverndroid.TestApplication
import io.github.cepeterjob.taverndroid.engine.OpState
import io.github.cepeterjob.taverndroid.engine.TurnRole
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

class RecoveryVerifyTest {
    @Test fun restartPreservesDurableOutcomeWithoutReplay() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue(arguments.getString("phase") == "verify")
        val runId = UUID.fromString(arguments.getString("recoveryRunId")).toString()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val markerFile = File(context.filesDir, "recovery-$runId.json")
        val marker = JSONObject(markerFile.readText())
        assertEquals(runId, marker.getString("runId"))
        val container = (context.applicationContext as TavernDroidApplication).container
        container.coordinator.initialize()
        val sessionId = marker.getString("sessionId")
        val operation = container.generations.observeLatestOperation(sessionId).first()!!
        val completed = marker.getString("mode") == "completed"
        assertEquals(marker.getString("operationId"), operation.snapshot.operationId)
        assertEquals(if (completed) OpState.Completed else OpState.Interrupted, operation.state)
        assertEquals(marker.getString("draft"), operation.draft)
        val turns = container.repository.observeTurns(sessionId).first()
        assertEquals(1, turns.count { it.role == TurnRole.User })
        assertEquals("Synthetic recovery", turns.first { it.role == TurnRole.User }.text)
        assertEquals(if (completed) 1 else 0, turns.count { it.role == TurnRole.Assistant })
        if (completed) {
            assertEquals(
                "Synthetic committed reply",
                turns.first {
                    it.role == TurnRole.Assistant
                }.text
            )
        }
        assertEquals(0, TestApplication.invocations.get())
        check(markerFile.delete())
    }
}
