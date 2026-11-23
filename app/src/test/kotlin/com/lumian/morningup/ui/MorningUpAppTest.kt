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

    /** MVP: 하단 메뉴는 '알람', '설정' 두 개예요(결정 140, 141). */
    @Test
    fun mvp_hasTwoTabs_withoutSleep() {
        compose.setContent { MorningTheme(darkTheme = false, reduceMotion = true) { MorningUpApp() } }
        compose.onNodeWithContentDescription("알람").assertIsSelected()
        compose.onNodeWithText("알람 화면은 준비 중이에요").assertExists()
        compose.onNodeWithContentDescription("잠들기").assertDoesNotExist()

        compose.onNodeWithContentDescription("설정").performClick().assertIsSelected()
        compose.onNodeWithText("설정 화면은 준비 중이에요").assertExists()
    }

    /** 수면 라운지 스위치를 켜면 잠들기 탭이 돌아와요. */
    @Test
    fun sleepLoungeOn_startsOnAlarmTab_andSwitchesTabs() {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) { MorningUpApp(sleepLounge = true) }
        }
        compose.onNodeWithContentDescription("알람").assertIsSelected()
        compose.onNodeWithContentDescription("잠들기").performClick().assertIsSelected()
        compose.onNodeWithText("잠들기 화면은 준비 중이에요").assertExists()
    }

    @Test
    fun settingsTab_showsGivenContent() {
        compose.setContent {
            MorningTheme(darkTheme = true, reduceMotion = false) {
                MorningUpApp(settingsTab = { Text("설정 자리") })
            }
        }
        compose.onNodeWithContentDescription("설정").performClick().assertIsSelected()
        compose.onNodeWithText("설정 자리").assertExists()
    }

    @Test
    fun alarmTab_showsGivenContent() {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                MorningUpApp(alarmTab = { Text("홈 자리") })
            }
        }
        compose.onNodeWithText("홈 자리").assertExists()
        compose.onNodeWithContentDescription("설정").performClick()
        compose.onNodeWithText("홈 자리").assertDoesNotExist()
    }
}
