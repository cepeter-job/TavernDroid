package io.github.cepeterjob.taverndroid

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.github.cepeterjob.taverndroid.providers.FakeStreamingProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class TavernDroidApplication : Application() {
    lateinit var container: AppContainer
        private set

    protected open fun createContainer(): AppContainer = AppContainer(
        this,
        FakeStreamingProvider(),
        Dispatchers.Default,
        SystemClock::elapsedRealtime
    )

    override fun onCreate() {
        super.onCreate()
        container = createContainer()
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = container.coordinator.onForeground()
            override fun onStop(owner: LifecycleOwner) = container.coordinator.onBackground()
        })
        container.scope.launch {
            // Coordinator exposes a recovery action if startup persistence is unavailable.
            runCatching { container.coordinator.initialize() }
        }
    }
}
