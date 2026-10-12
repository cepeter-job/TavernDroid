package io.github.cepeterjob.taverndroid.storage

import androidx.room.withTransaction
import io.github.cepeterjob.taverndroid.engine.Admission
import io.github.cepeterjob.taverndroid.engine.CommitResult
import io.github.cepeterjob.taverndroid.engine.FailureCategory
import io.github.cepeterjob.taverndroid.engine.FakeProviderConfig
import io.github.cepeterjob.taverndroid.engine.FinishResult
import io.github.cepeterjob.taverndroid.engine.GenerationStore
import io.github.cepeterjob.taverndroid.engine.OpState
import io.github.cepeterjob.taverndroid.engine.OperationSnapshot
import io.github.cepeterjob.taverndroid.engine.OperationView
import io.github.cepeterjob.taverndroid.engine.TurnRole
import io.github.cepeterjob.taverndroid.engine.isTerminal
import io.github.cepeterjob.taverndroid.engine.validateUserText
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomGenerationStore(
    private val database: TavernDatabase,
    private val config: FakeProviderConfig = FakeProviderConfig()
) : GenerationStore {
    private val dao = database.generationDao()
    private val stories = database.storyDao()

    override suspend fun admitSend(
        sessionId: String,
        expectedRevision: Long,
        clientNonce: String,
        text: String
    ): Admission {
        validateUserText(text)
        require(clientNonce.isNotBlank())
        return database.withTransaction {
            dao.byNonce(clientNonce)?.let {
                return@withTransaction Admission.AlreadyKnown(
                    it.operationId,
                    OpState.valueOf(it.state)
                )
            }
            val session = stories.session(sessionId) ?: return@withTransaction Admission.Missing
            if (session.revision != expectedRevision) return@withTransaction Admission.Stale
            if (dao.activeCount() != 0) return@withTransaction Admission.Busy
            val history = stories.readTurns(sessionId)
            val requestId = UUID.randomUUID().toString()
            val user = TurnEntity(
                UUID.randomUUID().toString(),
                sessionId,
                (history.lastOrNull()?.ordinal ?: -1) + 1,
                TurnRole.User.name,
                text,
                requestId
            )
            stories.insertTurn(user)
            stories.advanceRevision(sessionId)
            val operation = GenerationEntity(
                operationId = UUID.randomUUID().toString(),
                requestId = requestId,
                clientNonce = clientNonce,
                attemptNumber = 1,
                userTurnId = user.id,
                sessionId = sessionId,
                branchId = session.branchId,
                incarnation = session.incarnation,
                revision = session.revision + 1,
                characterName = session.characterName,
                characterDescription = session.characterDescription,
                characterGreeting = session.characterGreeting,
                historyJson = encodeHistory((history + user).map { it.toTurn() }),
                userText = text,
                chunkCodeUnits = config.chunkCodeUnits,
                chunkDelayMillis = config.chunkDelayMillis,
                state = OpState.Queued.name,
                draft = "",
                failure = null,
                createdOrder = dao.nextOrder()
            )
            dao.insert(operation)
            Admission.Admitted(operation.snapshot())
        }
    }

    override suspend fun admitRetry(operationId: String, clientNonce: String): Admission =
        database.withTransaction {
            require(clientNonce.isNotBlank())
            dao.byNonce(clientNonce)?.let {
                return@withTransaction Admission.AlreadyKnown(
                    it.operationId,
                    OpState.valueOf(it.state)
                )
            }
            val previous = dao.operation(operationId) ?: return@withTransaction Admission.Missing
            if (previous.state == OpState.Completed.name) return@withTransaction Admission.Terminal
            if (!isCurrent(previous)) return@withTransaction Admission.Stale
            if (dao.latest(previous.sessionId)?.operationId !=
                previous.operationId
            ) {
                return@withTransaction Admission.Stale
            }
            if (OpState.valueOf(previous.state) !in
                listOf(OpState.Cancelled, OpState.Failed, OpState.Interrupted)
            ) {
                return@withTransaction Admission.Terminal
            }
            val user = stories.turn(previous.userTurnId)
            if (user?.requestId != previous.requestId ||
                user.role != TurnRole.User.name
            ) {
                return@withTransaction Admission.Stale
            }
            if (dao.activeCount() != 0) return@withTransaction Admission.Busy
            val retry = previous.copy(
                operationId = UUID.randomUUID().toString(),
                clientNonce = clientNonce,
                attemptNumber = previous.attemptNumber + 1,
                state = OpState.Queued.name,
                draft = "",
                failure = null,
                createdOrder = dao.nextOrder()
            )
            dao.insert(retry)
            Admission.Admitted(retry.snapshot())
        }

    override suspend fun transition(operationId: String, from: OpState, to: OpState): Boolean {
        val legal = (from == OpState.Queued && to == OpState.Preparing) ||
            (from == OpState.Preparing && to == OpState.Streaming) ||
            (from == OpState.Streaming && to == OpState.Committing)
        if (!legal) return false
        return database.withTransaction {
            val operation = dao.operation(operationId) ?: return@withTransaction false
            isCurrent(operation) && dao.transition(operationId, from.name, to.name) == 1
        }
    }

    override suspend fun checkpointDraft(operationId: String, text: String): Boolean =
        database.withTransaction {
            val operation = dao.operation(operationId) ?: return@withTransaction false
            if (OpState.valueOf(operation.state).isTerminal ||
                !isCurrent(operation)
            ) {
                return@withTransaction false
            }
            dao.draft(operationId, text.take(32000)) == 1
        }

    override suspend fun complete(snapshot: OperationSnapshot, reply: String): CommitResult =
        database.withTransaction {
            val operation =
                dao.operation(snapshot.operationId) ?: return@withTransaction CommitResult.Missing
            if (OpState.valueOf(
                    operation.state
                ).isTerminal
            ) {
                return@withTransaction CommitResult.Terminal
            }
            if (operation.snapshot() != snapshot ||
                !isCurrent(operation)
            ) {
                return@withTransaction CommitResult.Stale
            }
            val turns = stories.readTurns(operation.sessionId)
            stories.insertTurn(
                TurnEntity(
                    UUID.randomUUID().toString(),
                    operation.sessionId,
                    (turns.lastOrNull()?.ordinal ?: -1) + 1,
                    TurnRole.Assistant.name,
                    reply.take(32000),
                    operationId = operation.operationId
                )
            )
            dao.finish(operation.operationId, OpState.Completed.name, "", null)
            stories.advanceRevision(operation.sessionId)
            CommitResult.Accepted
        }

    override suspend fun finish(
        operationId: String,
        state: OpState,
        draft: String,
        failure: FailureCategory?
    ): FinishResult {
        require(state == OpState.Cancelled || state == OpState.Failed)
        return database.withTransaction {
            val operation =
                dao.operation(operationId) ?: return@withTransaction FinishResult.Missing
            if (OpState.valueOf(
                    operation.state
                ).isTerminal
            ) {
                return@withTransaction FinishResult.Terminal
            }
            if (!isCurrent(operation)) return@withTransaction FinishResult.Stale
            dao.finish(
                operationId,
                state.name,
                draft.take(32000),
                if (state ==
                    OpState.Failed
                ) {
                    failure?.name
                } else {
                    null
                }
            )
            FinishResult.Accepted
        }
    }

    override suspend fun discardDraft(operationId: String): Boolean = database.withTransaction {
        val operation = dao.operation(operationId) ?: return@withTransaction false
        val state = OpState.valueOf(operation.state)
        if (!state.isTerminal || state == OpState.Completed) return@withTransaction false
        dao.draft(operationId, "") == 1
    }

    override suspend fun recoverUnfinished(): Int = dao.recover()

    override fun observeLatestOperation(sessionId: String): Flow<OperationView?> =
        dao.observeLatest(sessionId).map { operation ->
            operation?.let {
                OperationView(
                    it.snapshot(),
                    OpState.valueOf(it.state),
                    it.draft,
                    it.failure?.let(FailureCategory::valueOf)
                )
            }
        }

    private suspend fun isCurrent(operation: GenerationEntity): Boolean {
        val session = stories.session(operation.sessionId) ?: return false
        return session.branchId == operation.branchId &&
            session.incarnation == operation.incarnation &&
            session.revision == operation.revision
    }
}
