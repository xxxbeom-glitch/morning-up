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

    private fun show(
        state: HomeUiState,
        onAdd: () -> Unit = {},
        onLongPress: (Alarm) -> Unit = {},
        onOpen: (Alarm) -> Unit = {},
    ) {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                HomeScreen(
                    state = state,
                    onAdd = onAdd,
                    onOpen = onOpen,
                    onToggle = { _, _ -> },
                    onLongPress = onLongPress,
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
    fun header_click_opensNextAlarm() {
        // 결정 157: 위 문구를 누르면 다음에 울릴 알람(목록 첫 카드가 아니어도)의 편집이 열려요.
        val other = Alarm(id = 2, time = LocalTime.of(5, 0))
        var opened: Alarm? = null
        val at = ZonedDateTime.of(2026, 11, 17, 6, 30, 0, 0, ZoneId.of("Asia/Seoul"))
        show(
            HomeUiState(
                loading = false,
                alarms = listOf(other, alarm),
                next = NextAlarm(alarm, at, TimeUntil(0, 7, 20)),
                today = today,
            ),
            onOpen = { opened = it },
        )
        compose.onNodeWithText("11월 17일 (화) 오전 6:30").performClick()
        assertThat(opened).isEqualTo(alarm)
    }

    @Test
    fun savedToast_restReleased_saysRestReleased() {
        show(
            HomeUiState(
                loading = false,
                alarms = listOf(alarm),
                today = today,
                message = HomeMessage.Saved(TimeUntil(0, 7, 20), restReleased = true),
            ),
        )
        compose.onNodeWithText("이번만 쉬기를 풀었어요. 7시간 20분 뒤에 울려요").assertExists()
    }

    @Test
    fun selectionBar_showsRest_onlyWhenRestable() {
        val on = alarm.copy(repeatDays = setOf(DayOfWeek.SATURDAY))
        val state = HomeUiState(loading = false, alarms = listOf(on), today = today, selection = setOf(1L))
        show(state.copy(restable = setOf(1L)))
        compose.onNodeWithText("이번만 쉬기").assertExists()
    }

    @Test
    fun selectionBar_hidesRest_whenNothingToRest() {
        val on = alarm.copy(repeatDays = setOf(DayOfWeek.SATURDAY))
        show(HomeUiState(loading = false, alarms = listOf(on), today = today, selection = setOf(1L)))
        compose.onNodeWithText("이번만 쉬기").assertDoesNotExist()
    }

    @Test
    fun header_withoutNext_doesNothing() {
        var opened: Alarm? = null
        show(
            HomeUiState(loading = false, alarms = listOf(alarm.copy(enabled = false)), today = today),
            onOpen = { opened = it },
        )
        compose.onNodeWithText("11월 16일 (월)").performClick()
        assertThat(opened).isNull()
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
    fun deletedMany_showsCount() {
        val two = listOf(alarm, alarm.copy(id = 2))
        show(HomeUiState(loading = false, today = today, message = HomeMessage.Deleted(two)))
        compose.onNodeWithText("알람 2개를 지웠어요").assertExists()
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
                resting = mapOf(3L to RestLabel.OnDay(DayOfWeek.WEDNESDAY)),
            ),
        )
        compose.onNodeWithText("이번만 쉬기").assertExists()
        compose.onNodeWithText("수요일에 다시 울려요").assertExists()
    }

    @Test
    fun resting_withinADay_showsHoursAndMinutes() {
        val a = Alarm(id = 4, time = LocalTime.of(6, 30), repeatDays = setOf(DayOfWeek.THURSDAY))
        val b = Alarm(id = 5, time = LocalTime.of(7, 0), repeatDays = setOf(DayOfWeek.THURSDAY))
        show(
            HomeUiState(
                loading = false,
                alarms = listOf(a, b),
                today = today,
                resting = mapOf(
                    4L to RestLabel.Within(TimeUntil(0, 19, 43)),
                    5L to RestLabel.Within(TimeUntil(0, 0, 12)),
                ),
            ),
        )
        compose.onNodeWithText("19시간 43분 뒤 다시 울려요").assertExists()
        compose.onNodeWithText("12분 뒤 다시 울려요").assertExists()
    }

    @Test
    fun selectionMode_showsCountAndBar() {
        show(HomeUiState(loading = false, alarms = listOf(alarm), today = today, selection = setOf(1L)))
        compose.onNodeWithText("1개 선택됨").assertExists()
        compose.onNodeWithText("전체 선택").assertExists()
        compose.onNodeWithText("끄기").assertExists()
        compose.onNodeWithText("복제").assertExists()
        compose.onNodeWithText("삭제").assertExists()
    }
}
