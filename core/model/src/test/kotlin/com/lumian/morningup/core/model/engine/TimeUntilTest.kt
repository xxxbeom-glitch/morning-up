package com.lumian.morningup.core.model.engine

import com.google.common.truth.Truth.assertThat
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Test

class TimeUntilTest {
    private val seoul = ZoneId.of("Asia/Seoul")

    private fun at(d: Int, h: Int, m: Int, s: Int = 0, nanos: Int = 0) =
        ZonedDateTime.of(2026, 11, d, h, m, s, nanos, seoul)

    @Test
    fun exactMinutes_stayExact() {
        assertThat(TimeUntil.between(at(16, 23, 10), at(17, 6, 30))).isEqualTo(TimeUntil(0, 7, 20))
    }

    @Test
    fun partialMinute_roundsUp() {
        // 23:10:01부터 06:30까지는 7시간 19분 59초예요. 올려서 20분으로 보여 줘요(울리기 전에 '0분'이 나오지 않게).
        assertThat(TimeUntil.between(at(16, 23, 10, 1), at(17, 6, 30))).isEqualTo(TimeUntil(0, 7, 20))
        assertThat(TimeUntil.between(at(16, 23, 10, 0, 1), at(17, 6, 30))).isEqualTo(TimeUntil(0, 7, 20))
    }

    @Test
    fun underOneMinute_isOneMinute() {
        assertThat(TimeUntil.between(at(17, 6, 29, 59), at(17, 6, 30))).isEqualTo(TimeUntil(0, 0, 1))
    }

    @Test
    fun pastOrNow_isZero() {
        assertThat(TimeUntil.between(at(17, 6, 30), at(17, 6, 30))).isEqualTo(TimeUntil(0, 0, 0))
        assertThat(TimeUntil.between(at(17, 6, 31), at(17, 6, 30))).isEqualTo(TimeUntil(0, 0, 0))
    }

    @Test
    fun overADay_showsDays() {
        // H8-3 시안: '1일 7시간 50분 후'
        assertThat(TimeUntil.between(at(16, 22, 40), at(18, 6, 30))).isEqualTo(TimeUntil(1, 7, 50))
    }

    @Test
    fun exactlyOneDay_roundsToDays() {
        assertThat(TimeUntil.between(at(16, 6, 30), at(17, 6, 30))).isEqualTo(TimeUntil(1, 0, 0))
    }

    @Test
    fun roundingUp_carriesIntoHours() {
        // 59분 30초는 올리면 1시간이에요.
        assertThat(TimeUntil.between(at(17, 5, 30, 30), at(17, 6, 30))).isEqualTo(TimeUntil(0, 1, 0))
    }
}
