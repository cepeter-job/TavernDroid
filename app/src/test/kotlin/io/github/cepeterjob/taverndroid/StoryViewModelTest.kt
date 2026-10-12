package io.github.cepeterjob.taverndroid

import io.github.cepeterjob.taverndroid.engine.Character
import io.github.cepeterjob.taverndroid.engine.CharacterInput
import io.github.cepeterjob.taverndroid.engine.GenerationProvider
import io.github.cepeterjob.taverndroid.engine.SessionIdentity
import io.github.cepeterjob.taverndroid.engine.SessionSnapshot
import io.github.cepeterjob.taverndroid.engine.StoryRepository
import io.github.cepeterjob.taverndroid.engine.Turn
import io.github.cepeterjob.taverndroid.engine.TurnRole
import io.github.cepeterjob.taverndroid.stories.StoryViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StoryViewModelTest {
    private val session =
        SessionSnapshot(SessionIdentity("session", "branch", 0, 0), CharacterInput("Ada"))
    private fun repository() = object : StoryRepository {
        override suspend fun createCharacter(input: CharacterInput) = Character("character", input)
        override fun observeCharacters() = flowOf(emptyList<Character>())
        override suspend fun startSession(characterId: String) = session
        override fun observeSessions() = flowOf(listOf(session))
        override suspend fun readSession(sessionId: String) = session
        override fun observeTurns(sessionId: String) = flowOf(
            listOf(
                Turn("greeting", sessionId, 0, TurnRole.Assistant, "Hello")
            )
        )
    }

    @Test fun collectingStoryNeverStartsGeneration() = runTest {
        val store = CoordinatorStore()
        val coordinator = GenerationCoordinator(
            store,
            GenerationProvider { flowOf("reply") },
            backgroundScope,
            StandardTestDispatcher(testScheduler),
            { testScheduler.currentTime }
        )
        val model = StoryViewModel("session", repository(), store, coordinator, backgroundScope)
        runCurrent()
        assertEquals("Hello", model.state.value.turns.single().text)
        assertEquals(0, store.admissions)
    }

    @Test fun invalidInputShowsActionableErrorWithoutAdmission() = runTest {
        val store = CoordinatorStore()
        val coordinator = GenerationCoordinator(
            store,
            GenerationProvider { flowOf("reply") },
            backgroundScope,
            StandardTestDispatcher(testScheduler),
            { testScheduler.currentTime }
        )
        coordinator.onForeground()
        val model = StoryViewModel("session", repository(), store, coordinator, backgroundScope)
        assertFalse(model.send(" "))
        assertFalse(model.send("x".repeat(16001)))
        runCurrent()
        assertNotNull(model.state.value.error)
        assertEquals(0, store.admissions)
    }

    @Test fun terminalDraftIsSeparateAndDiscardDoesNotRemoveDialogue() = runTest {
        val store = CoordinatorStore()
        val coordinator = GenerationCoordinator(
            store,
            GenerationProvider { flowOf("reply") },
            backgroundScope,
            StandardTestDispatcher(testScheduler),
            { testScheduler.currentTime }
        )
        coordinator.onForeground()
        val model = StoryViewModel("session", repository(), store, coordinator, backgroundScope)
        coordinator.send("session", 0, "nonce", "user")
        coordinator.stop()
        store.latest.value = store.latest.value!!.copy(draft = "uncertain draft")
        runCurrent()
        assertEquals("uncertain draft", model.state.value.draft)
        assertEquals(1, model.state.value.turns.size)
        model.discardDraft(store.latest.value!!.snapshot.operationId)
        runCurrent()
        assertEquals("", model.state.value.draft)
        assertEquals("Hello", model.state.value.turns.single().text)
    }
}
