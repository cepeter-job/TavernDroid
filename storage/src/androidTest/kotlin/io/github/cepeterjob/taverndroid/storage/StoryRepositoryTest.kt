package io.github.cepeterjob.taverndroid.storage

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import io.github.cepeterjob.taverndroid.engine.CharacterInput
import io.github.cepeterjob.taverndroid.engine.InputValidationException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class StoryRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: TavernDatabase
    private lateinit var repository: RoomStoryRepository
    private lateinit var filename: String

    @Before fun open() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        filename = "synthetic-story-" + UUID.randomUUID() + ".db"
        reopen()
    }
    private fun reopen() {
        database = Room.databaseBuilder(context, TavernDatabase::class.java, filename).build()
        repository = RoomStoryRepository(database)
    }

    @After fun close() {
        database.close()
        context.deleteDatabase(filename)
    }

    @Test fun createPersistsNormalizedCharacter() = runTest {
        val character = repository.createCharacter(CharacterInput("  Ada  ", "Description", "Hi"))
        assertEquals("Ada", character.content.name)
        assertEquals(listOf(character), repository.observeCharacters().first())
    }

    @Test fun invalidCharacterIsNotPersisted() = runTest {
        try {
            repository.createCharacter(CharacterInput(" "))
            fail("Blank name accepted")
        } catch (expected: InputValidationException) {
            assertEquals("name", expected.field)
        }
        assertTrue(repository.observeCharacters().first().isEmpty())
    }

    @Test fun snapshotsStayIsolatedAndGreetingSurvivesReopenOnce() = runTest {
        val ada = repository.createCharacter(CharacterInput("Ada", "Before", "Hello Ada"))
        val bob = repository.createCharacter(CharacterInput("Bob", "Other", "Hello Bob"))
        val first = repository.startSession(ada.id)
        val second = repository.startSession(bob.id)
        database.openHelper.writableDatabase.execSQL(
            "UPDATE characters SET description = ? WHERE id = ?",
            arrayOf("After", ada.id)
        )
        assertEquals(
            "Before",
            repository.readSession(first.identity.sessionId)!!.character.description
        )
        assertEquals(
            "Other",
            repository.readSession(second.identity.sessionId)!!.character.description
        )
        database.close()
        reopen()
        assertEquals(
            listOf("Hello Ada"),
            repository.observeTurns(first.identity.sessionId).first().map {
                it.text
            }
        )
        assertEquals(
            listOf("Hello Bob"),
            repository.observeTurns(second.identity.sessionId).first().map {
                it.text
            }
        )
        assertEquals(2, repository.observeSessions().first().size)
        assertEquals(first, repository.readSession(first.identity.sessionId))
    }

    @Test fun emptyGreetingCreatesNoTurn() = runTest {
        val character = repository.createCharacter(CharacterInput("Ada"))
        val session = repository.startSession(character.id)
        assertTrue(repository.observeTurns(session.identity.sessionId).first().isEmpty())
    }

    @Test fun missingCharacterDoesNotCreateSession() = runTest {
        try {
            repository.startSession("missing")
            fail("Missing character accepted")
        } catch (expected: IllegalArgumentException) {
            assertTrue(repository.observeSessions().first().isEmpty())
        }
    }
}
