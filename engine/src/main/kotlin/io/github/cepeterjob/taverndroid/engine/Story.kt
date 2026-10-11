package io.github.cepeterjob.taverndroid.engine

data class SessionIdentity(
    val sessionId: String,
    val branchId: String,
    val incarnation: Long,
    val revision: Long
)

data class SessionSnapshot(val identity: SessionIdentity, val character: CharacterInput)

enum class TurnRole { User, Assistant }

data class Turn(
    val id: String,
    val sessionId: String,
    val ordinal: Long,
    val role: TurnRole,
    val text: String,
    val requestId: String? = null
)
