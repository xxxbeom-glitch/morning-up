package com.lumian.morningup.feature.settings

import androidx.compose.ui.test.junit4.createComposeRule
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
    fun splash_showsWordmark() {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) { OnboardingSplash() }
        }
        compose.onNodeWithText("morning up").assertExists()
    }
}
