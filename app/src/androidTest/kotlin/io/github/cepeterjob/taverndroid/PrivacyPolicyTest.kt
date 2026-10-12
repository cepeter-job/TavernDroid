package io.github.cepeterjob.taverndroid

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.xmlpull.v1.XmlPullParser

class PrivacyPolicyTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val domains = setOf(
        "root", "file", "database", "sharedpref", "external",
        "device_root", "device_file", "device_database", "device_sharedpref"
    )

    @Test fun appDisablesBackupAndNetworkPermission() {
        assertEquals(0, context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
        val permissions = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        ).requestedPermissions.orEmpty()
        assertFalse(permissions.contains("android.permission.INTERNET"))
    }

    @Test fun legacyBackupRulesExcludePrivatePaths() {
        val id = context.resources.getIdentifier("backup_rules", "xml", context.packageName)
        assertTrue("Missing legacy backup resource", id != 0)
        assertEquals(domains, exclusions(id)["full-backup-content"])
    }

    @Test fun currentRulesExcludeCloudAndDeviceTransfer() {
        val id = context.resources.getIdentifier(
            "data_extraction_rules",
            "xml",
            context.packageName
        )
        assertTrue("Missing transfer rules resource", id != 0)
        val rules = exclusions(id)
        assertEquals(domains, rules["cloud-backup"])
        assertEquals(domains, rules["device-transfer"])
    }

    private fun exclusions(id: Int): Map<String, Set<String>> {
        val results = mutableMapOf<String, MutableSet<String>>()
        var parent = ""
        context.resources.getXml(id).use { parser ->
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    if (parser.name in
                        setOf("full-backup-content", "cloud-backup", "device-transfer")
                    ) {
                        parent = parser.name
                        results.getOrPut(parent) { mutableSetOf() }
                    } else if (parser.name == "exclude") {
                        assertEquals(".", parser.getAttributeValue(null, "path"))
                        results.getValue(parent).add(parser.getAttributeValue(null, "domain"))
                    }
                }
                parser.next()
            }
        }
        return results
    }
}
