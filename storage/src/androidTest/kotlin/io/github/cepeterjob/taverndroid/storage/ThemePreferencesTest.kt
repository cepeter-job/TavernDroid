package io.github.cepeterjob.taverndroid.storage

import androidx.test.platform.app.InstrumentationRegistry
import io.github.cepeterjob.taverndroid.engine.ThemeChoice
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferencesTest {
    @Test fun defaultsToSystemAndPersistsAcrossReopen() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val filename = "synthetic-theme-" + UUID.randomUUID()
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val first = ThemePreferences(context, firstScope, filename)
            assertEquals(ThemeChoice.System, first.theme.first())
            first.setTheme(ThemeChoice.Dark)
            assertEquals(ThemeChoice.Dark, first.theme.first())
            firstScope.cancel()
            firstScope.coroutineContext[Job]!!.join()
            val second = ThemePreferences(context, secondScope, filename)
            assertEquals(ThemeChoice.Dark, second.theme.first())
            second.setTheme(ThemeChoice.Light)
            assertEquals(ThemeChoice.Light, second.theme.first())
        } finally {
            firstScope.cancel()
            secondScope.cancel()
            firstScope.coroutineContext[Job]!!.join()
            secondScope.coroutineContext[Job]!!.join()
            context.filesDir.resolve("datastore/$filename.preferences_pb").delete()
        }
    }
}
