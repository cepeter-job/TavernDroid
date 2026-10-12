package io.github.cepeterjob.taverndroid.settings

import androidx.lifecycle.ViewModel
import io.github.cepeterjob.taverndroid.engine.PreferencesRepository
import io.github.cepeterjob.taverndroid.engine.ThemeChoice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(private val preferences: PreferencesRepository) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)
    val error = message.asStateFlow()
    suspend fun setTheme(choice: ThemeChoice): Boolean = try {
        preferences.setTheme(choice)
        message.value = null
        true
    } catch (failure: Exception) {
        if (failure is CancellationException) throw failure
        message.value = "Could not save appearance. Try again."
        false
    }
}
