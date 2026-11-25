package com.lumian.morningup.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.LabSettings
import com.lumian.morningup.core.model.TrickSpeed
import com.lumian.morningup.core.model.WakeCheckSettings
import com.lumian.morningup.core.testing.MainDispatcherRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 실험실 T1 카드, T3, T3-1, T3-2(결정 146 1절, 시안 TSK-171), T3 미션 포기 숨기기와 T3-4(결정 151, 시안 TSK-181). */
@RunWith(RobolectricTestRunner::class)
class LabPageTest {
    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val main = MainDispatcherRule()

    private val calls = mutableListOf<String>()
    private var noGuarded = false

    private fun showPage(lab: LabSettings = LabSettings(), trickOff: Boolean = false) = compose.setContent {
        MorningTheme(darkTheme = false, reduceMotion = true) {
            LabScreen(
                lab = lab,
                onBack = { calls += "back" },
                onTrick = { calls += "trick:$it" },
                onTrickSpeed = { calls += "speed:${it.key}" },
                onMultiAsk = { calls += "multi:$it" },
                onMultiAskCount = { calls += "count:$it" },
                onPreview = { calls += "preview" },
                onEscapeHidden = { calls += "escape:$it" },
                trickOff = trickOff,
                noGuardedAlarm = noGuarded,
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
    fun t3_on_showsSpeedAndSeconds() {
        showPage(LabSettings(trickEnabled = true, multiAskEnabled = true, multiAskCount = 5))
        compose.onNodeWithText("속도 보통, 20초 동안").assertExists()
        compose.onNodeWithText("깼는지 5번 물어요").assertExists()
    }

    @Test
    fun trickSheet_picksSpeed_andOpensPreview() {
        showPage(LabSettings(trickEnabled = true, trickSpeed = TrickSpeed.Fast))
        compose.onNodeWithText("버튼 속이기").performClick()
        compose.onNodeWithTag(LAB_TRICK_SHEET_TAG).assertExists()
        // 결정 155로 강도 칩은 없어졌어요.
        compose.onNodeWithText("똑같은 가짜 버튼이 섞여요").assertDoesNotExist()
        compose.onNodeWithText("약하게").assertDoesNotExist()
        compose.onNodeWithText("느리게").performClick()
        compose.onNodeWithText("미리 해보기").performClick()
        assertThat(calls).containsExactly("speed:slow", "preview").inOrder()
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

    @Test
    fun escapeCard_showsRowAndNote() {
        showPage()
        compose.onNodeWithText("'미션 포기' 알림 숨기기").performScrollTo().assertExists()
        compose.onNodeWithText("미션을 못 할 때 뜨는 알림이에요").assertExists()
        compose.onNodeWithText("숨겨도 흔들기 센서가 없는 폰이나 마지막 다시 울림에서는 떠요.").assertExists()
    }

    @Test
    fun escapeHide_needsConfirm_thenHides() {
        showPage()
        compose.onNodeWithText("'미션 포기' 알림 숨기기").performScrollTo().performClick()
        compose.onNodeWithTag(LAB_ESCAPE_SHEET_TAG).assertExists()
        compose.onNodeWithText("'미션 포기' 알림을 숨길까요?").assertExists()
        compose.onNodeWithText("숨겨도 이럴 때는 떠요").assertExists()
        compose.onNodeWithText("2. 다시 울림이 마지막 바퀴일 때, 첫 미션에서").assertExists()
        assertThat(calls).isEmpty()
        compose.onNodeWithText("숨기기").performClick()
        assertThat(calls).containsExactly("escape:true")
        compose.onNodeWithTag(LAB_ESCAPE_SHEET_TAG).assertDoesNotExist()
    }

    @Test
    fun escapeHide_cancel_keepsShowing() {
        showPage()
        compose.onNodeWithText("'미션 포기' 알림 숨기기").performScrollTo().performClick()
        compose.onNodeWithText("취소").performClick()
        compose.onNodeWithTag(LAB_ESCAPE_SHEET_TAG).assertDoesNotExist()
        assertThat(calls).isEmpty()
    }

    @Test
    fun escapeShow_turnsOffAtOnce() {
        showPage(LabSettings(escapeHidden = true))
        compose.onNodeWithText("'미션 포기' 알림 숨기기").performScrollTo().performClick()
        compose.onNodeWithTag(LAB_ESCAPE_SHEET_TAG).assertDoesNotExist()
        assertThat(calls).containsExactly("escape:false")
    }

    @Test
    fun trickRow_a11yOff_showsNotice() {
        showPage(LabSettings(trickEnabled = true), trickOff = true)
        compose.onNodeWithText("접근성 기능이 켜져 있어 버튼 속이기가 꺼져요").assertExists()
    }

    @Test
    fun multiAskRow_showsNotice_whenNoGuardedAlarm() {
        noGuarded = true
        showPage()
        compose.onNodeWithText("켜 둔 알람이 없어요. 알람 편집에서 '다시 잠들기 방지'를 켜야 물어요").assertExists()
        compose.onNodeWithText("깼는지 3번 물어요").assertDoesNotExist()
    }
}
