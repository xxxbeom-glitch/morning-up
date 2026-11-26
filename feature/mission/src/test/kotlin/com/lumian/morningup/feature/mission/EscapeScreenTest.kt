package com.lumian.morningup.feature.mission

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.engine.MissionEscape
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** TSK-210, 결정 171, 172: M5 미션 포기 화면. */
@RunWith(RobolectricTestRunner::class)
class EscapeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun show(initial: EscapeUiState, remaining: Long = 48_000L) {
        compose.setContent {
            var state by mutableStateOf(initial)
            MorningTheme(darkTheme = true, reduceMotion = true) {
                EscapeScreen(
                    state = state,
                    remainingMillis = remaining,
                    onType = {
                        calls += "type"
                        state = state.copy(text = it, result = null)
                    },
                    onSubmit = { calls += "submit:${state.text}" },
                    onBack = { calls += "back" },
                )
            }
        }
    }

    @Test
    fun typing_showsSentenceIdleTimeAndHelp() {
        show(EscapeUiState(sentence = MissionEscape.TEST_SENTENCE))
        compose.onNodeWithText("미션 포기").assertExists()
        compose.onNodeWithText(MissionEscape.TEST_SENTENCE).assertExists()
        compose.onNodeWithText("48초 동안 안 쓰면 미션으로 돌아가요").assertExists()
        compose.onNodeWithText("띄어쓰기와 기호까지 똑같이 써 주세요").assertExists()
        compose.onNodeWithText("소리를 잠시 껐어요").assertExists()
        compose.onNodeWithTag(ESCAPE_INPUT_TAG).performTextInput("테스트")
        compose.onNodeWithTag(ESCAPE_SUBMIT_TAG).performClick()
        assertThat(calls).containsExactly("type", "submit:테스트").inOrder()
    }

    @Test
    fun wrong_showsRedGuide() {
        show(EscapeUiState(sentence = MissionEscape.TEST_SENTENCE, text = "테스트", result = EscapeResult.Wrong))
        compose.onNodeWithText("조금 달라요. 한 번 더 따라 써 주세요").assertExists()
    }

    @Test
    fun correct_hidesBottomRowAndLocksInput() {
        show(
            EscapeUiState(
                sentence = MissionEscape.TEST_SENTENCE,
                text = MissionEscape.TEST_SENTENCE,
                result = EscapeResult.Correct,
            ),
        )
        compose.onNodeWithText("맞아요. 알람을 껐어요").assertExists()
        compose.onNodeWithText("미션으로 돌아가기").assertDoesNotExist()
        compose.onNodeWithTag(ESCAPE_INPUT_TAG).assertIsNotEnabled()
    }

    @Test
    fun backPill_returnsToMission() {
        show(EscapeUiState(sentence = MissionEscape.TEST_SENTENCE))
        compose.onNodeWithTag(ESCAPE_BACK_TAG).performClick()
        assertThat(calls).containsExactly("back")
    }
}
