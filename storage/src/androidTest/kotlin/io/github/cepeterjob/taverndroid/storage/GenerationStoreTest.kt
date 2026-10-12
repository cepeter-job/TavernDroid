package io.github.cepeterjob.taverndroid.storage

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import io.github.cepeterjob.taverndroid.engine.Admission
import io.github.cepeterjob.taverndroid.engine.CharacterInput
import io.github.cepeterjob.taverndroid.engine.CommitResult
import io.github.cepeterjob.taverndroid.engine.FinishResult
import io.github.cepeterjob.taverndroid.engine.OpState
import io.github.cepeterjob.taverndroid.engine.OperationSnapshot
import io.github.cepeterjob.taverndroid.engine.SessionSnapshot
import io.github.cepeterjob.taverndroid.engine.TurnRole
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class GenerationStoreTest {
    private lateinit var context: Context
    private lateinit var database: TavernDatabase
    private lateinit var repository: RoomStoryRepository
    private lateinit var store: RoomGenerationStore
    private lateinit var filename: String

    @Before fun open() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        filename = "synthetic-generation-" + UUID.randomUUID() + ".db"
        reopen()
    }
    private fun reopen() {
        database = Room.databaseBuilder(context, TavernDatabase::class.java, filename).build()
        repository = RoomStoryRepository(database)
        store = RoomGenerationStore(database)
    }

    @After fun close() {
        database.close()
        context.deleteDatabase(filename)
    }
    private suspend fun session(): SessionSnapshot =
        repository.startSession(repository.createCharacter(CharacterInput("Ada")).id)
    private suspend fun send(
        session: SessionSnapshot,
        nonce: String = UUID.randomUUID().toString()
    ): OperationSnapshot = (
        store.admitSend(
            session.identity.sessionId,
            session.identity.revision,
            nonce,
            "Hello"
        ) as Admission.Admitted
        ).snapshot

    @Test fun duplicateSendProducesOneUserAndOneOperation() = runTest {
        val session = session()
        val snapshot = send(session, "duplicate")
        assertEquals(
            Admission.AlreadyKnown(snapshot.operationId, OpState.Queued),
            store.admitSend(
                session.identity.sessionId,
                session.identity.revision,
                "duplicate",
                "Hello"
            )
        )
        assertEquals(
            1,
            repository.observeTurns(session.identity.sessionId).first().count {
                it.role ==
                    TurnRole.User
            }
        )
        assertEquals(
            snapshot,
            store.observeLatestOperation(session.identity.sessionId).first()!!.snapshot
        )
    }

    @Test fun concurrentSessionsAdmitOnlyOneGlobally() = runTest {
        val first = session()
        val second = session()
        val one =
            async {
                store.admitSend(first.identity.sessionId, first.identity.revision, "one", "Hello")
            }
        val two =
            async {
                store.admitSend(second.identity.sessionId, second.identity.revision, "two", "Hello")
            }
        val results = listOf(one.await(), two.await())
        assertEquals(1, results.count { it is Admission.Admitted })
        assertEquals(1, results.count { it == Admission.Busy })
    }

    @Test fun retryKeepsUserTurnAndCompletedWinsLaterStop() = runTest {
        val session = session()
        val original = send(session)
        assertEquals(
            FinishResult.Accepted,
            store.finish(original.operationId, OpState.Cancelled, "partial")
        )
        val retry = (store.admitRetry(original.operationId, "retry") as Admission.Admitted).snapshot
        assertEquals(2, retry.attemptNumber)
        assertEquals(original.userTurnId, retry.userTurnId)
        assertEquals(CommitResult.Accepted, store.complete(retry, "Reply"))
        assertEquals(
            FinishResult.Terminal,
            store.finish(retry.operationId, OpState.Cancelled, "late")
        )
        assertEquals(CommitResult.Terminal, store.complete(retry, "duplicate"))
        assertEquals(Admission.Terminal, store.admitRetry(retry.operationId, "again"))
        assertEquals(
            1,
            repository.observeTurns(session.identity.sessionId).first().count {
                it.role ==
                    TurnRole.User
            }
        )
        assertEquals(
            1,
            repository.observeTurns(session.identity.sessionId).first().count {
                it.role ==
                    TurnRole.Assistant
            }
        )
    }

    @Test fun stopPreventsLateCommitCheckpointAndDraftResurrection() = runTest {
        val session = session()
        val snapshot = send(session)
        assertTrue(store.checkpointDraft(snapshot.operationId, "saved"))
        store.finish(snapshot.operationId, OpState.Cancelled, "saved")
        assertEquals(CommitResult.Terminal, store.complete(snapshot, "late"))
        assertTrue(store.discardDraft(snapshot.operationId))
        assertFalse(store.checkpointDraft(snapshot.operationId, "resurrected"))
        val retry = (store.admitRetry(snapshot.operationId, "retry") as Admission.Admitted).snapshot
        assertFalse(store.checkpointDraft(snapshot.operationId, "old attempt"))
        assertEquals("", store.observeLatestOperation(session.identity.sessionId).first()!!.draft)
        assertEquals(2, retry.attemptNumber)
        assertEquals(1, repository.observeTurns(session.identity.sessionId).first().size)
    }

    @Test fun allTerminalStatesRejectRevivalAndPublication() = runTest {
        val session = session()
        for (terminal in listOf(
            OpState.Cancelled,
            OpState.Failed,
            OpState.Interrupted,
            OpState.Completed
        )) {
            val current = repository.readSession(session.identity.sessionId)!!
            val snapshot = send(current)
            if (terminal == OpState.Completed) {
                store.complete(snapshot, "Reply")
            } else if (terminal == OpState.Interrupted) {
                store.recoverUnfinished()
            } else {
                store.finish(snapshot.operationId, terminal, "")
            }
            assertFalse(store.transition(snapshot.operationId, terminal, OpState.Streaming))
            assertEquals(CommitResult.Terminal, store.complete(snapshot, "late"))
        }
    }

    @Test fun transitionCannotWriteTerminalStateDirectly() = runTest {
        val snapshot = send(session())
        assertFalse(store.transition(snapshot.operationId, OpState.Queued, OpState.Completed))
        assertTrue(store.transition(snapshot.operationId, OpState.Queued, OpState.Preparing))
        assertTrue(store.transition(snapshot.operationId, OpState.Preparing, OpState.Streaming))
        assertTrue(store.transition(snapshot.operationId, OpState.Streaming, OpState.Committing))
    }

    @Test fun staleRevisionIncarnationAndBranchRejectPublication() = runTest {
        for (change in listOf(
            "revision = revision + 1",
            "incarnation = incarnation + 1",
            "branchId = 'other'"
        )) {
            val session = session()
            val snapshot = send(session)
            database.openHelper.writableDatabase.execSQL(
                "UPDATE sessions SET $change WHERE id = ?",
                arrayOf<Any>(session.identity.sessionId)
            )
            assertEquals(CommitResult.Stale, store.complete(snapshot, "late"))
            assertFalse(store.checkpointDraft(snapshot.operationId, "late"))
            assertEquals(Admission.Stale, store.admitRetry(snapshot.operationId, "stale-retry"))
            assertEquals(
                0,
                repository.observeTurns(session.identity.sessionId).first().count {
                    it.role ==
                        TurnRole.Assistant
                }
            )
            store.recoverUnfinished()
        }
    }

    @Test fun deletedSessionIsNotRecreatedByLateReply() = runTest {
        val session = session()
        val snapshot = send(session)
        database.openHelper.writableDatabase.execSQL(
            "DELETE FROM sessions WHERE id = ?",
            arrayOf<Any>(session.identity.sessionId)
        )
        assertEquals(CommitResult.Missing, store.complete(snapshot, "late"))
        assertTrue(repository.observeSessions().first().isEmpty())
    }

    @Test fun sharedResetFixtureRejectsLatePublication() = runTest {
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val fixture =
            JSONObject(
                testContext.assets.open("stale-generation.json").bufferedReader().use {
                    it.readText()
                }
            )
        val input = fixture.getJSONObject("input")
        val captured = input.getJSONObject("captured")
        val current = input.getJSONObject("current")
        val initial = session()
        database.openHelper.writableDatabase.execSQL(
            "UPDATE sessions SET id = ?, branchId = ?, incarnation = ?, revision = ? WHERE id = ?",
            arrayOf<Any>(
                captured.getString("sessionId"),
                captured.getString("branchId"),
                captured.getLong("incarnation"),
                captured.getLong("revision") - 1,
                initial.identity.sessionId
            )
        )
        val snapshot = send(repository.readSession(captured.getString("sessionId"))!!)
        database.openHelper.writableDatabase.execSQL(
            "UPDATE sessions SET incarnation = ?, revision = ? WHERE id = ?",
            arrayOf<Any>(
                current.getLong("incarnation"),
                current.getLong("revision"),
                current.getString("sessionId")
            )
        )
        assertEquals(CommitResult.Stale, store.complete(snapshot, input.getString("lateReply")))
        database.openHelper.writableDatabase.execSQL(
            "DELETE FROM turns WHERE sessionId = ?",
            arrayOf<Any>(current.getString("sessionId"))
        )
        assertEquals(CommitResult.Missing, store.complete(snapshot, input.getString("lateReply")))
        assertEquals(
            fixture.getJSONObject("expected").getInt("canonicalTurnCountAfterReset"),
            repository.observeTurns(current.getString("sessionId")).first().size
        )
    }

    @Test fun assistantAndCompletedStateRollBackTogether() = runTest {
        val session = session()
        val snapshot = send(session)
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER synthetic_commit_failure " +
                "BEFORE UPDATE OF state ON generation_operations " +
                "WHEN NEW.state = 'Completed' BEGIN SELECT RAISE(ABORT, 'synthetic failure'); END"
        )
        try {
            store.complete(snapshot, "Reply")
            fail("Injected failure not observed")
        } catch (
            expected: Exception
        ) {
            assertEquals(
                OpState.Queued,
                store.observeLatestOperation(session.identity.sessionId).first()!!.state
            )
            assertEquals(
                0,
                repository.observeTurns(session.identity.sessionId).first().count {
                    it.role ==
                        TurnRole.Assistant
                }
            )
        }
    }

    @Test fun failedAdmissionRollsBackUserAndRevision() = runTest {
        val session = session()
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER synthetic_admission_failure BEFORE INSERT ON generation_operations BEGIN SELECT RAISE(ABORT, 'synthetic failure'); END"
        )
        try {
            send(session)
            fail("Injected failure not observed")
        } catch (expected: Exception) {
            assertTrue(repository.observeTurns(session.identity.sessionId).first().isEmpty())
            assertEquals(0, repository.readSession(session.identity.sessionId)!!.identity.revision)
            assertEquals(null, store.observeLatestOperation(session.identity.sessionId).first())
        }
    }

    @Test fun reopeningInterruptsQueuedAndStreamingWithoutReplay() = runTest {
        val session = session()
        val queued = send(session)
        database.close()
        reopen()
        assertEquals(1, store.recoverUnfinished())
        assertEquals(
            OpState.Interrupted,
            store.observeLatestOperation(session.identity.sessionId).first()!!.state
        )
        val retry = (store.admitRetry(queued.operationId, "retry") as Admission.Admitted).snapshot
        store.transition(retry.operationId, OpState.Queued, OpState.Preparing)
        store.transition(retry.operationId, OpState.Preparing, OpState.Streaming)
        store.checkpointDraft(retry.operationId, "saved draft")
        database.close()
        reopen()
        assertEquals(1, store.recoverUnfinished())
        assertEquals(
            "saved draft",
            store.observeLatestOperation(session.identity.sessionId).first()!!.draft
        )
        assertEquals(1, repository.observeTurns(session.identity.sessionId).first().size)
        assertEquals(0, store.recoverUnfinished())
    }

    @Test fun reopeningCompletedOutputNeverDuplicatesIt() = runTest {
        val session = session()
        val snapshot = send(session)
        store.complete(snapshot, "Reply")
        database.close()
        reopen()
        assertEquals(0, store.recoverUnfinished())
        assertEquals(
            OpState.Completed,
            store.observeLatestOperation(session.identity.sessionId).first()!!.state
        )
        assertEquals(CommitResult.Terminal, store.complete(snapshot, "again"))
        assertEquals(
            listOf("Hello", "Reply"),
            repository.observeTurns(session.identity.sessionId).first().map {
                it.text
            }
        )
    }
}
