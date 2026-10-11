package io.github.cepeterjob.taverndroid.providers

import io.github.cepeterjob.taverndroid.engine.CharacterInput
import io.github.cepeterjob.taverndroid.engine.FakeProviderConfig
import io.github.cepeterjob.taverndroid.engine.OperationSnapshot
import io.github.cepeterjob.taverndroid.engine.SessionIdentity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakeStreamingProviderTest {
    private fun snapshot(config: FakeProviderConfig = FakeProviderConfig()) = OperationSnapshot(
        "op", "request", 1, "user",
        SessionIdentity(
            "session",
            "main",
            1,
            1
        ),
        CharacterInput("Ada"), emptyList(), "Hello", config
    )

    @Test fun streamsDeterministicReplyUsingCapturedTiming() = runTest {
        val chunks = FakeStreamingProvider().stream(snapshot(FakeProviderConfig(4, 100))).toList()
        assertEquals("Synthetic reply: Hello", chunks.joinToString(""))
        assertTrue(chunks.all { it.length <= 4 })
        assertEquals(600, testScheduler.currentTime)
    }

    @Test fun rejectsInvalidCapturedConfiguration() {
        assertThrows(IllegalArgumentException::class.java) { FakeProviderConfig(0, 100) }
        assertThrows(IllegalArgumentException::class.java) { FakeProviderConfig(16, -1) }
    }

    @Test fun cancellationStopsBeforeAnyLateChunk() = runTest {
        val chunks = mutableListOf<String>()
        val job = launch { FakeStreamingProvider().stream(snapshot()).toList(chunks) }
        testScheduler.advanceTimeBy(100)
        testScheduler.runCurrent()
        job.cancel()
        testScheduler.advanceUntilIdle()
        assertEquals(1, chunks.size)
    }
}
