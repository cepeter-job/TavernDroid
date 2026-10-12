package io.github.cepeterjob.taverndroid

import io.github.cepeterjob.taverndroid.engine.Admission
import io.github.cepeterjob.taverndroid.engine.DraftUpdate
import io.github.cepeterjob.taverndroid.engine.GenerationProvider
import io.github.cepeterjob.taverndroid.engine.GenerationRunner
import io.github.cepeterjob.taverndroid.engine.GenerationStore
import io.github.cepeterjob.taverndroid.engine.OpState
import io.github.cepeterjob.taverndroid.engine.OperationSnapshot
import io.github.cepeterjob.taverndroid.engine.isTerminal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class GenerationCoordinator(
    private val store: GenerationStore,
    provider: GenerationProvider,
    private val scope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
    clock: () -> Long,
    private val diagnostics: Diagnostics = Diagnostics {}
) {
    private val mutableDrafts = MutableStateFlow<DraftUpdate?>(null)
    val drafts: StateFlow<DraftUpdate?> = mutableDrafts
    private val mutableRecovery = MutableStateFlow(false)
    val recoveryRequired: StateFlow<Boolean> = mutableRecovery
    private val runner = GenerationRunner(store, provider, dispatcher, clock)
    private val initializeMutex = Mutex()
    private val commandMutex = Mutex()
    private val ownership = Any()
    private var initialized = false
    private var foreground = false
    private var epoch = 0L
    private var active: Active? = null
    private data class Active(val snapshot: OperationSnapshot, val job: Job)

    suspend fun initialize() = initializeMutex.withLock {
        if (!initialized) {
            try {
                store.recoverUnfinished()
                diagnostics.record(DiagnosticEvent.Recovery)
                initialized = true
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                mutableRecovery.value = true
                throw failure
            }
        }
    }

    fun onForeground() {
        synchronized(ownership) {
            if (!foreground) epoch++
            foreground = true
        }
    }

    fun onBackground() {
        val owner = synchronized(ownership) {
            foreground = false
            epoch++
            active
        }
        if (owner != null) scope.launch { cancel(owner) }
    }

    suspend fun send(
        sessionId: String,
        expectedRevision: Long,
        clientNonce: String,
        text: String
    ): Admission = command { store.admitSend(sessionId, expectedRevision, clientNonce, text) }

    suspend fun retry(operationId: String, clientNonce: String): Admission =
        command { store.admitRetry(operationId, clientNonce) }

    private suspend fun command(admit: suspend () -> Admission): Admission {
        val captured = synchronized(ownership) { if (foreground) epoch else null }
            ?: return Admission.Busy
        // Awaiting callers may disappear; admission and the worker belong to the application.
        return scope.async(start = CoroutineStart.UNDISPATCHED) {
            commandMutex.withLock {
                initialize()
                if (mutableRecovery.value) return@withLock Admission.Busy
                val admitted = admit()
                if (admitted is Admission.Admitted) {
                    val snapshot = admitted.snapshot
                    val allowed = synchronized(ownership) { foreground && captured == epoch }
                    if (!allowed) {
                        saveCancelled(snapshot)
                    } else {
                        val job = scope.launch(start = CoroutineStart.LAZY) {
                            try {
                                diagnostics.record(
                                    DiagnosticEvent.GenerationStarted,
                                    snapshot.operationId
                                )
                                runner.run(snapshot) { mutableDrafts.value = it }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (failure: Exception) {
                                mutableRecovery.value = true
                                diagnostics.record(
                                    DiagnosticEvent.GenerationFailed,
                                    snapshot.operationId
                                )
                            } finally {
                                synchronized(ownership) {
                                    if (active?.snapshot?.operationId ==
                                        snapshot.operationId
                                    ) {
                                        active = null
                                    }
                                }
                                mutableDrafts.value = null
                            }
                        }
                        val owner = Active(snapshot, job)
                        val installed = synchronized(ownership) {
                            if (foreground && epoch == captured) {
                                active = owner
                                true
                            } else {
                                false
                            }
                        }
                        if (installed) {
                            job.start()
                        } else {
                            job.cancel()
                            saveCancelled(snapshot)
                        }
                    }
                }
                admitted
            }
        }.await()
    }

    suspend fun stop() {
        val owner = synchronized(ownership) {
            epoch++
            active
        }
        if (owner != null) withContext(NonCancellable) { cancel(owner) }
    }

    private suspend fun cancel(owner: Active) {
        try {
            withTimeout(3000) { owner.job.cancelAndJoin() }
            saveCancelled(owner.snapshot)
        } catch (failure: Exception) {
            mutableRecovery.value = true
        } finally {
            synchronized(ownership) {
                if (active == owner && owner.job.isCompleted) active = null
            }
        }
    }

    private suspend fun saveCancelled(snapshot: OperationSnapshot) {
        try {
            withContext(NonCancellable) {
                withTimeout(2000) {
                    val durable = store.observeLatestOperation(snapshot.identity.sessionId).first()
                    val draft = durable?.takeIf {
                        it.snapshot.operationId == snapshot.operationId
                    }?.draft.orEmpty()
                    store.finish(snapshot.operationId, OpState.Cancelled, draft)
                }
            }
            diagnostics.record(DiagnosticEvent.GenerationCancelled, snapshot.operationId)
        } catch (failure: Exception) {
            mutableRecovery.value = true
        }
    }

    suspend fun recover() {
        stop()
        commandMutex.withLock {
            check(synchronized(ownership) { active == null }) { "Generation still stopping" }
            store.recoverUnfinished()
            initialized = true
            mutableRecovery.value = false
            mutableDrafts.value = null
        }
    }
}
