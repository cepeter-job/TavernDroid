package io.github.cepeterjob.taverndroid

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.Lifecycle
import io.github.cepeterjob.taverndroid.engine.CharacterInput
import io.github.cepeterjob.taverndroid.engine.OpState
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ForegroundLifecycleTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val container get() = (compose.activity.application as TavernDroidApplication).container
    private var sessionId = ""

    private fun startHeldReply(): String {
        TestApplication.holdStream = true
        TestApplication.invocations.set(0)
        sessionId = runBlocking {
            val character = container.repository.createCharacter(
                CharacterInput("Synthetic ${UUID.randomUUID()}")
            )
            container.repository.startSession(character.id).identity.sessionId
        }
        compose.waitUntil(10000) {
            compose.onAllNodes(
                androidx.compose.ui.test.hasTestTag("open-$sessionId")
            ).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("open-$sessionId").performScrollTo().performClick()
        compose.onNodeWithTag("message").performTextInput("Synthetic lifecycle message")
        compose.onNodeWithText("Send").performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.waitUntil(10000) { TestApplication.invocations.get() == 1 }
        compose.waitUntil(10000) {
            compose.onAllNodes(
                androidx.compose.ui.test.hasTestTag("draft")
            ).fetchSemanticsNodes().isNotEmpty()
        }
        return runBlocking {
            container.generations.observeLatestOperation(sessionId).first()!!.snapshot.operationId
        }
    }

    @After fun stopHeldReply() {
        TestApplication.holdStream = false
        runBlocking { container.coordinator.stop() }
    }

    @Test fun navigationAndRecreationKeepSameAttemptAndProviderInvocation() {
        val operationId = startHeldReply()
        compose.onNodeWithText("Stories").performClick()
        compose.onNodeWithTag("open-$sessionId").performScrollTo().performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Stop").assertIsDisplayed()
        val latest =
            runBlocking { container.generations.observeLatestOperation(sessionId).first()!! }
        assertEquals(operationId, latest.snapshot.operationId)
        assertEquals(OpState.Streaming, latest.state)
        assertEquals(1, TestApplication.invocations.get())
    }

    @Test fun genuineBackgroundCancelsAndReturningDoesNotReplay() {
        startHeldReply()
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        val deadline = System.currentTimeMillis() + 10000
        var state: OpState?
        do {
            state =
                runBlocking {
                    container.generations.observeLatestOperation(sessionId).first()?.state
                }
            if (state == OpState.Cancelled) break
            Thread.sleep(50)
        } while (System.currentTimeMillis() < deadline)
        assertEquals(OpState.Cancelled, state)
        val operation =
            runBlocking { container.generations.observeLatestOperation(sessionId).first()!! }
        assertTrue(operation.draft.isNotEmpty())
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.onNodeWithText("Retry").performScrollTo().assertIsDisplayed()
        assertEquals(1, TestApplication.invocations.get())
    }
}
