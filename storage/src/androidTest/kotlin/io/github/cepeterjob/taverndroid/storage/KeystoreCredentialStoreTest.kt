package io.github.cepeterjob.taverndroid.storage

import android.content.Context
import android.util.AtomicFile
import androidx.test.platform.app.InstrumentationRegistry
import io.github.cepeterjob.taverndroid.engine.CredentialRead
import io.github.cepeterjob.taverndroid.engine.ReentryReason
import java.security.KeyStore
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KeystoreCredentialStoreTest {
    private lateinit var context: Context
    private lateinit var alias: String
    private lateinit var credentials: KeystoreCredentialStore
    private lateinit var paths: PrivatePaths

    @Before fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        alias = "synthetic-" + UUID.randomUUID()
        paths = PrivatePaths(context)
        credentials = KeystoreCredentialStore(context, alias)
    }

    @After fun cleanup() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keyStore.deleteEntry(alias)
        val directory = runCatching { paths.credentialDirectory(alias) }.getOrNull()
        if (directory != null &&
            directory.canonicalPath.startsWith(context.noBackupFilesDir.canonicalPath + "/")
        ) {
            directory.deleteRecursively()
        }
    }

    @Test fun syntheticCredentialCanBeReplacedAndDeleted() = runTest {
        assertEquals(CredentialRead.Absent, credentials.read("test-profile"))
        credentials.put("test-profile", "synthetic-canary-1")
        assertEquals(
            CredentialRead.Available("synthetic-canary-1"),
            credentials.read("test-profile")
        )
        credentials.put("test-profile", "synthetic-canary-2")
        assertEquals(
            CredentialRead.Available("synthetic-canary-2"),
            credentials.read("test-profile")
        )
        credentials.delete("test-profile")
        assertEquals(CredentialRead.Absent, credentials.read("test-profile"))
    }

    @Test fun ciphertextIsPrivateAndUsesFreshRandomization() = runTest {
        val secret = "synthetic-canary"
        credentials.put("profile", secret)
        val file = paths.credentialFile(alias, "profile")
        val first = file.readBytes()
        assertFalse(first.toString(Charsets.UTF_8).contains(secret))
        credentials.put("profile", secret)
        assertFalse(first.contentEquals(file.readBytes()))
        assertTrue(file.canonicalPath.startsWith(context.noBackupFilesDir.canonicalPath + "/"))
    }

    @Test fun tamperingRequiresReentryWithoutReplacingCiphertext() = runTest {
        credentials.put("profile", "synthetic-canary")
        val file = paths.credentialFile(alias, "profile")
        val corrupted = file.readBytes().apply {
            this[lastIndex] =
                (this[lastIndex].toInt() xor 1).toByte()
        }
        file.writeBytes(corrupted)
        assertEquals(
            CredentialRead.NeedsReentry(ReentryReason.CorruptCiphertext),
            credentials.read("profile")
        )
        assertArrayEquals(corrupted, file.readBytes())
    }

    @Test fun missingKeyRequiresExplicitReplacement() = runTest {
        credentials.put("profile", "synthetic-canary")
        val file = paths.credentialFile(alias, "profile")
        val previous = file.readBytes()
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
            deleteEntry(alias)
        }
        assertEquals(
            CredentialRead.NeedsReentry(ReentryReason.UnavailableKey),
            credentials.read("profile")
        )
        assertArrayEquals(previous, file.readBytes())
        credentials.put("profile", "synthetic-new-canary")
        assertEquals(CredentialRead.Available("synthetic-new-canary"), credentials.read("profile"))
    }

    @Test fun swappedProfileCiphertextCannotBeDecrypted() = runTest {
        credentials.put("first", "synthetic-one")
        credentials.put("second", "synthetic-two")
        paths.credentialFile(
            alias,
            "second"
        ).writeBytes(paths.credentialFile(alias, "first").readBytes())
        assertEquals(
            CredentialRead.NeedsReentry(ReentryReason.CorruptCiphertext),
            credentials.read("second")
        )
    }

    @Test fun profileNameCannotEscapePrivateDirectory() = runTest {
        val profile = "../../synthetic-escape"
        credentials.put(profile, "synthetic-canary")
        val file = paths.credentialFile(alias, profile)
        assertEquals(
            paths.credentialDirectory(alias).canonicalPath,
            file.parentFile!!.canonicalPath
        )
        assertEquals(CredentialRead.Available("synthetic-canary"), credentials.read(profile))
    }

    @Test fun interruptedReplacementKeepsLastCommittedCredential() = runTest {
        credentials.put("profile", "synthetic-committed")
        val atomic = AtomicFile(paths.credentialFile(alias, "profile"))
        val unfinished = atomic.startWrite()
        try {
            unfinished.write(byteArrayOf(0, 1, 2))
            unfinished.flush()
            // Close the descriptor without finishWrite/failWrite, as process death would.
            unfinished.close()
            val reopened = KeystoreCredentialStore(context, alias)
            assertEquals(CredentialRead.Available("synthetic-committed"), reopened.read("profile"))
        } finally {
            atomic.failWrite(unfinished)
        }
    }

    @Test fun legacyBackupWithoutBaseRecoversCommittedCredential() = runTest {
        credentials.put("profile", "synthetic-committed")
        val file = paths.credentialFile(alias, "profile")
        val backup = java.io.File(file.path + ".bak")
        check(file.renameTo(backup))
        val reopened = KeystoreCredentialStore(context, alias)
        assertEquals(CredentialRead.Available("synthetic-committed"), reopened.read("profile"))
        assertEquals(CredentialRead.Available("synthetic-committed"), reopened.read("profile"))
    }
}
