package io.github.cepeterjob.taverndroid.engine

import kotlinx.coroutines.flow.Flow

enum class ThemeChoice { System, Light, Dark }

interface PreferencesRepository {
    val theme: Flow<ThemeChoice>
    suspend fun setTheme(choice: ThemeChoice)
}
