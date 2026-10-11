package io.github.cepeterjob.taverndroid.engine

import org.junit.Assert.assertFalse
import org.junit.Test

class CredentialReadTest {
    @Test fun availableValueDoesNotExposeSecretInDiagnostics() {
        assertFalse(
            CredentialRead.Available(
                "synthetic-secret-canary"
            ).toString().contains("synthetic-secret-canary")
        )
    }
}
