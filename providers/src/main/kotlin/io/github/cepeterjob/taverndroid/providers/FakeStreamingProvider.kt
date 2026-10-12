package io.github.cepeterjob.taverndroid.providers

import io.github.cepeterjob.taverndroid.engine.GenerationProvider
import io.github.cepeterjob.taverndroid.engine.OperationSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeStreamingProvider : GenerationProvider {
    override fun stream(snapshot: OperationSnapshot): Flow<String> = flow {
        val reply = "Synthetic reply: " + snapshot.userText
        for (chunk in reply.chunked(snapshot.config.chunkCodeUnits)) {
            delay(snapshot.config.chunkDelayMillis)
            emit(chunk)
        }
    }
}
