package com.lumian.morningup.feature.ring

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.designsystem.theme.MorningTheme
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
        compose.onNodeWithText("6:30").assertExists()
        compose.onNodeWithText("알람 끄기").assertDoesNotExist()
        compose.onNodeWithText("미션 시작").performClick()
        assertThat(pressed).isTrue()
    }

    @Test
    fun ringing_withoutMissions_showsDismiss() {
        ringing(hasMissions = false)
        compose.onNodeWithText("알람 끄기").assertExists()
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

    @Test
    fun wakeCheck_showsCountdown_andAwake() {
        var awake = false
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                WakeCheckScreen(
                    dayPart = DayPart.Morning,
                    answerSeconds = 60,
                    remainingMillis = 42_000,
                    confirmed = false,
                    onAwake = { awake = true },
                )
            }
        }
        compose.onNodeWithText("깨어 있나요?").assertExists()
        compose.onNodeWithText("0:42").assertExists()
        compose.onNodeWithText("1분 안에 눌러 주세요", substring = true).assertExists()
        compose.onNodeWithText("깼어요").performClick()
        assertThat(awake).isTrue()
    }
}
