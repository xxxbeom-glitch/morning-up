package com.lumian.morningup.feature.settings

import android.os.Build
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.AlarmPermission
import com.lumian.morningup.core.model.PermissionReport
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FirstRunPermissionsTest {
    @get:Rule
    val compose = createComposeRule()

    private val sdk14 = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    private val sdk12 = Build.VERSION_CODES.S

    /** [off]만 꺼진 상태. */
    private fun report(vararg off: AlarmPermission) =
        PermissionReport(AlarmPermission.entries.associateWith { it !in off })

    @Test
    fun allOn_skipsO5() {
        assertThat(FirstRunPermissionRow.missing(report(), sdk14)).isEmpty()
    }

    @Test
    fun allOff_showsFourRowsInPlanOrder() {
        val rows = FirstRunPermissionRow.missing(report(*AlarmPermission.entries.toTypedArray()), sdk14)
        assertThat(rows).containsExactly(
            FirstRunPermissionRow.Alarm,
            FirstRunPermissionRow.Battery,
            FirstRunPermissionRow.Overlay,
            FirstRunPermissionRow.PowerGuard,
        ).inOrder()
    }

    @Test
    fun alarmRow_coversNotificationsAndFullScreen_andOpensOffOneFirst() {
        val fullScreenOff = report(AlarmPermission.FullScreen)
        assertThat(FirstRunPermissionRow.missing(fullScreenOff, sdk14)).containsExactly(FirstRunPermissionRow.Alarm)
        assertThat(FirstRunPermissionRow.Alarm.nextToOpen(fullScreenOff, sdk14)).isEqualTo(AlarmPermission.FullScreen)
        val bothOff = report(AlarmPermission.Notifications, AlarmPermission.FullScreen)
        assertThat(FirstRunPermissionRow.Alarm.nextToOpen(bothOff, sdk14)).isEqualTo(AlarmPermission.Notifications)
    }

    @Test
    fun exactAlarm_countsOnlyOnAndroid12() {
        val exactOff = report(AlarmPermission.ExactAlarm)
        assertThat(FirstRunPermissionRow.missing(exactOff, sdk12)).containsExactly(FirstRunPermissionRow.Alarm)
        assertThat(FirstRunPermissionRow.missing(exactOff, sdk14)).isEmpty()
    }

    @Test
    fun locationAndDnd_areNotAskedInFirstRun() {
        val rows = FirstRunPermissionRow.missing(report(AlarmPermission.Location, AlarmPermission.DoNotDisturb), sdk14)
        assertThat(rows).isEmpty()
    }

    @Test
    fun page_showsRows_checkForGranted_andStartFinishes() {
        var set: FirstRunPermissionRow? = null
        var finished = 0
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                FirstRunPermissionPage(
                    rows = listOf(FirstRunPermissionRow.Alarm, FirstRunPermissionRow.Battery),
                    isOn = { it == FirstRunPermissionRow.Battery },
                    onSet = { set = it },
                    onFinish = { finished++ },
                )
            }
        }
        compose.onNodeWithText("알람이 제때 울리도록\n아래 권한을 켜 주세요").assertExists()
        compose.onNodeWithText("잠금 화면에서도 알람 화면을 바로 띄워요").assertExists()
        // 켜진 줄은 '설정하기' 대신 체크라서 '설정하기'는 하나뿐이에요.
        compose.onNodeWithText("설정하기").performClick()
        assertThat(set).isEqualTo(FirstRunPermissionRow.Alarm)
        compose.onNodeWithTag("${FIRST_RUN_PERM_ROW_TAG}Battery").performClick()
        assertThat(set).isEqualTo(FirstRunPermissionRow.Alarm)
        compose.onNodeWithText("시작하기").performClick()
        compose.onNodeWithContentDescription("닫기").performClick()
        assertThat(finished).isEqualTo(2)
    }
}
