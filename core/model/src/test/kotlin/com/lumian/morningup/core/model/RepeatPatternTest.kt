package com.lumian.morningup.core.model

import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import org.junit.Test

class RepeatPatternTest {
    @Test
    fun classifies() {
        assertThat(RepeatPattern.of(emptySet())).isEqualTo(RepeatPattern.Once)
        assertThat(RepeatPattern.of(setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)))
            .isEqualTo(RepeatPattern.Weekdays)
        assertThat(RepeatPattern.of(setOf(SATURDAY, SUNDAY))).isEqualTo(RepeatPattern.Weekend)
        assertThat(RepeatPattern.of(java.time.DayOfWeek.entries.toSet())).isEqualTo(RepeatPattern.Everyday)
        assertThat(RepeatPattern.of(setOf(FRIDAY, MONDAY))).isEqualTo(RepeatPattern.Custom(listOf(MONDAY, FRIDAY)))
    }

    @Test
    fun newAlarmDefaults_followSpec() {
        // dev-readiness 4-1: 새 알람은 미션 없이 시작. F9: 다시 잠들기 방지 처음 값은 1분 후, 답할 시간 1분.
        val alarm = Alarm(time = java.time.LocalTime.of(7, 0))
        assertThat(alarm.missions).isEmpty()
        assertThat(alarm.ringAgain).isEqualTo(RingAgain(intervalMinutes = 5, count = 3))
        assertThat(alarm.wakeCheck.askAfterMinutes).isEqualTo(3)
        assertThat(alarm.wakeCheck.answerSeconds).isEqualTo(60)
    }
}
