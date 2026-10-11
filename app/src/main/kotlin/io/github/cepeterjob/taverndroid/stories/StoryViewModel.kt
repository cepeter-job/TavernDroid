package io.github.cepeterjob.taverndroid.stories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.cepeterjob.taverndroid.GenerationCoordinator
import io.github.cepeterjob.taverndroid.engine.Admission
import io.github.cepeterjob.taverndroid.engine.GenerationStore
import io.github.cepeterjob.taverndroid.engine.InputValidationException
import io.github.cepeterjob.taverndroid.engine.OpState
import io.github.cepeterjob.taverndroid.engine.OperationView
import io.github.cepeterjob.taverndroid.engine.SessionSnapshot
import io.github.cepeterjob.taverndroid.engine.StoryRepository
import io.github.cepeterjob.taverndroid.engine.Turn
import io.github.cepeterjob.taverndroid.engine.isTerminal
import io.github.cepeterjob.taverndroid.engine.validateUserText
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class StoryUiState(
    val session: SessionSnapshot? = null,
    val turns: List<Turn> = emptyList(),
    val operation: OperationView? = null,
    val draft: String = "",
    val recoveryRequired: Boolean = false,
    val commandPending: Boolean = false,
    val error: String? = null
)

class StoryViewModel(
    private val sessionId: String,
    private val repository: StoryRepository,
    private val store: GenerationStore,
    private val coordinator: GenerationCoordinator,
    scope: CoroutineScope? = null
) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)
    private val pending = MutableStateFlow(false)
    private val content = combine(
        repository.observeSessions(),
        repository.observeTurns(sessionId),
        store.observeLatestOperation(sessionId),
        coordinator.drafts
    ) { sessions, turns, operation, streaming ->
        val draft = if (operation?.state == OpState.Completed) {
            ""
        } else {
            streaming?.takeIf { it.operationId == operation?.snapshot?.operationId }?.text
                ?: operation?.draft.orEmpty()
        }
        StoryUiState(sessions.find { it.identity.sessionId == sessionId }, turns, operation, draft)
    }
    val state: StateFlow<StoryUiState> = combine(
        content,
        message,
        pending,
        coordinator.recoveryRequired
    ) { content, error, pending, recovery ->
        content.copy(error = error, commandPending = pending, recoveryRequired = recovery)
    }.stateIn(scope ?: viewModelScope, SharingStarted.Eagerly, StoryUiState())

    suspend fun send(text: String): Boolean {
        if (!pending.compareAndSet(false, true)) return false
        return try {
            validateUserText(text)
            val session = repository.readSession(sessionId) ?: return false
            val result = coordinator.send(
                sessionId,
                session.identity.revision,
                UUID.randomUUID().toString(),
                text
            )
            message.value = admissionMessage(result)
            result is Admission.Admitted || result is Admission.AlreadyKnown
        } catch (invalid: InputValidationException) {
            message.value = "Enter a message from 1 to 16,000 characters."
            false
        } catch (failure: Exception) {
            if (failure is CancellationException) throw failure
            message.value = "Could not save. Recover saved state before trying again."
            false
        } finally {
            pending.value = false
        }
    }

    suspend fun stop() = coordinator.stop()
    suspend fun retry(operationId: String) = action {
        message.value =
            admissionMessage(coordinator.retry(operationId, UUID.randomUUID().toString()))
    }
    suspend fun discardDraft(operationId: String) = action { store.discardDraft(operationId) }
    suspend fun recover() = action { coordinator.recover() }

    private suspend fun action(block: suspend () -> Unit) {
        try {
            block()
        } catch (failure: Exception) {
            if (failure is CancellationException) throw failure
            message.value = "Could not save. Recover saved state before trying again."
        }
    }

    private fun admissionMessage(result: Admission): String? = when (result) {
        is Admission.Admitted, is Admission.AlreadyKnown -> null
        Admission.Busy -> "Another reply is active or saved state needs recovery."
        Admission.Stale -> "Story changed. Review it and try again."
        Admission.Missing -> "Story is unavailable."
        Admission.Terminal -> "This reply is already completed."
    }
}
