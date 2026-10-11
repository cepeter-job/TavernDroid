package io.github.cepeterjob.taverndroid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.cepeterjob.taverndroid.characters.CharactersScreen
import io.github.cepeterjob.taverndroid.characters.CharactersViewModel
import io.github.cepeterjob.taverndroid.engine.ThemeChoice
import io.github.cepeterjob.taverndroid.library.LibraryScreen
import io.github.cepeterjob.taverndroid.settings.SettingsScreen
import io.github.cepeterjob.taverndroid.stories.StoriesScreen
import io.github.cepeterjob.taverndroid.stories.StoryScreen
import io.github.cepeterjob.taverndroid.stories.StoryViewModel
import io.github.cepeterjob.taverndroid.theme.TavernTheme
import kotlinx.coroutines.launch

@Composable
fun TavernNavigation(container: AppContainer) {
    val focus = LocalFocusManager.current
    val theme by container.preferences.theme.collectAsStateWithLifecycle(ThemeChoice.System)
    val sessions by container.repository.observeSessions().collectAsStateWithLifecycle(emptyList())
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val scope = rememberCoroutineScope()
    TavernTheme(theme) {
        Scaffold(contentWindowInsets = WindowInsets.safeDrawing, bottomBar = {
            if (entry?.destination?.route != "story/{id}") {
                NavigationBar {
                    listOf("Stories", "Characters", "Library", "Settings").forEach { label ->
                        val route = label.lowercase()
                        NavigationBarItem(
                            selected = entry?.destination?.route == route,
                            onClick = {
                                nav.navigate(route) {
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text(label.take(1)) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }) { padding ->
            Box(Modifier.padding(padding).consumeWindowInsets(padding).imePadding()) {
                NavHost(nav, startDestination = "stories") {
                    composable("stories") {
                        StoriesScreen(sessions, {
                            nav.navigate("story/$it")
                        }, { nav.navigate("characters") })
                    }
                    composable("characters") {
                        val model: CharactersViewModel = viewModel(
                            factory = viewModelFactory {
                                initializer { CharactersViewModel(container.repository) }
                            }
                        )
                        CharactersScreen(model) { id ->
                            focus.clearFocus()
                            scope.launch {
                                val session = container.repository.startSession(id)
                                nav.navigate("story/${session.identity.sessionId}")
                            }
                        }
                    }
                    composable("story/{id}") { backStack ->
                        val id = requireNotNull(backStack.arguments?.getString("id"))
                        val model: StoryViewModel = viewModel(
                            factory = viewModelFactory {
                                initializer {
                                    StoryViewModel(
                                        id,
                                        container.repository,
                                        container.generations,
                                        container.coordinator
                                    )
                                }
                            }
                        )
                        StoryScreen(model) { nav.popBackStack("stories", false) }
                    }
                    composable("library") { LibraryScreen() }
                    composable("settings") {
                        SettingsScreen(theme) {
                            scope.launch { container.preferences.setTheme(it) }
                        }
                    }
                }
            }
        }
    }
}
