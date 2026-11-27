package com.lumian.morningup.feature.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
    fun splash_showsHandwrittenName() {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) { OnboardingSplash() }
        }
        compose.onNodeWithContentDescription("일어나").assertExists()
    }
}
