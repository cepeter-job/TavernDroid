package io.github.cepeterjob.taverndroid.engine

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GenerationRunnerTest {
    @Test fun validatesUserInputWithoutTrimmingContent() {
        assertEquals(" Hello ", validateUserText(" Hello "))
        assertEquals(16000, validateUserText("a".repeat(16000)).length)
        assertThrows(InputValidationException::class.java) { validateUserText(" ") }
        assertThrows(InputValidationException::class.java) { validateUserText("a".repeat(16001)) }
    }

    @Test fun capsDraftAndRatesUiAndCheckpointWrites() = runTest {
        val store = RecordingStore { testScheduler.currentTime }
        val uiTimes = mutableListOf<Long>()
        val updates = mutableListOf<DraftUpdate>()
        val provider =
            GenerationProvider {
                flow {
                    repeat(100) {
                        emit("x".repeat(400))
                        delay(20)
                    }
                }
            }
        GenerationRunner(store, provider, StandardTestDispatcher(testScheduler), {
            testScheduler.currentTime
        })
            .run(snapshot()) {
                uiTimes.add(testScheduler.currentTime)
                updates.add(it)
            }
        assertEquals(32000, store.reply!!.length)
        assertEquals(OpState.Completed, store.state)
        assertTrue(updates.all { it.text.length <= 32000 })
        assertTrue(uiTimes.zipWithNext().all { (first, second) -> second - first >= 100 })
        assertTrue(store.checkpointTimes.isNotEmpty())
        assertTrue(
            store.checkpointTimes.zipWithNext().all { (first, second) ->
                second - first >=
                    1000
            }
        )
        assertTrue(store.checkpointTimes.first() >= 1000)
    }

    @Test fun cancellationKeepsDraftAndPropagatesCancellation() = runTest {
        val store = RecordingStore { testScheduler.currentTime }
        val provider = GenerationProvider {
            flow {
                emit("partial")
                delay(10000)
                emit("late")
            }
        }
        val job =
            launch {
                GenerationRunner(store, provider, StandardTestDispatcher(testScheduler), {
                    testScheduler.currentTime
                }).run(snapshot()) {}
            }
        runCurrent()
        job.cancel()
        advanceUntilIdle()
        assertTrue(job.isCancelled)
        assertEquals(OpState.Cancelled, store.state)
        assertEquals("partial", store.durable)
        assertNull(store.reply)
    }

    @Test fun cancellationBeforeFirstChunkStillTerminalizes() = runTest {
        val store = RecordingStore { testScheduler.currentTime }
        val provider = GenerationProvider {
            flow {
                delay(10000)
                emit("late")
            }
        }
        val job =
            launch {
                GenerationRunner(store, provider, StandardTestDispatcher(testScheduler), {
                    testScheduler.currentTime
                }).run(snapshot()) {}
            }
        runCurrent()
        job.cancel()
        advanceUntilIdle()
        assertEquals(OpState.Cancelled, store.state)
        assertEquals("", store.durable)
        assertNull(store.reply)
    }

    @Test fun providerFailureKeepsDraftForExplicitRetry() = runTest {
        val store = RecordingStore { testScheduler.currentTime }
        val provider =
            GenerationProvider {
                flow {
                    emit("partial")
                    throw IOException("synthetic provider failure")
                }
            }
        GenerationRunner(store, provider, StandardTestDispatcher(testScheduler), {
            testScheduler.currentTime
        }).run(snapshot()) {}
        assertEquals(OpState.Failed, store.state)
        assertEquals(FailureCategory.Provider, store.failure)
        assertEquals("partial", store.durable)
        assertNull(store.reply)
    }

    @Test fun lostTerminalSavePreservesOnlyLastDurableState() = runTest {
        val store = RecordingStore { testScheduler.currentTime }
        store.durable = "already saved"
        store.failFinish = true
        val provider =
            GenerationProvider {
                flow {
                    emit("unsaved")
                    throw IOException("synthetic provider failure")
                }
            }
        try {
            GenerationRunner(store, provider, StandardTestDispatcher(testScheduler), {
                testScheduler.currentTime
            }).run(snapshot()) {}
            fail("Failed terminal save was hidden")
        } catch (expected: IOException) {
            assertEquals("already saved", store.durable)
            assertEquals(OpState.Streaming, store.state)
            assertNull(store.reply)
        }
    }

    @Test fun terminalAttemptNeverInvokesProviderAgain() = runTest {
        val store = RecordingStore { testScheduler.currentTime }
        store.state = OpState.Cancelled
        var requests = 0
        val provider = GenerationProvider {
            requests++
            flow { emit("late") }
        }
        GenerationRunner(store, provider, StandardTestDispatcher(testScheduler), {
            testScheduler.currentTime
        }).run(snapshot()) {}
        assertEquals(0, requests)
        assertNull(store.reply)
    }

    private fun snapshot() = OperationSnapshot(
        "op", "request", 1, "user", SessionIdentity("session", "main", 1, 1),
        CharacterInput("Ada"), emptyList(), "Hello", FakeProviderConfig()
    )

    private class RecordingStore(private val clock: () -> Long) : GenerationStore {
        var state = OpState.Queued
        var durable = ""
        var reply: String? = null
        var failure: FailureCategory? = null
        var failFinish = false
        val checkpointTimes = mutableListOf<Long>()
        override suspend fun transition(operationId: String, from: OpState, to: OpState): Boolean {
            if (state != from) return false
            state = to
            return true
        }
        override suspend fun checkpointDraft(operationId: String, text: String): Boolean {
            checkpointTimes.add(clock())
            durable = text
            return true
        }
        override suspend fun complete(snapshot: OperationSnapshot, reply: String): CommitResult {
            this.reply = reply
            state = OpState.Completed
            return CommitResult.Accepted
        }
        override suspend fun finish(
            operationId: String,
            state: OpState,
            draft: String,
            failure: FailureCategory?
        ): FinishResult {
            if (failFinish) throw IOException("synthetic save failure")
            this.state = state
            durable = draft
            this.failure = failure
            return FinishResult.Accepted
        }
        override suspend fun admitSend(
            sessionId: String,
            expectedRevision: Long,
            clientNonce: String,
            text: String
        ): Admission = error("unused")
        override suspend fun admitRetry(operationId: String, clientNonce: String): Admission =
            error("unused")
        override suspend fun discardDraft(operationId: String): Boolean = error("unused")
        override suspend fun recoverUnfinished(): Int = error("unused")
        override fun observeLatestOperation(sessionId: String): Flow<OperationView?> =
            MutableStateFlow(null)
    }
}
