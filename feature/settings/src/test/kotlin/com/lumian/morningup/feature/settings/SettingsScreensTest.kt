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
    fun t1_requiredOff_needsAttention_andOpensPermissionPage() {
        var opened = false
        setT1(report(AlarmPermission.Overlay)) { opened = true }
        compose.onNodeWithText("확인이 필요해요").assertExists()
        compose.onNodeWithText("1.0.0").assertExists()
        compose.onNodeWithText("권한 점검").performClick()
        assertThat(opened).isTrue()
    }

    @Test
    fun t1_optionalOff_noValue() {
        setT1(report(AlarmPermission.Battery, AlarmPermission.Location, AlarmPermission.AlarmVolume))
        compose.onNodeWithText("확인이 필요해요").assertDoesNotExist()
    }

    @Test
    fun t1_silenced_needsAttention() {
        setT1(report().copy(alarmsSilenced = true))
        compose.onNodeWithText("확인이 필요해요").assertExists()
    }

    /** TSK-287: 카드 8개 묶음 이름과 새 줄. 개인정보 처리방침은 주소가 없으면 숨겨요. */
    @Test
    fun t1_groupsAndNewRows() {
        setT1(report())
        listOf("알람이 잘 울리게", "알람", "소리", "미션 중", "화면", "도움", "앱 정보").forEach {
            compose.onNodeWithText(it).assertExists()
        }
        compose.onNodeWithText("방해 금지 모드에서 허용하기").assertExists()
        compose.onNodeWithText("한 주 시작 요일").assertExists()
        compose.onNodeWithText("일요일").assertExists()
        compose.onNodeWithText("이어폰, 블루투스로 울리기").assertExists()
        compose.onNodeWithText("문의하기").assertExists()
        compose.onNodeWithText("개인정보 처리방침").assertDoesNotExist()
    }

    @Test
    fun t1_weekStartSheet_picksMonday() {
        var monday: Boolean? = null
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                SettingsScreen(
                    report = report(),
                    versionName = "1.0.0",
                    tryAlarmAt = null,
                    onOpenPermissions = {},
                    onTryAlarm = {},
                    onFeedback = {},
                    onWeekMonday = { monday = it },
                )
            }
        }
        compose.onNodeWithText("한 주 시작 요일").performScrollTo().performClick()
        compose.onNodeWithText("월요일").performClick()
        assertThat(monday).isTrue()
    }

    @Test
    fun t1_privacyRow_whenUrlGiven() {
        var opened = false
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                SettingsScreen(
                    report = report(),
                    versionName = "1.0.0",
                    tryAlarmAt = null,
                    onOpenPermissions = {},
                    onTryAlarm = {},
                    onFeedback = {},
                    onPrivacy = { opened = true },
                )
            }
        }
        compose.onNodeWithText("개인정보 처리방침").performScrollTo().performClick()
        assertThat(opened).isTrue()
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
