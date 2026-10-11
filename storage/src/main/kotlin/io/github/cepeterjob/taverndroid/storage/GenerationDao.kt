package io.github.cepeterjob.taverndroid.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GenerationDao {
    @Insert suspend fun insert(operation: GenerationEntity)

    @Query("SELECT * FROM generation_operations WHERE operationId = :id")
    suspend fun operation(id: String): GenerationEntity?

    @Query("SELECT * FROM generation_operations WHERE clientNonce = :nonce")
    suspend fun byNonce(nonce: String): GenerationEntity?

    @Query(
        "SELECT * FROM generation_operations WHERE sessionId = :sessionId ORDER BY createdOrder DESC LIMIT 1"
    )
    suspend fun latest(sessionId: String): GenerationEntity?

    @Query(
        "SELECT * FROM generation_operations WHERE sessionId = :sessionId ORDER BY createdOrder DESC LIMIT 1"
    )
    fun observeLatest(sessionId: String): Flow<GenerationEntity?>

    @Query(
        "SELECT COUNT(*) FROM generation_operations WHERE state IN ('Queued','Preparing','Streaming','Committing')"
    )
    suspend fun activeCount(): Int

    @Query("SELECT COALESCE(MAX(createdOrder), 0) + 1 FROM generation_operations")
    suspend fun nextOrder(): Long

    @Query(
        "UPDATE generation_operations SET state = :toState WHERE operationId = :id AND state = :fromState"
    )
    suspend fun transition(id: String, fromState: String, toState: String): Int

    @Query("UPDATE generation_operations SET draft = :draft WHERE operationId = :id")
    suspend fun draft(id: String, draft: String): Int

    @Query(
        "UPDATE generation_operations SET state = :state, draft = :draft, failure = :failure WHERE operationId = :id"
    )
    suspend fun finish(id: String, state: String, draft: String, failure: String?): Int

    @Query(
        "UPDATE generation_operations SET state = 'Interrupted', failure = NULL WHERE state IN ('Queued','Preparing','Streaming','Committing')"
    )
    suspend fun recover(): Int
}
