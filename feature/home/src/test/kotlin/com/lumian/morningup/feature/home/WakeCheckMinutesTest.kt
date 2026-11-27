package com.lumian.morningup.feature.home

import com.google.common.truth.Truth.assertThat
import java.time.LocalTime
import org.junit.Test

/** W2 대기 띠 '재수면 여부를 n분 후 재확인할게요'의 n(TSK-285). */
class WakeCheckMinutesTest {
    @Test
    fun roundsUp() {
        assertThat(wakeCheckMinutesLeft(LocalTime.of(6, 40, 0), LocalTime.of(6, 43))).isEqualTo(3)
        assertThat(wakeCheckMinutesLeft(LocalTime.of(6, 40, 30), LocalTime.of(6, 43))).isEqualTo(3)
        assertThat(wakeCheckMinutesLeft(LocalTime.of(6, 41, 59), LocalTime.of(6, 43))).isEqualTo(2)
    }

    @Test
    fun underOneMinuteIsZero() {
        assertThat(wakeCheckMinutesLeft(LocalTime.of(6, 42, 30), LocalTime.of(6, 43))).isEqualTo(0)
        assertThat(wakeCheckMinutesLeft(LocalTime.of(6, 43, 20), LocalTime.of(6, 43))).isEqualTo(0)
    }

    @Test
    fun crossesMidnight() {
        assertThat(wakeCheckMinutesLeft(LocalTime.of(23, 58), LocalTime.of(0, 1))).isEqualTo(3)
    }
}
