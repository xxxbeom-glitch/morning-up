package com.lumian.morningup.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MorningUpAppTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun startsOnAlarmTab_andSwitchesTabs() {
        compose.setContent { MorningTheme(darkTheme = false, reduceMotion = true) { MorningUpApp() } }
        compose.onNodeWithContentDescription("알람").assertIsSelected()
        compose.onNodeWithText("알람 화면은 준비 중이에요").assertExists()

        compose.onNodeWithContentDescription("잠들기").performClick().assertIsSelected()
        compose.onNodeWithText("잠들기 화면은 준비 중이에요").assertExists()
    }

    @Test
    fun settingsTab_showsSample_inDebugBuild() {
        compose.setContent { MorningTheme(darkTheme = true, reduceMotion = false) { MorningUpApp() } }
        compose.onNodeWithContentDescription("설정").performClick().assertIsSelected()
        compose.onNodeWithText("견본").assertExists()
    }

    @Test
    fun alarmTab_showsGivenContent() {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                MorningUpApp(alarmTab = { Text("홈 자리") })
            }
        }
        compose.onNodeWithText("홈 자리").assertExists()
        compose.onNodeWithContentDescription("잠들기").performClick()
        compose.onNodeWithText("홈 자리").assertDoesNotExist()
    }
}
