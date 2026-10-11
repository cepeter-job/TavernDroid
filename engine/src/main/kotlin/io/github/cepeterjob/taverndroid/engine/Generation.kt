package io.github.cepeterjob.taverndroid.engine

import kotlinx.coroutines.flow.Flow

fun validateUserText(text: String): String {
    if (text.isBlank() || text.length > 16000) throw InputValidationException("text")
    return text
}

data class FakeProviderConfig(val chunkCodeUnits: Int = 16, val chunkDelayMillis: Long = 100) {
    init {
        require(chunkCodeUnits > 0)
        require(chunkDelayMillis >= 0)
    }
}

data class OperationSnapshot(
    val operationId: String,
    val requestId: String,
    val attemptNumber: Int,
    val userTurnId: String,
    val identity: SessionIdentity,
    val character: CharacterInput,
    val history: List<Turn>,
    val userText: String,
    val config: FakeProviderConfig
)

enum class OpState {
    Queued,
    Preparing,
    Streaming,
    Committing,
    Completed,
    Cancelled,
    Failed,
    Interrupted
}

val OpState.isTerminal: Boolean
    get() = this == OpState.Completed || this == OpState.Cancelled || this == OpState.Failed ||
        this == OpState.Interrupted
enum class FailureCategory { Provider, Storage }
enum class CommitResult { Accepted, Stale, Terminal, Missing }
enum class FinishResult { Accepted, Stale, Terminal, Missing }

sealed interface Admission {
    data class Admitted(val snapshot: OperationSnapshot) : Admission
    data class AlreadyKnown(val operationId: String, val state: OpState) : Admission
    data object Busy : Admission
    data object Stale : Admission
    data object Missing : Admission
    data object Terminal : Admission
}

data class OperationView(
    val snapshot: OperationSnapshot,
    val state: OpState,
    val draft: String,
    val failure: FailureCategory? = null
)

data class DraftUpdate(val operationId: String, val text: String)

fun interface GenerationProvider {
    fun stream(snapshot: OperationSnapshot): Flow<String>
}
