package com.lumian.morningup.feature.mission

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.mission.MathKey
import com.lumian.morningup.core.model.mission.QuickMathGame
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuickMathScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val game = QuickMathGame(level = 1, target = 3, random = Random(SEED))

    @Test
    fun showsProblem_streak_andHint() {
        val start = game.start()
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                QuickMathScreen(QuickMathUiState(game = start), remainingMillis = 42_000, onKey = {})
            }
        }
        compose.onNodeWithText(start.problem.text).assertExists()
        compose.onNodeWithText("연속 정답 0 / 3").assertExists()
        compose.onNodeWithText("틀리면 새 문제로 처음부터 해요").assertExists()
    }

    @Test
    fun keys_followShuffledOrder_andSendDigits() {
        val start = game.start()
        val pressed = mutableListOf<MathKey>()
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                QuickMathScreen(QuickMathUiState(game = start), remainingMillis = 42_000, onKey = { pressed += it })
            }
        }
        compose.onNodeWithTag("${MATH_KEY_TAG}0").assertContentDescriptionEquals(start.keypad[0].toString())
        compose.onNodeWithTag("${MATH_KEY_TAG}10").assertContentDescriptionEquals(start.keypad[9].toString())
        compose.onNodeWithTag("${MATH_KEY_TAG}0").performClick()
        compose.onNodeWithTag("${MATH_KEY_TAG}9").performClick()
        compose.onNodeWithTag("${MATH_KEY_TAG}11").performClick()
        assertThat(pressed).containsExactly(MathKey.Digit(start.keypad[0]), MathKey.Delete, MathKey.Confirm).inOrder()
    }

    @Test
    fun correctAnswer_reshufflesKeys_onScreen() {
        val start = game.start()
        var state by mutableStateOf(QuickMathUiState(game = start))
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                QuickMathScreen(state, remainingMillis = 42_000, onKey = {})
            }
        }
        val typed = start.problem.answer.toString().fold(start) { s, c -> game.type(s, c.digitToInt()) }
        val next = game.submit(typed).first
        state = QuickMathUiState(game = next)
        compose.waitForIdle()
        val shown = (0 until 9).map { slot -> next.keypad[slot].toString() }
        shown.forEachIndexed { slot, digit ->
            compose.onNodeWithTag("$MATH_KEY_TAG$slot").assertContentDescriptionEquals(digit)
        }
        compose.onNodeWithText("연속 정답 1 / 3").assertExists()
    }

    private companion object {
        const val SEED = 7
    }
}
