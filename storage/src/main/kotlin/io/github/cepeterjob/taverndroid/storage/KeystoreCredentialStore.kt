package io.github.cepeterjob.taverndroid.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import io.github.cepeterjob.taverndroid.engine.CredentialRead
import io.github.cepeterjob.taverndroid.engine.CredentialStore
import io.github.cepeterjob.taverndroid.engine.ReentryReason
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class KeystoreCredentialStore(
    context: Context,
    private val keyAlias: String = "taverndroid.credentials.v1",
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : CredentialStore {
    private val paths = PrivatePaths(context)
    private val mutex = Mutex()

    override suspend fun put(profileId: String, secret: String) = withContext(dispatcher) {
        mutex.withLock {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            var key = try {
                readKey()
            } catch (_: GeneralSecurityException) {
                null
            }
            try {
                cipher.init(Cipher.ENCRYPT_MODE, key ?: createKey())
            } catch (_: GeneralSecurityException) {
                // Only an explicit write can replace an unavailable key.
                keyStore().deleteEntry(keyAlias)
                key = createKey()
                cipher.init(Cipher.ENCRYPT_MODE, key)
            }
            cipher.updateAAD(profileId.toByteArray(Charsets.UTF_8))
            val encrypted = cipher.doFinal(secret.toByteArray(Charsets.UTF_8))
            val blob = byteArrayOf(84, 68, 67, 1, cipher.iv.size.toByte()) + cipher.iv + encrypted
            val file = AtomicFile(paths.credentialFile(keyAlias, profileId))
            val output = file.startWrite()
            try {
                output.write(blob)
                file.finishWrite(output)
            } catch (failure: Exception) {
                file.failWrite(output)
                throw failure
            }
        }
    }

    override suspend fun read(profileId: String): CredentialRead = withContext(dispatcher) {
        mutex.withLock {
            val file = paths.credentialFile(keyAlias, profileId)
            if (!file.exists()) return@withLock CredentialRead.Absent
            val key = try {
                readKey()
            } catch (_: GeneralSecurityException) {
                null
            }
                ?: return@withLock CredentialRead.NeedsReentry(ReentryReason.UnavailableKey)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val blob = file.readBytes()
            if (blob.size < 33 ||
                !blob.take(4).toByteArray().contentEquals(byteArrayOf(84, 68, 67, 1)) ||
                blob[4].toInt() != 12
            ) {
                return@withLock CredentialRead.NeedsReentry(ReentryReason.CorruptCiphertext)
            }
            try {
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    GCMParameterSpec(128, blob.copyOfRange(5, 17))
                )
            } catch (_: GeneralSecurityException) {
                return@withLock CredentialRead.NeedsReentry(ReentryReason.UnavailableKey)
            }
            try {
                cipher.updateAAD(profileId.toByteArray(Charsets.UTF_8))
                CredentialRead.Available(
                    String(cipher.doFinal(blob.copyOfRange(17, blob.size)), Charsets.UTF_8)
                )
            } catch (_: GeneralSecurityException) {
                CredentialRead.NeedsReentry(ReentryReason.CorruptCiphertext)
            }
        }
    }

    override suspend fun delete(profileId: String) = withContext(dispatcher) {
        mutex.withLock { AtomicFile(paths.credentialFile(keyAlias, profileId)).delete() }
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private fun readKey(): SecretKey? = keyStore().getKey(keyAlias, null) as? SecretKey
    private fun createKey(): SecretKey = KeyGenerator.getInstance("AES", "AndroidKeyStore").apply {
        init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
    }.generateKey()
}
