package com.lumian.morningup.feature.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.AlarmPermission
import com.lumian.morningup.core.model.PermissionReport
import com.lumian.morningup.core.model.PermissionScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private fun report(vararg off: AlarmPermission) =
        PermissionReport(AlarmPermission.entries.associateWith { it !in off })

    @Test
    fun t1_warnsOnlyWhenRequiredOff_andOpensPermissionPage() {
        var opened: PermissionScreen? = null
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                SettingsScreen(
                    report = report(AlarmPermission.Battery),
                    versionName = "1.0.0",
                    tryAlarmAt = null,
                    onOpenPermissions = { opened = it },
                    onTryAlarm = {},
                    onFeedback = {},
                )
            }
        }
        // 알람 울림은 필수 3개 중 2개만 켜져서 '일부 허용됨', 알람 화면 표시는 '허용됨'(결정 102).
        compose.onNodeWithText("일부 허용됨").assertExists()
        compose.onNodeWithText("허용됨").assertExists()
        compose.onNodeWithText("1.0.0").assertExists()
        compose.onNodeWithText("알람 울림").performClick()
        assertThat(opened).isEqualTo(PermissionScreen.Ring)
    }

    @Test
    fun t1_allOn_noWarning() {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                SettingsScreen(report(), "1.0.0", null, {}, {}, {})
            }
        }
        compose.onNodeWithText("일부 허용됨").assertDoesNotExist()
        compose.onNodeWithText("허용 안 됨").assertDoesNotExist()
    }

    @Test
    fun t2a_hidesExactWhenOn_andRowOpensDeviceSetting() {
        val opened = mutableListOf<AlarmPermission>()
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                PermissionPage(
                    screen = PermissionScreen.Ring,
                    report = report(),
                    showSamsungSleep = false,
                    onBack = {},
                    onOpen = { opened += it },
                    onOpenSamsungSleep = {},
                )
            }
        }
        compose.onNodeWithText("정확한 알람").assertDoesNotExist()
        compose.onNodeWithText("절전 예외 앱에 추가").assertDoesNotExist()
        compose.onNodeWithText("배터리 사용 제한 없음").performClick()
        assertThat(opened).containsExactly(AlarmPermission.Battery)
    }

    @Test
    fun t2b_showsOptionalOverlay() {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                PermissionPage(PermissionScreen.Screen, report(AlarmPermission.Overlay), false, {}, {}, {})
            }
        }
        compose.onNodeWithText("선택 권한").assertExists()
        compose.onNodeWithText("다른 앱 위에 표시").assertExists()
        compose.onNodeWithText("전체 화면 알림").assertExists()
    }
}
