package io.github.cepeterjob.taverndroid.engine

import kotlinx.coroutines.flow.Flow

interface StoryRepository {
    suspend fun createCharacter(input: CharacterInput): Character
    fun observeCharacters(): Flow<List<Character>>
    suspend fun startSession(characterId: String): SessionSnapshot
    fun observeSessions(): Flow<List<SessionSnapshot>>
    suspend fun readSession(sessionId: String): SessionSnapshot?
    fun observeTurns(sessionId: String): Flow<List<Turn>>
}
