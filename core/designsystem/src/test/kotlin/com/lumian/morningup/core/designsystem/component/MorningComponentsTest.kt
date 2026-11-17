package com.lumian.morningup.core.designsystem.component

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
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
class MorningComponentsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun settingRow_disabled_ignoresToggleAndClick() {
        var toggled = false
        var clicked = false
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                MorningSettingRow(
                    label = "전원 종료 방지",
                    value = "미션 끝날 때까지",
                    state = SettingRowState.Disabled,
                    onCheckedChange = { toggled = true },
                    onClick = { clicked = true },
                )
            }
        }
        compose.onNode(isToggleable()).assertIsOff().assertIsNotEnabled().performClick()
        compose.onNodeWithText("전원 종료 방지").performClick()
        assertThat(toggled).isFalse()
        assertThat(clicked).isFalse()
    }

    @Test
    fun settingRow_on_togglesAndOpens() {
        var toggled: Boolean? = null
        var clicked = false
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                MorningSettingRow(
                    label = "다시 울림",
                    value = "5분, 3번",
                    state = SettingRowState.On,
                    onCheckedChange = { toggled = it },
                    onClick = { clicked = true },
                )
            }
        }
        compose.onNode(isToggleable()).assertIsOn().performClick()
        compose.onNodeWithText("다시 울림").performClick()
        assertThat(toggled).isFalse()
        assertThat(clicked).isTrue()
    }

    @Test
    fun dialog_buttonsCallBack() {
        var result = ""
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                MorningDialog(
                    title = "이 알람을 삭제할까요?",
                    message = "오전 6:30 평일 알람이에요.",
                    dismissLabel = "취소",
                    confirmLabel = "삭제",
                    onDismiss = { result += "dismiss" },
                    onConfirm = { result += "confirm" },
                    danger = true,
                )
            }
        }
        compose.onNodeWithText("이 알람을 삭제할까요?").assertExists()
        compose.onNodeWithText("삭제").performClick()
        compose.onNodeWithText("취소").performClick()
        assertThat(result).isEqualTo("confirmdismiss")
    }

    @Test
    fun snackbar_actionCallsBack() {
        var undone = false
        compose.setContent {
            MorningTheme(darkTheme = true, reduceMotion = false) {
                MorningSnackbar(
                    text = "알람을 지웠어요",
                    actionLabel = "되돌리기",
                    onAction = { undone = true },
                    onSwipeDismiss = {},
                )
            }
        }
        compose.onNodeWithText("되돌리기").performClick()
        assertThat(undone).isTrue()
    }

    @Test
    fun wheelPicker_showsSelectedInMiddle() {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                MorningWheelPicker(
                    items = (0..59).map { "%02d".format(it) },
                    selectedIndex = 30,
                    onSelect = {},
                    looping = true,
                )
            }
        }
        compose.onNodeWithText("29").assertExists()
        compose.onNodeWithText("30").assertExists()
        compose.onNodeWithText("31").assertExists()
    }

    @Test
    fun splitRuns_separatesDigits() {
        assertThat(splitRuns("7시간 20분 후,")).containsExactly(
            TextRun("7", true),
            TextRun("시간 ", false),
            TextRun("2", true),
            TextRun("0", true),
            TextRun("분 후,", false),
        ).inOrder()
    }
}
