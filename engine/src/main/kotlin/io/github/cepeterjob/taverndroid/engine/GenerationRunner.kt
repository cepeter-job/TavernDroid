package io.github.cepeterjob.taverndroid.engine

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class GenerationPersistenceException(cause: Exception) :
    IOException("Could not save generation", cause)

@OptIn(ExperimentalCoroutinesApi::class)
class GenerationRunner(
    private val store: GenerationStore,
    private val provider: GenerationProvider,
    private val dispatcher: CoroutineDispatcher,
    private val clock: () -> Long
) {
    suspend fun run(snapshot: OperationSnapshot, onDraft: (DraftUpdate) -> Unit) =
        withContext(dispatcher) {
            val draft = StringBuilder()
            val owner = currentCoroutineContext()
            var lastCheckpoint = clock()
            var lastUi = clock() - 100
            try {
                if (!persist {
                        store.transition(snapshot.operationId, OpState.Queued, OpState.Preparing)
                    }
                ) {
                    return@withContext
                }
                if (!persist {
                        store.transition(snapshot.operationId, OpState.Preparing, OpState.Streaming)
                    }
                ) {
                    return@withContext
                }
                provider.stream(snapshot).transformWhile { chunk ->
                    emit(chunk)
                    draft.length < 32000
                }.collect { chunk ->
                    owner.ensureActive()
                    draft.append(chunk.take(32000 - draft.length))
                    val now = clock()
                    if (now - lastCheckpoint >= 1000) {
                        if (!persist {
                                store.checkpointDraft(snapshot.operationId, draft.toString())
                            }
                        ) {
                            throw CancellationException("Generation is no longer current")
                        }
                        lastCheckpoint = now
                    }
                    if (now - lastUi >= 100) {
                        onDraft(DraftUpdate(snapshot.operationId, draft.toString()))
                        lastUi = now
                    }
                }
                owner.ensureActive()
                if (persist {
                        store.transition(
                            snapshot.operationId,
                            OpState.Streaming,
                            OpState.Committing
                        )
                    }
                ) {
                    persist { store.complete(snapshot, draft.toString()) }
                }
            } catch (cancelled: CancellationException) {
                // Saving must survive cancellation without blocking shutdown indefinitely.
                try {
                    withContext(NonCancellable) {
                        withTimeout(2000) {
                            persist {
                                store.finish(
                                    snapshot.operationId,
                                    OpState.Cancelled,
                                    draft.toString()
                                )
                            }
                        }
                    }
                } catch (saveFailure: Exception) {
                    // The durable row remains authoritative; startup will recover unfinished work.
                }
                throw cancelled
            } catch (failure: Exception) {
                val category = if (failure is GenerationPersistenceException) {
                    FailureCategory.Storage
                } else {
                    FailureCategory.Provider
                }
                persist {
                    store.finish(snapshot.operationId, OpState.Failed, draft.toString(), category)
                }
            }
        }

    private suspend fun <T> persist(block: suspend () -> T): T = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        throw GenerationPersistenceException(failure)
    }
}
