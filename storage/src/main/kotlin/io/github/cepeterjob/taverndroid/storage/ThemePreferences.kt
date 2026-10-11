package io.github.cepeterjob.taverndroid.storage

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.cepeterjob.taverndroid.engine.PreferencesRepository
import io.github.cepeterjob.taverndroid.engine.ThemeChoice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ThemePreferences(context: Context, scope: CoroutineScope, filename: String = "taverndroid") :
    PreferencesRepository {
    private val key = stringPreferencesKey("theme")
    private val store = PreferenceDataStoreFactory.create(scope = scope) {
        context.preferencesDataStoreFile(filename)
    }

    override val theme: Flow<ThemeChoice> = store.data.map { preferences ->
        ThemeChoice.entries.firstOrNull { it.name == preferences[key] } ?: ThemeChoice.System
    }

    override suspend fun setTheme(choice: ThemeChoice) {
        store.edit { it[key] = choice.name }
    }
}
