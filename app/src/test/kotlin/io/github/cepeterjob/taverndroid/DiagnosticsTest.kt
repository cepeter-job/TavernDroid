package io.github.cepeterjob.taverndroid

import io.github.cepeterjob.taverndroid.engine.FailureCategory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsTest {
    @Test fun retainsClosedEventAndCategoryWithoutSensitiveId() {
        val messages = mutableListOf<String>()
        Diagnostics(
            messages::add
        ).record(
            DiagnosticEvent.GenerationFailed,
            "synthetic-secret-and-transcript",
            FailureCategory.Storage
        )
        assertTrue(messages.single().contains("GenerationFailed"))
        assertTrue(messages.single().contains("Storage"))
        assertFalse(messages.single().contains("synthetic-secret-and-transcript"))
        assertTrue(messages.single().length <= 120)
    }

    @Test fun recordsCanonicalOperationIdForCorrelation() {
        val messages = mutableListOf<String>()
        Diagnostics(
            messages::add
        ).record(DiagnosticEvent.GenerationStarted, "00000000-0000-0000-0000-000000000001")
        assertTrue(messages.single().contains("00000000-0000-0000-0000-000000000001"))
    }
}
