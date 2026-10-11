package io.github.cepeterjob.taverndroid

import androidx.lifecycle.ViewModelStore
import io.github.cepeterjob.taverndroid.characters.CharactersViewModel
import io.github.cepeterjob.taverndroid.engine.Character
import io.github.cepeterjob.taverndroid.engine.CharacterInput
import io.github.cepeterjob.taverndroid.engine.PreferencesRepository
import io.github.cepeterjob.taverndroid.engine.SessionIdentity
import io.github.cepeterjob.taverndroid.engine.SessionSnapshot
import io.github.cepeterjob.taverndroid.engine.StoryRepository
import io.github.cepeterjob.taverndroid.engine.ThemeChoice
import io.github.cepeterjob.taverndroid.engine.Turn
import io.github.cepeterjob.taverndroid.settings.SettingsViewModel
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersistenceCommandTest {
    private val models = ViewModelStore()

    @Before fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After fun cleanup() {
        models.clear()
        Dispatchers.resetMain()
    }

    @Test fun failedStartKeepsScreenAndOffersRetryThenClearsError() = runTest {
        val repository = CommandRepository()
        val model = CharactersViewModel(repository)
        models.put("characters", model)
        repository.failure = IOException("synthetic storage failure")
        assertNull(model.startStory("character"))
        assertEquals("Could not start story. Try again.", model.error.value)
        repository.failure = null
        assertEquals("session", model.startStory("character"))
        assertNull(model.error.value)
    }

    @Test fun startRethrowsCancellation() = runTest {
        val repository = CommandRepository()
        val model = CharactersViewModel(repository)
        models.put("characters", model)
        repository.failure = CancellationException("synthetic cancellation")
        var cancelled = false
        try {
            model.startStory("character")
        } catch (_: CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
        assertNull(model.error.value)
    }

    @Test fun failedThemePreservesSelectionAndOffersRetryThenClearsError() = runTest {
        val preferences = CommandPreferences()
        val model = SettingsViewModel(preferences)
        models.put("settings", model)
        preferences.failure = IOException("synthetic storage failure")
        assertFalse(model.setTheme(ThemeChoice.Dark))
        assertEquals(ThemeChoice.System, preferences.theme.value)
        assertEquals("Could not save appearance. Try again.", model.error.value)
        preferences.failure = null
        assertTrue(model.setTheme(ThemeChoice.Dark))
        assertEquals(ThemeChoice.Dark, preferences.theme.value)
        assertNull(model.error.value)
    }

    @Test fun themeRethrowsCancellation() = runTest {
        val preferences = CommandPreferences()
        val model = SettingsViewModel(preferences)
        models.put("settings", model)
        preferences.failure = CancellationException("synthetic cancellation")
        var cancelled = false
        try {
            model.setTheme(ThemeChoice.Dark)
        } catch (
            _: CancellationException
        ) {
            cancelled = true
        }
        assertTrue(cancelled)
        assertNull(model.error.value)
    }

    private class CommandRepository : StoryRepository {
        var failure: Exception? = null
        override suspend fun createCharacter(input: CharacterInput) = Character("character", input)
        override fun observeCharacters() = flowOf(emptyList<Character>())
        override suspend fun startSession(characterId: String): SessionSnapshot {
            failure?.let { throw it }
            return SessionSnapshot(
                SessionIdentity("session", "branch", 0, 0),
                CharacterInput("Ada")
            )
        }
        override fun observeSessions() = flowOf(emptyList<SessionSnapshot>())
        override suspend fun readSession(sessionId: String): SessionSnapshot? = null
        override fun observeTurns(sessionId: String) = flowOf(emptyList<Turn>())
    }
    private class CommandPreferences : PreferencesRepository {
        var failure: Exception? = null
        override val theme = MutableStateFlow(ThemeChoice.System)
        override suspend fun setTheme(choice: ThemeChoice) {
            failure?.let { throw it }
            theme.value = choice
        }
    }
}
