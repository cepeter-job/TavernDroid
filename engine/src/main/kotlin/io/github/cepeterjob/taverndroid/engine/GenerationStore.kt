package io.github.cepeterjob.taverndroid.engine

import kotlinx.coroutines.flow.Flow

interface GenerationStore {
    suspend fun admitSend(
        sessionId: String,
        expectedRevision: Long,
        clientNonce: String,
        text: String
    ): Admission
    suspend fun admitRetry(operationId: String, clientNonce: String): Admission
    suspend fun transition(operationId: String, from: OpState, to: OpState): Boolean
    suspend fun checkpointDraft(operationId: String, text: String): Boolean
    suspend fun complete(snapshot: OperationSnapshot, reply: String): CommitResult
    suspend fun finish(
        operationId: String,
        state: OpState,
        draft: String,
        failure: FailureCategory? = null
    ): FinishResult
    suspend fun discardDraft(operationId: String): Boolean
    suspend fun recoverUnfinished(): Int
    fun observeLatestOperation(sessionId: String): Flow<OperationView?>
}
