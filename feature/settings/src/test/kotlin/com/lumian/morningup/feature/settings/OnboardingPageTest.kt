package com.lumian.morningup.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingPageTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun fullScreenStep_usesDecidedCopy_andButtonsWork() {
        var allowed = false
        var later = false
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                OnboardingStepPage(
                    step = OnboardingStep.FullScreen,
                    onAllow = { allowed = true },
                    onLater = { later = true },
                )
            }
        }
        compose.onNodeWithText("잠겨 있어도 알람이\n화면 위로 떠요").assertExists()
        compose.onNodeWithText("화면이 꺼져 있을 때 알람이 울리면\n바로 화면으로 보여줘요.").assertExists()
        compose.onNodeWithText("전체 화면 알림 허용하기").performClick()
        compose.onNodeWithText("나중에 하기").performClick()
        assertThat(allowed).isTrue()
        assertThat(later).isTrue()
    }

    @Test
    fun locationStep_darkTheme_showsSkip() {
        compose.setContent {
            MorningTheme(darkTheme = true, reduceMotion = true) {
                OnboardingStepPage(step = OnboardingStep.Location, onAllow = {}, onLater = {})
            }
        }
        compose.onNodeWithText("위치 허용하기").assertExists()
        compose.onNodeWithText("건너뛰기").assertExists()
    }

    @Test
    fun pictures_followFigma_perStep() {
        var step by mutableStateOf(OnboardingStep.Notifications)
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                OnboardingStepPage(step = step, onAllow = {}, onLater = {})
            }
        }
        compose.onNodeWithTag(PICTURE_TAG_PREFIX + "Notifications").assertExists()

        step = OnboardingStep.FullScreen
        compose.onNodeWithTag(PICTURE_TAG_PREFIX + "FullScreen").assertExists()
        // 애니메이션 줄이기면 끝 장면(알람 화면이 올라온 폰)에서 멈춰 있어요.
        compose.onNodeWithText("미션 시작").assertExists()
        compose.onNodeWithText("11월 18일 수요일").assertExists()

        // O2-3: 온보딩 전용 대화 상자. 애니메이션 줄이기면 떠 있는 장면에서 멈춰요.
        step = OnboardingStep.Battery
        compose.onNodeWithText("배터리 최적화를 해제할까요?").assertExists()
        compose.onNodeWithText("거부").assertExists()
        compose.onNodeWithText("허용").assertExists()

        // O2-4: 앱 이름 카드. 끝 장면은 켜진 스위치와 '함'이에요.
        step = OnboardingStep.Overlay
        compose.onNodeWithTag(PICTURE_TAG_PREFIX + "Overlay").assertExists()
        compose.onNodeWithText("일어나").assertExists()
        compose.onNodeWithText("다른 앱 위에 표시 함").assertExists()
        compose.onNodeWithText("다른 앱 위에 표시 안 함").assertDoesNotExist()
        compose.onNodeWithText("미션 시작").assertDoesNotExist()

        // O3: 라이트는 낮 그림(구름과 해).
        step = OnboardingStep.Location
        compose.onNodeWithTag(WEATHER_DAY_TAG, useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(WEATHER_NIGHT_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun darkTheme_weatherIsNight_andModalsStayLight() {
        var step by mutableStateOf(OnboardingStep.Location)
        compose.setContent {
            MorningTheme(darkTheme = true, reduceMotion = true) {
                OnboardingStepPage(step = step, onAllow = {}, onLater = {})
            }
        }
        compose.onNodeWithTag(WEATHER_NIGHT_TAG, useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(WEATHER_DAY_TAG, useUnmergedTree = true).assertDoesNotExist()

        step = OnboardingStep.Battery
        compose.onNodeWithText("배터리 최적화를 해제할까요?").assertExists()
    }

    @Test
    fun notificationsStep_usesLatestCopy() {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                OnboardingStepPage(step = OnboardingStep.Notifications, onAllow = {}, onLater = {})
            }
        }
        compose.onNodeWithText("알림을 허용해 주세요").assertExists()
        compose.onNodeWithText("알림이 꺼져 있으면 알람 화면이 뜨지 않을 수 있어요.\n미션을 못 할 때 끄는 길도 알림으로 와요.").assertExists()
    }

    @Test
    fun splash_showsHandwrittenName() {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) { OnboardingSplash() }
        }
        compose.onNodeWithContentDescription("일어나").assertExists()
    }
}
