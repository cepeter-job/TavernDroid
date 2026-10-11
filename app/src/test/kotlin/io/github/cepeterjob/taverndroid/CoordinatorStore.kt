package io.github.cepeterjob.taverndroid

import io.github.cepeterjob.taverndroid.engine.Admission
import io.github.cepeterjob.taverndroid.engine.CharacterInput
import io.github.cepeterjob.taverndroid.engine.CommitResult
import io.github.cepeterjob.taverndroid.engine.FailureCategory
import io.github.cepeterjob.taverndroid.engine.FakeProviderConfig
import io.github.cepeterjob.taverndroid.engine.FinishResult
import io.github.cepeterjob.taverndroid.engine.GenerationStore
import io.github.cepeterjob.taverndroid.engine.OpState
import io.github.cepeterjob.taverndroid.engine.OperationSnapshot
import io.github.cepeterjob.taverndroid.engine.OperationView
import io.github.cepeterjob.taverndroid.engine.SessionIdentity
import io.github.cepeterjob.taverndroid.engine.isTerminal
import io.github.cepeterjob.taverndroid.engine.validateUserText
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow

class CoordinatorStore : GenerationStore {
    val latest = MutableStateFlow<OperationView?>(null)
    val admissionStarted = CompletableDeferred<Unit>()
    val releaseAdmission = CompletableDeferred<Unit>()
    var suspended = false
    var failFinish = false
    var recoveries = 0
    var admissions = 0
    override suspend fun admitSend(
        sessionId: String,
        expectedRevision: Long,
        clientNonce: String,
        text: String
    ): Admission {
        validateUserText(text)
        admissions++
        val snapshot = OperationSnapshot(
            "00000000-0000-0000-0000-000000000001", "request", 1, "user",
            SessionIdentity(sessionId, "branch", 0, expectedRevision + 1),
            CharacterInput("Ada"), emptyList(), text, FakeProviderConfig()
        )
        latest.value = OperationView(snapshot, OpState.Queued, "", null)
        admissionStarted.complete(Unit)
        if (suspended) releaseAdmission.await()
        return Admission.Admitted(snapshot)
    }
    override suspend fun admitRetry(operationId: String, clientNonce: String): Admission {
        val old = latest.value!!
        latest.value = old.copy(state = OpState.Queued, draft = "")
        return Admission.Admitted(old.snapshot)
    }
    override suspend fun transition(operationId: String, from: OpState, to: OpState): Boolean {
        val old = latest.value ?: return false
        if (old.state != from) return false
        latest.value = old.copy(state = to)
        return true
    }
    override suspend fun checkpointDraft(operationId: String, text: String): Boolean {
        latest.value = latest.value!!.copy(draft = text)
        return true
    }
    override suspend fun complete(snapshot: OperationSnapshot, reply: String): CommitResult {
        if (latest.value!!.state.isTerminal) return CommitResult.Terminal
        latest.value = latest.value!!.copy(state = OpState.Completed, draft = reply)
        return CommitResult.Accepted
    }
    override suspend fun finish(
        operationId: String,
        state: OpState,
        draft: String,
        failure: FailureCategory?
    ): FinishResult {
        if (failFinish) throw IOException("synthetic persistence outage")
        if (latest.value!!.state.isTerminal) return FinishResult.Terminal
        latest.value = latest.value!!.copy(state = state, draft = draft, failure = failure)
        return FinishResult.Accepted
    }
    override suspend fun discardDraft(operationId: String): Boolean {
        latest.value = latest.value!!.copy(draft = "")
        return true
    }
    override suspend fun recoverUnfinished(): Int {
        recoveries++
        if (latest.value?.state?.isTerminal == false) {
            latest.value = latest.value!!.copy(state = OpState.Interrupted)
            return 1
        }
        return 0
    }
    override fun observeLatestOperation(sessionId: String) = latest
}
