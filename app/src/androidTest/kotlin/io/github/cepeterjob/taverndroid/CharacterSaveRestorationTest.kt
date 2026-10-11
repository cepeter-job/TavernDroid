package io.github.cepeterjob.taverndroid

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.platform.app.InstrumentationRegistry
import io.github.cepeterjob.taverndroid.characters.CharactersScreen
import io.github.cepeterjob.taverndroid.characters.CharactersViewModel
import io.github.cepeterjob.taverndroid.engine.Character
import io.github.cepeterjob.taverndroid.engine.CharacterInput
import io.github.cepeterjob.taverndroid.engine.StoryRepository
import kotlinx.coroutines.awaitCancellation
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class CharacterSaveRestorationTest {
    @Test fun restoringFormDoesNotKeepAnAbandonedSaveDisabled() = runComposeUiTest {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
            .applicationContext as TavernDroidApplication
        val gated = object : StoryRepository by app.container.repository {
            override suspend fun createCharacter(input: CharacterInput): Character =
                awaitCancellation()
        }
        val model = CharactersViewModel(gated)
        val restoration = StateRestorationTester(this)
        restoration.setContent { CharactersScreen(model) {} }
        onNodeWithTag("character-name").performTextInput("Synthetic restored form")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        onNodeWithText("Save character").performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        onNodeWithText("Save character").assertIsNotEnabled()
        restoration.emulateSaveAndRestore()
        onNodeWithText("Save character").performScrollTo().assertIsEnabled()
    }
}
