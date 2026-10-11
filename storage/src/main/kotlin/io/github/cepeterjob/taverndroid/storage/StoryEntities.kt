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
