package com.lumian.morningup.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 값 넘기기 줄: 범위 안에서만 바뀌고, 끝 값에서는 그쪽 버튼이 비활성이에요. */
@RunWith(RobolectricTestRunner::class)
class MorningValueSwitchRowTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun stepsWithinRange_andDisablesAtEnds() {
        var last = 0
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                var value by remember { mutableIntStateOf(2) }
                last = value
                MorningValueSwitchRow(
                    label = "난이도",
                    value = "$value",
                    type = ValueSwitchType.Level,
                    canDecrease = value > 1,
                    canIncrease = value < 3,
                    onDecrease = { value = (value - 1).coerceAtLeast(1) },
                    onIncrease = { value = (value + 1).coerceAtMost(3) },
                    decreaseDescription = "낮추기",
                    increaseDescription = "높이기",
                    tag = "row",
                )
            }
        }
        compose.onNodeWithTag("row$VALUE_INC_SUFFIX").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertThat(last).isEqualTo(3)
        compose.onNodeWithTag("row$VALUE_INC_SUFFIX").assertIsNotEnabled()
        compose.onNodeWithTag("row$VALUE_INC_SUFFIX").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertThat(last).isEqualTo(3)
        compose.onNodeWithTag("row$VALUE_DEC_SUFFIX").assertIsEnabled()
        repeat(3) {
            compose.onNodeWithTag("row$VALUE_DEC_SUFFIX").performSemanticsAction(SemanticsActions.OnClick)
            compose.waitForIdle()
        }
        assertThat(last).isEqualTo(1)
        compose.onNodeWithTag("row$VALUE_DEC_SUFFIX").assertIsNotEnabled()
        compose.onNodeWithText("난이도").assertExists()
    }

    @Test
    fun tapOnBoxButton_stepsOnce() {
        var count = 0
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                MorningValueSwitchRow(
                    label = "개수",
                    value = "3",
                    type = ValueSwitchType.Count,
                    canDecrease = true,
                    canIncrease = true,
                    onDecrease = {},
                    onIncrease = { count++ },
                    decreaseDescription = "빼기",
                    increaseDescription = "더하기",
                    tag = "row",
                )
            }
        }
        compose.onNodeWithTag("row$VALUE_INC_SUFFIX").performClick()
        compose.waitForIdle()
        assertThat(count).isEqualTo(1)
    }
}
