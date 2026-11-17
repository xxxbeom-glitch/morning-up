package com.lumian.morningup.core.ui

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.engine.TimeUntil
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AlarmTextTest {
    private val res = ApplicationProvider.getApplicationContext<android.content.Context>().resources

    @Test
    fun time_twelveHour() {
        assertThat(AlarmText.time(res, LocalTime.of(6, 30))).isEqualTo("오전 6:30")
        assertThat(AlarmText.time(res, LocalTime.of(0, 5))).isEqualTo("오전 12:05")
        assertThat(AlarmText.time(res, LocalTime.of(12, 0))).isEqualTo("오후 12:00")
        assertThat(AlarmText.time(res, LocalTime.of(19, 10))).isEqualTo("오후 7:10")
    }

    @Test
    fun until_dropsZeroUnits() {
        assertThat(AlarmText.until(res, TimeUntil(0, 7, 20))).isEqualTo("7시간 20분")
        assertThat(AlarmText.until(res, TimeUntil(1, 7, 50))).isEqualTo("1일 7시간 50분")
        assertThat(AlarmText.until(res, TimeUntil(0, 0, 20))).isEqualTo("20분")
        assertThat(AlarmText.until(res, TimeUntil(0, 7, 0))).isEqualTo("7시간")
        assertThat(AlarmText.until(res, TimeUntil(0, 0, 0))).isEqualTo("0분")
    }

    @Test
    fun date_andRepeat() {
        assertThat(AlarmText.date(res, LocalDate.of(2026, 11, 16))).isEqualTo("11월 16일 (월)")
        val weekdays =
            setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        assertThat(AlarmText.repeat(res, weekdays)).isEqualTo("평일")
        assertThat(AlarmText.repeat(res, setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY))).isEqualTo("월 금")
        assertThat(AlarmText.repeat(res, emptySet())).isEqualTo("한 번")
    }

    @Test
    fun missions_summary() {
        assertThat(AlarmText.missions(res, emptyList())).isEqualTo("없음")
        assertThat(AlarmText.missions(res, listOf(MissionType.QuickMath))).isEqualTo("Quick Math")
        assertThat(AlarmText.missions(res, listOf(MissionType.DropWords, MissionType.PopQuiz)))
            .isEqualTo("Drop Words 외 1개")
    }

    @Test
    fun dateLong_durationAndCountdown() {
        assertThat(AlarmText.dateLong(res, LocalDate.of(2026, 11, 16))).isEqualTo("11월 16일 월요일")
        assertThat(AlarmText.duration(res, 30)).isEqualTo("30초")
        assertThat(AlarmText.duration(res, 60)).isEqualTo("1분")
        assertThat(AlarmText.duration(res, 90)).isEqualTo("1분 30초")
        assertThat(AlarmText.duration(res, 120)).isEqualTo("2분")
        assertThat(AlarmText.countdown(res, 42_000)).isEqualTo("0:42")
        assertThat(AlarmText.countdown(res, 41_001)).isEqualTo("0:42")
        assertThat(AlarmText.countdown(res, 65_000)).isEqualTo("1:05")
        assertThat(AlarmText.countdown(res, -5)).isEqualTo("0:00")
    }
}
