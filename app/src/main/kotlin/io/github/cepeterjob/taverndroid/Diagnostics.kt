package io.github.cepeterjob.taverndroid

import io.github.cepeterjob.taverndroid.engine.FailureCategory

enum class DiagnosticEvent {
    GenerationStarted,
    GenerationCompleted,
    GenerationCancelled,
    GenerationFailed,
    Recovery
}

class Diagnostics(private val emit: (String) -> Unit) {
    fun record(
        event: DiagnosticEvent,
        operationId: String? = null,
        category: FailureCategory? = null
    ) {
        val id = operationId?.takeIf {
            it.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
        }
        emit(listOfNotNull(event.name, id, category?.name).joinToString(" "))
    }
}
