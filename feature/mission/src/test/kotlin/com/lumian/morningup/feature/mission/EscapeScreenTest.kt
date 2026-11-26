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

/** TSK-238(시안 TSK-231), 결정 171: M5 미션 포기, 흐린 예문 위에 따라 쓰기. */
@RunWith(RobolectricTestRunner::class)
class EscapeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val typed = mutableListOf<String>()
    private var backs = 0

    private fun show(initial: EscapeUiState, remaining: Long = 48_000L) {
        compose.setContent {
            var state by mutableStateOf(initial)
            MorningTheme(darkTheme = true, reduceMotion = true) {
                EscapeScreen(
                    state = state,
                    remainingMillis = remaining,
                    onType = {
                        typed += it
                        state = state.copy(text = it)
                    },
                    onBack = { backs++ },
                )
            }
        }
    }

    @Test
    fun typing_showsGuideTimeAndGhostSentence_withoutRemovedParts() {
        show(EscapeUiState(sentence = MissionEscape.TEST_SENTENCE))
        compose.onNodeWithText("문장을 그대로 따라 쓰면 알람이 꺼져요").assertExists()
        compose.onNodeWithTag(MISSION_TIMELINE_TAG).assertExists()
        compose.onNodeWithText("다시 미션 수행하기").assertExists()
        compose.onNodeWithText("미션 포기").assertDoesNotExist()
        compose.onNodeWithText("소리를 잠시 껐어요").assertDoesNotExist()
        compose.onNodeWithText("빨간 글자를 지우고 다시 써 주세요").assertDoesNotExist()
        compose.onNodeWithTag(ESCAPE_INPUT_TAG).performTextInput("테스트")
        assertThat(typed).containsExactly("테스트")
    }

    @Test
    fun lowTime_guideTurnsIntoCountdown() {
        show(EscapeUiState(sentence = MissionEscape.TEST_SENTENCE), remaining = 9_000L)
        compose.onNodeWithText("9초 안에 쓰지 않으면 미션으로 돌아가요").assertExists()
    }

    @Test
    fun correct_hidesPillsAndLocksInput() {
        show(
            EscapeUiState(
                sentence = MissionEscape.TEST_SENTENCE,
                text = MissionEscape.TEST_SENTENCE,
                result = EscapeResult.Correct,
            ),
        )
        compose.onNodeWithTag(ESCAPE_BACK_TAG).assertDoesNotExist()
        compose.onNodeWithText("맞아요. 알람을 껐어요").assertDoesNotExist()
        compose.onNodeWithTag(ESCAPE_INPUT_TAG).assertIsNotEnabled()
    }

    @Test
    fun backPill_returnsToMission() {
        show(EscapeUiState(sentence = MissionEscape.TEST_SENTENCE))
        compose.onNodeWithTag(ESCAPE_BACK_TAG).performClick()
        assertThat(backs).isEqualTo(1)
    }

    @Test
    fun overlay_appendsGhostRest_andKeepsCursorInTypedPart() {
        val colors = TraceColors(
            done = androidx.compose.ui.graphics.Color.White,
            ghost = androidx.compose.ui.graphics.Color.Gray,
            wrong = androidx.compose.ui.graphics.Color.Red,
        )
        val out = TraceOverlay("테스트 문장", wrongAt = 2, colors = colors)
            .filter(androidx.compose.ui.text.AnnotatedString("테스ㅌ"))
        assertThat(out.text.text).isEqualTo("테스ㅌ 문장")
        assertThat(out.text.spanStyles.first { it.start == 2 }.item.color).isEqualTo(colors.wrong)
        assertThat(out.offsetMapping.transformedToOriginal(6)).isEqualTo(3)
    }
}
