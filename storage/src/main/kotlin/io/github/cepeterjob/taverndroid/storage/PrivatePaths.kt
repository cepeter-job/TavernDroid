package io.github.cepeterjob.taverndroid.storage

import android.content.Context
import java.io.File
import java.security.MessageDigest

class PrivatePaths(private val context: Context) {
    fun credentialDirectory(namespace: String): File =
        File(context.noBackupFilesDir, "credentials/${digest(namespace)}").also {
            check(it.isDirectory || it.mkdirs()) { "Private storage unavailable" }
        }

    fun credentialFile(namespace: String, profileId: String): File =
        File(credentialDirectory(namespace), "${digest(profileId)}.bin")

    val assets: File
        get() = File(context.filesDir, "assets").also {
            check(it.isDirectory || it.mkdirs()) { "Private storage unavailable" }
        }

    private fun digest(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
