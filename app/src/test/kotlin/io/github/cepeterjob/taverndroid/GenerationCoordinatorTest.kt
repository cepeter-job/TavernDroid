package io.github.cepeterjob.taverndroid

import io.github.cepeterjob.taverndroid.engine.Admission
import io.github.cepeterjob.taverndroid.engine.GenerationProvider
import io.github.cepeterjob.taverndroid.engine.OpState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GenerationCoordinatorTest {
    @Test fun backgroundBeforeWorkerStartsPreventsProviderEntry() = runTest {
        val store = CoordinatorStore()
        var calls = 0
        val coordinator =
            GenerationCoordinator(
                store,
                GenerationProvider {
                    flow {
                        calls++
                        emit("reply")
                    }
                },
                backgroundScope,
                StandardTestDispatcher(testScheduler),
                { testScheduler.currentTime }
            )
        coordinator.onForeground()
        coordinator.send("session", 0, "nonce", "hello")
        coordinator.onBackground()
        runCurrent()
        assertEquals(0, calls)
        assertEquals(OpState.Cancelled, store.latest.value!!.state)
    }

    @Test fun queuedCancellationSaveFailureCanRecoverExplicitly() = runTest {
        val store = CoordinatorStore().apply { failFinish = true }
        val coordinator =
            GenerationCoordinator(
                store,
                GenerationProvider { flow { emit("reply") } },
                backgroundScope,
                StandardTestDispatcher(testScheduler),
                { testScheduler.currentTime }
            )
        coordinator.onForeground()
        coordinator.send("session", 0, "nonce", "hello")
        coordinator.stop()
        assertTrue(coordinator.recoveryRequired.value)
        store.failFinish = false
        coordinator.recover()
        assertEquals(OpState.Interrupted, store.latest.value!!.state)
    }

    @Test fun recoversOnlyOnceBeforeAdmission() = runTest {
        val store = CoordinatorStore()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val coordinator =
            GenerationCoordinator(
                store,
                GenerationProvider { flow { emit("reply") } },
                backgroundScope,
                dispatcher,
                { testScheduler.currentTime }
            )
        coordinator.onForeground()
        coordinator.initialize()
        coordinator.initialize()
        assertEquals(1, store.recoveries)
        assertTrue(coordinator.send("session", 0, "nonce", "hello") is Admission.Admitted)
    }

    @Test fun stopBeforeWorkerStartsTerminalizesAdmission() = runTest {
        val store = CoordinatorStore()
        var calls = 0
        val coordinator =
            GenerationCoordinator(
                store,
                GenerationProvider {
                    flow {
                        calls++
                        emit("reply")
                    }
                },
                backgroundScope,
                StandardTestDispatcher(testScheduler),
                { testScheduler.currentTime }
            )
        coordinator.initialize()
        coordinator.onForeground()
        assertTrue(coordinator.send("session", 0, "nonce", "hello") is Admission.Admitted)
        coordinator.stop()
        runCurrent()
        assertEquals(OpState.Cancelled, store.latest.value!!.state)
        assertEquals(0, calls)
    }

    @Test fun backgroundDuringAdmissionPreventsLaterLaunch() = runTest {
        val store = CoordinatorStore().apply { suspended = true }
        var calls = 0
        val coordinator =
            GenerationCoordinator(
                store,
                GenerationProvider {
                    flow {
                        calls++
                        emit("reply")
                    }
                },
                backgroundScope,
                StandardTestDispatcher(testScheduler),
                { testScheduler.currentTime }
            )
        coordinator.initialize()
        coordinator.onForeground()
        val send = async { coordinator.send("session", 0, "nonce", "hello") }
        runCurrent()
        assertTrue(store.admissionStarted.isCompleted)
        coordinator.onBackground()
        coordinator.onForeground()
        store.releaseAdmission.complete(Unit)
        assertTrue(send.await() is Admission.Admitted)
        runCurrent()
        assertEquals(OpState.Cancelled, store.latest.value!!.state)
        assertEquals(0, calls)
    }

    @Test fun stopDuringAdmissionPreventsLaterLaunch() = runTest {
        val store = CoordinatorStore().apply { suspended = true }
        var calls = 0
        val coordinator =
            GenerationCoordinator(
                store,
                GenerationProvider {
                    flow {
                        calls++
                        emit("reply")
                    }
                },
                backgroundScope,
                StandardTestDispatcher(testScheduler),
                { testScheduler.currentTime }
            )
        coordinator.initialize()
        coordinator.onForeground()
        val send = async { coordinator.send("session", 0, "nonce", "hello") }
        runCurrent()
        assertTrue(store.admissionStarted.isCompleted)
        coordinator.stop()
        store.releaseAdmission.complete(Unit)
        assertTrue(send.await() is Admission.Admitted)
        runCurrent()
        assertEquals(OpState.Cancelled, store.latest.value!!.state)
        assertEquals(0, calls)
    }

    @Test fun cancellingCallerCannotAbandonCommittedAttempt() = runTest {
        val store = CoordinatorStore().apply { suspended = true }
        var calls = 0
        val coordinator =
            GenerationCoordinator(
                store,
                GenerationProvider {
                    flow {
                        calls++
                        emit("reply")
                    }
                },
                backgroundScope,
                StandardTestDispatcher(testScheduler),
                { testScheduler.currentTime }
            )
        coordinator.initialize()
        coordinator.onForeground()
        val send = launch { coordinator.send("session", 0, "nonce", "hello") }
        runCurrent()
        assertTrue(store.admissionStarted.isCompleted)
        send.cancel()
        store.releaseAdmission.complete(Unit)
        runCurrent()
        assertEquals(OpState.Completed, store.latest.value!!.state)
        assertEquals(1, calls)
        assertEquals(1, store.admissions)
    }

    @Test fun backgroundPreservesDraftWithoutAutomaticRetry() = runTest {
        val store = CoordinatorStore()
        var calls = 0
        val coordinator = GenerationCoordinator(
            store,
            GenerationProvider {
                flow {
                    calls++
                    emit("partial")
                    delay(10000)
                    emit("late")
                }
            },
            backgroundScope,
            StandardTestDispatcher(testScheduler),
            { testScheduler.currentTime }
        )
        coordinator.initialize()
        coordinator.onForeground()
        coordinator.send("session", 0, "nonce", "hello")
        runCurrent()
        coordinator.onBackground()
        runCurrent()
        assertEquals(OpState.Cancelled, store.latest.value!!.state)
        assertEquals("partial", store.latest.value!!.draft)
        coordinator.onForeground()
        advanceTimeBy(10000)
        runCurrent()
        assertEquals(1, calls)
        assertEquals(OpState.Cancelled, store.latest.value!!.state)
    }

    @Test fun failedTerminalSaveRequiresExplicitRecovery() = runTest {
        val store = CoordinatorStore().apply { failFinish = true }
        val coordinator = GenerationCoordinator(
            store,
            GenerationProvider { flow { delay(10000) } },
            backgroundScope,
            StandardTestDispatcher(testScheduler),
            { testScheduler.currentTime }
        )
        coordinator.initialize()
        coordinator.onForeground()
        coordinator.send("session", 0, "nonce", "hello")
        runCurrent()
        coordinator.stop()
        assertTrue(coordinator.recoveryRequired.value)
        assertEquals(OpState.Streaming, store.latest.value!!.state)
        store.failFinish = false
        coordinator.recover()
        assertEquals(OpState.Interrupted, store.latest.value!!.state)
    }
}
