package com.lumian.morningup.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.LabSettings
import com.lumian.morningup.core.model.TrickLevel
import com.lumian.morningup.core.model.TrickSpeed
import com.lumian.morningup.core.model.WakeCheckSettings
import com.lumian.morningup.core.testing.MainDispatcherRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 실험실 T1 카드, T3, T3-1, T3-2(결정 146 1절, 시안 TSK-171). */
@RunWith(RobolectricTestRunner::class)
class LabPageTest {
    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val main = MainDispatcherRule()

    private val calls = mutableListOf<String>()

    private fun showPage(lab: LabSettings = LabSettings()) = compose.setContent {
        MorningTheme(darkTheme = false, reduceMotion = true) {
            LabScreen(
                lab = lab,
                onBack = { calls += "back" },
                onTrick = { calls += "trick:$it" },
                onTrickLevel = { calls += "level:${it.key}" },
                onTrickSpeed = { calls += "speed:${it.key}" },
                onMultiAsk = { calls += "multi:$it" },
                onMultiAskCount = { calls += "count:$it" },
                onPreview = { calls += "preview" },
            )
        }
    }

    @Test
    fun t1Card_showsOff_orTurnedOnNames() {
        var lab by mutableStateOf(LabSettings())
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) { LabCardContent(lab) { calls += "open" } }
        }
        compose.onNodeWithText("실험실").assertExists()
        compose.onNodeWithText("꺼짐").assertExists()
        lab = LabSettings(trickEnabled = true, multiAskEnabled = true)
        compose.onNodeWithText("버튼 속이기, 여러 번 묻기").assertExists()
        lab = LabSettings(multiAskEnabled = true)
        compose.onNodeWithText("여러 번 묻기").performClick()
        assertThat(calls).containsExactly("open")
    }

    @Test
    fun t3_off_showsIntroRowsAndNote() {
        showPage()
        compose.onNodeWithText("확실히 잠깨기").assertExists()
        compose.onNodeWithText("울릴 때 버튼이 움직여요").assertExists()
        compose.onNodeWithText("깼는지 3번 물어요").assertExists()
        compose.onNodeWithText("톡백 같은 접근성 기능을 쓰는 중에는 버튼 속이기가 자동으로 꺼져요.").assertExists()
    }

    @Test
    fun t3_on_showsLevelSpeedAndSeconds() {
        showPage(LabSettings(trickEnabled = true, multiAskEnabled = true, multiAskCount = 5))
        compose.onNodeWithText("보통, 속도 보통, 20초 동안").assertExists()
        compose.onNodeWithText("깼는지 5번 물어요").assertExists()
    }

    @Test
    fun trickSheet_picksLevelSpeed_andOpensPreview() {
        showPage(LabSettings(trickEnabled = true, trickLevel = TrickLevel.Strong, trickSpeed = TrickSpeed.Fast))
        compose.onNodeWithText("버튼 속이기").performClick()
        compose.onNodeWithTag(LAB_TRICK_SHEET_TAG).assertExists()
        compose.onNodeWithText("똑같은 가짜 버튼이 섞여요").assertExists()
        compose.onNodeWithText("약하게").performClick()
        compose.onNodeWithText("느리게").performClick()
        compose.onNodeWithText("미리 해보기").performClick()
        assertThat(calls).containsExactly("level:weak", "speed:slow", "preview").inOrder()
    }

    @Test
    fun multiSheet_picksCount() {
        showPage()
        compose.onNodeWithText("여러 번 묻기").performClick()
        compose.onNodeWithTag(LAB_MULTI_SHEET_TAG).assertExists()
        compose.onNodeWithText("4번").performClick()
        assertThat(calls).containsExactly("count:4")
    }

    @Test
    fun t1Recheck_lockedOn_whileMultiAsk() {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                WakeCheckCardContent(
                    settings = WakeCheckSettings(recheckEnabled = false, lab = LabSettings(multiAskEnabled = true)),
                    onAskAfter = {},
                    onAnswer = {},
                    onRecheck = { calls += "recheck:$it" },
                    onRecheckMinutes = {},
                )
            }
        }
        compose.onNodeWithText("실험실에서 3번 묻는 중이에요").assertExists()
        assertThat(calls).isEmpty()
    }
}
