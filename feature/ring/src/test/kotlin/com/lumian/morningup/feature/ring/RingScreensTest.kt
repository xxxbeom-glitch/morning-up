package com.lumian.morningup.feature.ring

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.ButtonTrick
import com.lumian.morningup.core.model.DayPart
import com.lumian.morningup.core.model.WeatherKind
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RingScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private val date = LocalDate.of(2026, 11, 16)

    private fun ringing(hasMissions: Boolean, onMain: () -> Unit = {}) = compose.setContent {
        MorningTheme(darkTheme = false, reduceMotion = true) {
            RingingScreen(
                date = date,
                time = LocalTime.of(6, 30),
                dayPart = DayPart.Morning,
                hasMissions = hasMissions,
                onMainButton = onMain,
            )
        }
    }

    @Test
    fun ringing_withMissions_showsStartMission() {
        var pressed = false
        ringing(hasMissions = true) { pressed = true }
        compose.onNodeWithText("11월 16일 월요일").assertExists()
        compose.onNodeWithText("오전 6:30").assertExists()
        compose.onNodeWithText("알람 해제").assertDoesNotExist()
        compose.onNodeWithText("미션 시작").performClick()
        assertThat(pressed).isTrue()
    }

    /** ISS-066: 움직이는 버튼을 켜도 미루기가 눌리고, 미션 시작으로 가지 않아요. */
    @Test
    fun trickOn_snoozeStillTappable() {
        var snoozed = 0
        var main = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                RingingScreen(
                    date = date,
                    time = LocalTime.of(6, 30),
                    dayPart = DayPart.Morning,
                    hasMissions = true,
                    onMainButton = { main++ },
                    trick = ButtonTrick(),
                    snoozeMinutes = 1,
                    onSnooze = { snoozed++ },
                )
            }
        }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag(SNOOZE_BUTTON_TAG).performClick()
        compose.mainClock.advanceTimeBy(100)
        assertThat(snoozed).isEqualTo(1)
        assertThat(main).isEqualTo(0)
    }

    @Test
    fun ringing_withoutMissions_showsDismiss() {
        ringing(hasMissions = false)
        compose.onNodeWithText("알람 해제").assertExists()
        compose.onNodeWithText("미션 시작").assertDoesNotExist()
    }

    @Test
    fun wakeUp_morningGreeting_andConfirm() {
        var confirmed = false
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                WakeUpScreen(dayPart = DayPart.Morning, onConfirm = { confirmed = true })
            }
        }
        compose.onNodeWithText("좋은 아침이에요").assertExists()
        compose.onNodeWithText("확인").performClick()
        assertThat(confirmed).isTrue()
    }

    @Test
    fun wakeUp_rainShowsWeatherLine() {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                WakeUpScreen(dayPart = DayPart.Morning, onConfirm = {}, weather = WeatherKind.Rain)
            }
        }
        compose.onNodeWithText("밖에 비가 와요", substring = true).assertExists()
    }

    private fun wakeCheck(recheck: Boolean = false, remaining: Long? = 42_000, onAwake: () -> Unit = {}) =
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                WakeCheckScreen(
                    dayPart = DayPart.Morning,
                    remainingMillis = remaining,
                    confirmed = false,
                    onAwake = onAwake,
                    recheck = recheck,
                )
            }
        }

    @Test
    fun wakeCheck_showsCountdown_andHoldTenSecondsConfirms() {
        var awake = false
        wakeCheck { awake = true }
        compose.onNodeWithText("일어나셨죠?").assertExists()
        compose.onNodeWithText("0:42").assertExists()
        compose.onNodeWithText("시간 안에 누르지 않으면 알람이 다시 울려요").assertExists()
        // 톡백처럼 두 번 탭하면 10초에 걸쳐 저절로 차요.
        compose.onNodeWithTag(HOLD_BUTTON_TAG).performSemanticsAction(SemanticsActions.OnClick)
        compose.mainClock.advanceTimeBy(5_000)
        assertThat(awake).isFalse()
        compose.mainClock.advanceTimeBy(6_000)
        compose.waitForIdle()
        assertThat(awake).isTrue()
    }

    @Test
    fun hold_releasedEarly_showsHint_andDoesNotConfirm() {
        var awake = false
        wakeCheck { awake = true }
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag(HOLD_BUTTON_TAG).performTouchInput { down(center) }
        // 시계를 멈춰 둔 채라 누른 것이 반영되도록 한 번 처리해요.
        compose.waitForIdle()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(3_000)
        compose.onNodeWithText("8").assertExists()
        compose.onNodeWithTag(HOLD_BUTTON_TAG).performTouchInput { up() }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(2_000)
        compose.onNodeWithText("끝까지 누르고 있어야 해요").assertExists()
        assertThat(awake).isFalse()
    }

    @Test
    fun recheck_showsTitle_andHidesClockWhenOpenedEarly() {
        wakeCheck(recheck = true, remaining = null)
        compose.onNodeWithText("아직 깨어 계시죠?").assertExists()
        compose.onNodeWithText("시간 안에 누르지 않으면 알람이 다시 울려요").assertDoesNotExist()
    }

    @Test
    fun checkDone_showsNextAsk() {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                WakeCheckScreen(
                    dayPart = DayPart.Morning,
                    remainingMillis = null,
                    confirmed = true,
                    onAwake = {},
                    nextAskMinutes = 3,
                )
            }
        }
        compose.onNodeWithText("확인했어요").assertExists()
        compose.onNodeWithText("3분 뒤 재수면 여부를 재확인할게요").assertExists()
    }

    @Test
    fun trickPreview_a11yOff_showsNotice() {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                TrickPreviewScreen(
                    trick = null,
                    onClose = {},
                    trickOff = true,
                    date = LocalDate.of(2026, 11, 24),
                    time = LocalTime.of(7, 0),
                )
            }
        }
        compose.onNodeWithText("미리 해보기, 소리는 나지 않아요").assertExists()
        compose.onNodeWithText("접근성 기능이 켜져 있어 움직이는 버튼이 꺼져요").assertExists()
    }
}
