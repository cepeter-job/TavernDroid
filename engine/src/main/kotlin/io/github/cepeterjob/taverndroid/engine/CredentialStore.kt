package io.github.cepeterjob.taverndroid.engine

interface CredentialStore {
    suspend fun put(profileId: String, secret: String)
    suspend fun read(profileId: String): CredentialRead
    suspend fun delete(profileId: String)
}

enum class ReentryReason { UnavailableKey, CorruptCiphertext }

sealed interface CredentialRead {
    data object Absent : CredentialRead
    data class Available(val secret: String) : CredentialRead {
        override fun toString(): String = "Available(redacted)"
    }
    data class NeedsReentry(val reason: ReentryReason) : CredentialRead
}
