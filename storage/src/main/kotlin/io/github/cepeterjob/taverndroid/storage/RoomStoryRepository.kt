package io.github.cepeterjob.taverndroid.storage

import androidx.room.withTransaction
import io.github.cepeterjob.taverndroid.engine.Character
import io.github.cepeterjob.taverndroid.engine.CharacterInput
import io.github.cepeterjob.taverndroid.engine.SessionIdentity
import io.github.cepeterjob.taverndroid.engine.SessionSnapshot
import io.github.cepeterjob.taverndroid.engine.StoryRepository
import io.github.cepeterjob.taverndroid.engine.Turn
import io.github.cepeterjob.taverndroid.engine.TurnRole
import io.github.cepeterjob.taverndroid.engine.normalizeCharacter
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomStoryRepository(private val database: TavernDatabase) : StoryRepository {
    private val dao = database.storyDao()

    override suspend fun createCharacter(input: CharacterInput): Character {
        val normalized = normalizeCharacter(input)
        val character = Character(UUID.randomUUID().toString(), normalized)
        dao.insertCharacter(
            CharacterEntity(
                character.id,
                normalized.name,
                normalized.description,
                normalized.greeting
            )
        )
        return character
    }

    override fun observeCharacters(): Flow<List<Character>> = dao.characters().map { rows ->
        rows.map { Character(it.id, CharacterInput(it.name, it.description, it.greeting)) }
    }

    override suspend fun startSession(characterId: String): SessionSnapshot =
        database.withTransaction {
            val character = requireNotNull(dao.character(characterId)) { "Character not found" }
            val session = SessionEntity(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                0,
                0,
                character.name,
                character.description,
                character.greeting,
                System.currentTimeMillis()
            )
            dao.insertSession(session)
            if (character.greeting.isNotEmpty()) {
                dao.insertTurn(
                    TurnEntity(
                        UUID.randomUUID().toString(),
                        session.id,
                        0,
                        TurnRole.Assistant.name,
                        character.greeting
                    )
                )
            }
            session.toSnapshot()
        }

    override fun observeSessions(): Flow<List<SessionSnapshot>> = dao.sessions().map { rows ->
        rows.map { it.toSnapshot() }
    }

    override suspend fun readSession(sessionId: String): SessionSnapshot? =
        dao.session(sessionId)?.toSnapshot()

    override fun observeTurns(sessionId: String): Flow<List<Turn>> =
        dao.turns(sessionId).map { rows ->
            rows.map { it.toTurn() }
        }
}

internal fun SessionEntity.toSnapshot(): SessionSnapshot = SessionSnapshot(
    SessionIdentity(id, branchId, incarnation, revision),
    CharacterInput(characterName, characterDescription, characterGreeting)
)

internal fun TurnEntity.toTurn(): Turn =
    Turn(id, sessionId, ordinal, TurnRole.valueOf(role), text, requestId)
