package com.lumian.morningup.feature.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.AlarmPermission
import com.lumian.morningup.core.model.PermissionReport
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

    private fun setT1(report: PermissionReport, onOpen: () -> Unit = {}) {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                SettingsScreen(
                    report = report,
                    versionName = "1.0.0",
                    tryAlarmAt = null,
                    onOpenPermissions = onOpen,
                    onTryAlarm = {},
                    onFeedback = {},
                )
            }
        }
    }

    /** TSK-270: 출시 앱처럼 '화면 모드(임시)' 줄은 화면에 없어요. 화면 모드는 디버그 adb THEME 명령으로 바꿔요. */
    @Test
    fun themeModeRow_absent() {
        setT1(report())
        compose.onNodeWithText("화면 모드(임시)").assertDoesNotExist()
    }

    @Test
    fun t1_oneRow_partialWhenOneRequiredOff_andOpensPermissionPage() {
        var opened = false
        setT1(report(AlarmPermission.Overlay)) { opened = true }
        compose.onNodeWithText("일부 허용됨").assertExists()
        compose.onNodeWithText("알람 울림").assertDoesNotExist()
        compose.onNodeWithText("알람 화면 표시").assertDoesNotExist()
        compose.onNodeWithText("1.0.0").assertExists()
        compose.onNodeWithText("권한").performClick()
        assertThat(opened).isTrue()
    }

    @Test
    fun t1_optionalOff_stillGranted() {
        setT1(report(AlarmPermission.Battery, AlarmPermission.Location, AlarmPermission.AlarmVolume))
        compose.onNodeWithText("허용됨").assertExists()
    }

    @Test
    fun t1_allRequiredOff_none() {
        setT1(report(AlarmPermission.Notifications, AlarmPermission.FullScreen, AlarmPermission.Overlay))
        compose.onNodeWithText("허용 안 됨").assertExists()
    }

    private fun setPage(
        report: PermissionReport,
        samsung: Boolean,
        exactAlarm: Boolean = false,
        onOpen: (AlarmPermission) -> Unit = {},
    ) {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                PermissionPage(
                    report = report,
                    showSamsungSleep = samsung,
                    onBack = {},
                    onOpen = onOpen,
                    onOpenSamsungSleep = {},
                    showExactAlarm = exactAlarm,
                )
            }
        }
    }

    @Test
    fun page_android12_showsExactAlarmRow() {
        var opened: AlarmPermission? = null
        setPage(report(AlarmPermission.ExactAlarm), samsung = false, exactAlarm = true) { opened = it }
        compose.onNodeWithText("정확한 알람").assertExists().performClick()
        assertThat(opened).isEqualTo(AlarmPermission.ExactAlarm)
    }

    @Test
    fun exactAlarmRow_onlyAndroid12() {
        assertThat(listOf(30, 31, 32, 33).map(::exactAlarmRowVisible))
            .containsExactly(false, true, true, false).inOrder()
    }

    @Test
    fun page_threeCards_noExactAlarm() {
        setPage(report(AlarmPermission.ExactAlarm), samsung = true)
        compose.onNodeWithText("필수 권한").assertExists()
        compose.onNodeWithText("선택 권한").assertExists()
        compose.onNodeWithText("기타").performScrollTo().assertExists()
        compose.onNodeWithText("다른 앱 위에 표시").assertExists()
        compose.onNodeWithText("위치").assertExists()
        compose.onNodeWithText("절전 예외 앱에 추가").performScrollTo().assertExists()
        compose.onNodeWithText("정확한 알람").assertDoesNotExist()
    }

    @Test
    fun page_rowsOpenTheirSetting() {
        val opened = mutableListOf<AlarmPermission>()
        setPage(report(), samsung = false) { opened += it }
        compose.onNodeWithText("절전 예외 앱에 추가").assertDoesNotExist()
        compose.onNodeWithText("배터리 사용 제한 없음").performScrollTo().performClick()
        compose.onNodeWithText("알람 음량").performScrollTo().performClick()
        assertThat(opened).containsExactly(AlarmPermission.Battery, AlarmPermission.AlarmVolume).inOrder()
    }
}
