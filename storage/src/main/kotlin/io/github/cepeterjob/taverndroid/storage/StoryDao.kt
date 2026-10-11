package io.github.cepeterjob.taverndroid.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {
    @Insert suspend fun insertCharacter(character: CharacterEntity)

    @Query("SELECT * FROM characters ORDER BY name, id")
    fun characters(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun character(id: String): CharacterEntity?

    @Insert suspend fun insertSession(session: SessionEntity)

    @Query("SELECT * FROM sessions ORDER BY createdAt DESC, id")
    fun sessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun session(id: String): SessionEntity?

    @Insert suspend fun insertTurn(turn: TurnEntity)

    @Query("SELECT * FROM turns WHERE sessionId = :sessionId ORDER BY ordinal")
    fun turns(sessionId: String): Flow<List<TurnEntity>>

    @Query("SELECT * FROM turns WHERE sessionId = :sessionId ORDER BY ordinal")
    suspend fun readTurns(sessionId: String): List<TurnEntity>

    @Query("UPDATE sessions SET revision = revision + 1 WHERE id = :id")
    suspend fun advanceRevision(id: String): Int

    @Query("SELECT * FROM turns WHERE id = :id")
    suspend fun turn(id: String): TurnEntity?
}
