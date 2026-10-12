package io.github.cepeterjob.taverndroid

import android.content.Context
import androidx.room.Room
import io.github.cepeterjob.taverndroid.engine.GenerationProvider
import io.github.cepeterjob.taverndroid.storage.KeystoreCredentialStore
import io.github.cepeterjob.taverndroid.storage.RoomGenerationStore
import io.github.cepeterjob.taverndroid.storage.RoomStoryRepository
import io.github.cepeterjob.taverndroid.storage.TavernDatabase
import io.github.cepeterjob.taverndroid.storage.ThemePreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(
    context: Context,
    provider: GenerationProvider,
    dispatcher: CoroutineDispatcher,
    clock: () -> Long
) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val database: TavernDatabase = Room.databaseBuilder(
        context.applicationContext,
        TavernDatabase::class.java,
        "tavern.db"
    ).build()
    val repository = RoomStoryRepository(database)
    val generations = RoomGenerationStore(database)
    val preferences = ThemePreferences(context.applicationContext, scope)
    val credentials = KeystoreCredentialStore(context.applicationContext)
    val coordinator = GenerationCoordinator(
        generations,
        provider,
        scope,
        dispatcher,
        clock,
        Diagnostics { android.util.Log.i("TavernDroid", it) }
    )
}
