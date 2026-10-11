package io.github.cepeterjob.taverndroid

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OfflineStoryFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun createSendReopenKeepsOneGreetingAndReply() {
        compose.onNodeWithText("Characters").performClick()
        compose.onNodeWithTag("character-name").performTextInput("Synthetic Ada")
        compose.onNodeWithTag("character-greeting").performTextInput("Welcome synthetic reader")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Save character").performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        val container = (compose.activity.application as TavernDroidApplication).container
        var characterId: String? = null
        compose.waitUntil(10000) {
            characterId = runBlocking {
                container.repository.observeCharacters().first()
                    .find { it.content.name == "Synthetic Ada" }?.id
            }
            characterId != null
        }
        compose.onNodeWithTag(
            "characters-list"
        ).performScrollToNode(hasTestTag("start-$characterId"))
        compose.onNodeWithTag("start-$characterId").performClick()
        compose.onNodeWithTag("dialogue").performScrollToNode(hasText("Welcome synthetic reader"))
        compose.onNodeWithText("Welcome synthetic reader").assertIsDisplayed()
        compose.onNodeWithTag("message").performTextInput("Hello")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Send", substring = false).assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.waitUntil(10000) {
            compose.onAllNodes(androidx.compose.ui.test.hasText("Synthetic reply: Hello"))
                .fetchSemanticsNodes().size == 1
        }
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("dialogue").performScrollToNode(hasText("Synthetic reply: Hello"))
        compose.onNodeWithText("Synthetic reply: Hello").assertIsDisplayed()
        compose.onNodeWithTag("dialogue").performScrollToNode(hasText("Welcome synthetic reader"))
        compose.onNodeWithText("Welcome synthetic reader").assertIsDisplayed()
        val session = runBlocking {
            container.repository.observeSessions().first()
                .first { it.character.name == "Synthetic Ada" }
        }
        val turns =
            runBlocking { container.repository.observeTurns(session.identity.sessionId).first() }
        assertEquals(
            listOf("Welcome synthetic reader", "Hello", "Synthetic reply: Hello"),
            turns.map { it.text }
        )
    }
}
