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
        compose.onNodeWithText("허용하지 않으면 잠금 화면에 알람이 바로 안 떠요.").assertExists()
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

        step = OnboardingStep.Battery
        compose.onNodeWithText("제한 없음").assertExists()
        compose.onNodeWithText("제한 없이 사용").assertExists()

        // O2-4, O3는 피그마 그림 칸이 비어 있어 칸만 있고 그림은 없어요.
        step = OnboardingStep.Overlay
        compose.onNodeWithTag(PICTURE_TAG_PREFIX + "Overlay").assertExists()
        compose.onNodeWithText("미션 시작").assertDoesNotExist()
        compose.onNodeWithText("제한 없음").assertDoesNotExist()
    }

    @Test
    fun splash_showsWordmark() {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) { OnboardingSplash() }
        }
        compose.onNodeWithText("morning up").assertExists()
    }
}
