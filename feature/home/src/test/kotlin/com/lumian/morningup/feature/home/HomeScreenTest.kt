package com.lumian.morningup.feature.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.engine.NextAlarm
import com.lumian.morningup.core.model.engine.TimeUntil
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 11, 16)
    private val alarm = Alarm(id = 1, time = LocalTime.of(6, 30))

    private fun show(state: HomeUiState, onAdd: () -> Unit = {}, onLongPress: (Alarm) -> Unit = {}) {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                HomeScreen(
                    state = state,
                    onAdd = onAdd,
                    onOpen = {},
                    onToggle = { _, _ -> },
                    onLongPress = onLongPress,
                    onCancelDelete = {},
                    onConfirmDelete = {},
                    onUndo = {},
                    onDismissMessage = {},
                )
            }
        }
    }

    @Test
    fun empty_showsCreateButton_andHidesAdd() {
        var added = false
        show(HomeUiState(loading = false, today = today), onAdd = { added = true })
        compose.onNodeWithText("첫 알람을 만들어 볼까요?").assertExists()
        compose.onNodeWithContentDescription("알람 추가").assertDoesNotExist()
        compose.onNodeWithText("11월 16일 (월)").assertExists()
        compose.onNodeWithText("알람 만들기").performClick()
        assertThat(added).isTrue()
    }

    @Test
    fun list_showsHeaderAndCard_longPressAsksDelete() {
        var longPressed: Alarm? = null
        val at = ZonedDateTime.of(2026, 11, 17, 6, 30, 0, 0, ZoneId.of("Asia/Seoul"))
        show(
            HomeUiState(
                loading = false,
                alarms = listOf(alarm),
                next = NextAlarm(alarm, at, TimeUntil(0, 7, 20)),
                today = today,
            ),
            onLongPress = { longPressed = it },
        )
        compose.onNodeWithContentDescription("7시간 20분 후, 알림이 울립니다").assertExists()
        compose.onNodeWithText("11월 17일 (화) 오전 6:30").assertExists()
        compose.onNodeWithContentDescription("오전 6:30, 한 번").performTouchInput { longClick() }
        assertThat(longPressed).isEqualTo(alarm)
    }

    @Test
    fun pendingDelete_showsDialogText() {
        show(HomeUiState(loading = false, alarms = listOf(alarm), today = today, pendingDelete = listOf(alarm)))
        compose.onNodeWithText("이 알람을 삭제할까요?").assertExists()
        compose.onNodeWithText("오전 6:30 한 번 알람이에요.").assertExists()
    }

    @Test
    fun deletedMessage_showsUndo() {
        show(HomeUiState(loading = false, today = today, message = HomeMessage.Deleted(listOf(alarm))))
        compose.onNodeWithText("알람을 지웠어요").assertExists()
        compose.onNodeWithText("되돌리기").assertExists()
    }

    @Test
    fun restOfferAndResting_showOnCards() {
        val weekly = Alarm(id = 2, time = LocalTime.of(7, 0), repeatDays = setOf(DayOfWeek.MONDAY))
            .copy(enabled = false)
        val resting = Alarm(id = 3, time = LocalTime.of(8, 0), repeatDays = setOf(DayOfWeek.WEDNESDAY))
        show(
            HomeUiState(
                loading = false,
                alarms = listOf(resting, weekly),
                today = today,
                restOffers = setOf(2L),
                resting = mapOf(3L to DayOfWeek.WEDNESDAY),
            ),
        )
        compose.onNodeWithText("이번만 쉬기").assertExists()
        compose.onNodeWithText("수요일에 다시 울려요").assertExists()
    }

    @Test
    fun selectionMode_showsCountAndBar() {
        show(HomeUiState(loading = false, alarms = listOf(alarm), today = today, selection = setOf(1L)))
        compose.onNodeWithText("1개 선택됨").assertExists()
        compose.onNodeWithText("전체 선택").assertExists()
        compose.onNodeWithText("끄기").assertExists()
        compose.onNodeWithText("복사").assertExists()
        compose.onNodeWithText("삭제").assertExists()
    }
}
