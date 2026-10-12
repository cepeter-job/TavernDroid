package io.github.cepeterjob.taverndroid.storage

import io.github.cepeterjob.taverndroid.engine.CharacterInput
import io.github.cepeterjob.taverndroid.engine.FakeProviderConfig
import io.github.cepeterjob.taverndroid.engine.OperationSnapshot
import io.github.cepeterjob.taverndroid.engine.SessionIdentity
import io.github.cepeterjob.taverndroid.engine.Turn
import io.github.cepeterjob.taverndroid.engine.TurnRole
import org.json.JSONArray
import org.json.JSONObject

internal fun encodeHistory(turns: List<Turn>): String = JSONArray().apply {
    turns.forEach { turn ->
        put(
            JSONObject().put(
                "id",
                turn.id
            ).put("sessionId", turn.sessionId).put("ordinal", turn.ordinal)
                .put("role", turn.role.name).put("text", turn.text).put(
                    "requestId",
                    turn.requestId ?: JSONObject.NULL
                )
        )
    }
}.toString()

internal fun GenerationEntity.snapshot(): OperationSnapshot {
    val history = JSONArray(historyJson)
    return OperationSnapshot(
        operationId, requestId, attemptNumber, userTurnId,
        SessionIdentity(sessionId, branchId, incarnation, revision),
        CharacterInput(characterName, characterDescription, characterGreeting),
        (0 until history.length()).map { index ->
            val row = history.getJSONObject(index)
            Turn(
                row.getString("id"),
                row.getString("sessionId"),
                row.getLong("ordinal"),
                TurnRole.valueOf(row.getString("role")),
                row.getString("text"),
                if (row.isNull("requestId")) null else row.getString("requestId")
            )
        },
        userText, FakeProviderConfig(chunkCodeUnits, chunkDelayMillis)
    )
}
