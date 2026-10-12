package io.github.cepeterjob.taverndroid.storage

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val greeting: String
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val branchId: String,
    val incarnation: Long,
    val revision: Long,
    val characterName: String,
    val characterDescription: String,
    val characterGreeting: String,
    val createdAt: Long
)

@Entity(
    tableName = "turns",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            value = ["sessionId", "ordinal"],
            unique = true
        ), Index(
            value = ["requestId"],
            unique = true
        ), Index(value = ["operationId"], unique = true)
    ]
)
data class TurnEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val ordinal: Long,
    val role: String,
    val text: String,
    val requestId: String? = null,
    val operationId: String? = null
)

@Entity(
    tableName = "generation_operations",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TurnEntity::class,
            parentColumns = ["id"],
            childColumns = ["userTurnId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            value = ["sessionId"]
        ), Index(value = ["userTurnId"]), Index(value = ["clientNonce"], unique = true),
        Index(
            value = ["requestId", "attemptNumber"],
            unique = true
        ), Index(value = ["createdOrder"], unique = true)
    ]
)
data class GenerationEntity(
    @PrimaryKey val operationId: String,
    val requestId: String,
    val clientNonce: String,
    val attemptNumber: Int,
    val userTurnId: String,
    val sessionId: String,
    val branchId: String,
    val incarnation: Long,
    val revision: Long,
    val characterName: String,
    val characterDescription: String,
    val characterGreeting: String,
    val historyJson: String,
    val userText: String,
    val chunkCodeUnits: Int,
    val chunkDelayMillis: Long,
    val state: String,
    val draft: String,
    val failure: String?,
    val createdOrder: Long
)
