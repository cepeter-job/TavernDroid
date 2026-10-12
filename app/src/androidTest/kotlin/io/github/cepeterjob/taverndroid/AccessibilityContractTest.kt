package io.github.cepeterjob.taverndroid

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import io.github.cepeterjob.taverndroid.engine.CharacterInput
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class AccessibilityContractTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun navigationAndCharacterControlsHaveLabelsAndMinimumTargets() {
        compose.onNodeWithText("Characters").assertHasClickAction().performClick()
        compose.onNodeWithTag("character-name").assertIsDisplayed()
        compose.onNodeWithText("Save character").performScrollTo()
            .assertHeightIsAtLeast(48.dp).assertHasClickAction()
    }

    @Test fun messageAndSendRemainReachableAndBlankInputCannotSend() {
        val container = (compose.activity.application as TavernDroidApplication).container
        val id = runBlocking {
            val character = container.repository.createCharacter(
                CharacterInput("Accessibility synthetic")
            )
            container.repository.startSession(character.id).identity.sessionId
        }
        compose.waitUntil(10000) {
            compose.onAllNodes(
                androidx.compose.ui.test.hasTestTag("open-$id")
            ).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("open-$id").performScrollTo().performClick()
        compose.onNodeWithTag("message").assertIsDisplayed()
        compose.onNodeWithText("Send").assertIsNotEnabled().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("message").performTextInput("Synthetic keyboard reachability")
        compose.onNodeWithTag("message").assertIsDisplayed()
        compose.onNodeWithText("Send").assertIsDisplayed()
    }
}
