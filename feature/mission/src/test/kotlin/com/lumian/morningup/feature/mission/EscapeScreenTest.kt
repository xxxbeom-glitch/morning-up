package com.lumian.morningup.feature.mission

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
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

    @Test
    fun openKey_changesOnEachOpen_andSurvivesRecreation() {
        // ISS-056: 한 울림에서 M5를 다시 열면 새 열기 번호라 뷰모델이 새로 만들어져요.
        var shown by mutableStateOf(true)
        val keys = mutableListOf<String>()
        val restore = StateRestorationTester(compose)
        restore.setContent {
            if (shown) {
                val key = rememberEscapeOpenKey()
                SideEffect { if (keys.lastOrNull() != key) keys += key }
            }
        }
        compose.waitForIdle()
        // 접기, 회전처럼 다시 그려도 같은 번호예요.
        restore.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        assertThat(keys).hasSize(1)
        shown = false
        compose.waitForIdle()
        shown = true
        compose.waitForIdle()
        assertThat(keys).hasSize(2)
        assertThat(keys[1]).isNotEqualTo(keys[0])
    }

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
        compose.onNodeWithText("문장을 그대로 따라 쓰면 알람이 해제돼요").assertExists()
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
        compose.onNodeWithText("맞아요. 알람을 해제했어요").assertDoesNotExist()
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
        val wrong = out.text.spanStyles.first { it.start == 2 }.item
        assertThat(wrong.color).isEqualTo(colors.wrong)
        // TSK-246: 틀린 글자도 밑줄 없이 빨강만. 남은 예문은 투명(맨 아래 겹이 흐리게 보여 줘요).
        assertThat(out.text.spanStyles.all { it.item.textDecoration == null }).isTrue()
        assertThat(out.text.spanStyles.first { it.start == 3 }.item.color)
            .isEqualTo(androidx.compose.ui.graphics.Color.Transparent)
        assertThat(out.offsetMapping.transformedToOriginal(6)).isEqualTo(3)
    }

    @Test
    fun inputLayer_isAllTransparent_soCompositionUnderlineHides() {
        // 진짜 입력 칸 겹은 글자가 모두 투명이라 키보드 조합 밑줄(글자색을 따라요)도 안 보여요.
        val out = TraceOverlay("테스트 문장", wrongAt = null, colors = null)
            .filter(androidx.compose.ui.text.AnnotatedString("테스ㅌ"))
        assertThat(out.text.text).isEqualTo("테스ㅌ 문장")
        assertThat(out.text.spanStyles.map { it.item.color }.toSet())
            .containsExactly(androidx.compose.ui.graphics.Color.Transparent)
    }

    @Test
    fun ghostLayer_keepsWholeSentence_hidesTypedButShowsUnderComposing() {
        val ghost = androidx.compose.ui.graphics.Color.Gray
        val out = traceGhost("테스트 문장", typedLength = 3, composition = androidx.compose.ui.text.TextRange(2, 3), ghost)
        // 늘 예문 전체라 조합 중 글자 폭이 달라도 뒤 글자 자리가 그대로예요.
        assertThat(out.text).isEqualTo("테스트 문장")
        val colorAt = { i: Int -> out.spanStyles.first { it.start == i }.item.color }
        assertThat(colorAt(0)).isEqualTo(androidx.compose.ui.graphics.Color.Transparent)
        assertThat(colorAt(2)).isEqualTo(ghost)
        assertThat(colorAt(4)).isEqualTo(ghost)
    }
}
