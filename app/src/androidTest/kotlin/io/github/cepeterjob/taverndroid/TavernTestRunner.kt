package io.github.cepeterjob.taverndroid

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.test.runner.AndroidJUnitRunner
import io.github.cepeterjob.taverndroid.engine.GenerationProvider
import io.github.cepeterjob.taverndroid.providers.FakeStreamingProvider
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow

class TavernTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application =
        super.newApplication(cl, TestApplication::class.java.name, context)
}

class TestApplication : TavernDroidApplication() {
    override fun createContainer(): AppContainer = AppContainer(
        this,
        GenerationProvider { snapshot ->
            flow {
                invocations.incrementAndGet()
                if (holdStream) {
                    emit("synthetic partial draft")
                    awaitCancellation()
                } else {
                    FakeStreamingProvider().stream(snapshot).collect { emit(it) }
                }
            }
        },
        Dispatchers.Default,
        SystemClock::elapsedRealtime
    )
    companion object {
        val invocations = AtomicInteger()

        @Volatile var holdStream = false
    }
}
