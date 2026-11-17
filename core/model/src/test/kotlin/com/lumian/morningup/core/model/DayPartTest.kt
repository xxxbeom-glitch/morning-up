package com.lumian.morningup.core.model

import com.google.common.truth.Truth.assertThat
import java.time.LocalTime
import org.junit.Test

class DayPartTest {
    @Test
    fun boundaries() {
        assertThat(DayPart.of(LocalTime.of(3, 59))).isEqualTo(DayPart.Night)
        assertThat(DayPart.of(LocalTime.of(4, 0))).isEqualTo(DayPart.Morning)
        assertThat(DayPart.of(LocalTime.of(6, 30))).isEqualTo(DayPart.Morning)
        assertThat(DayPart.of(LocalTime.of(17, 59))).isEqualTo(DayPart.Morning)
        assertThat(DayPart.of(LocalTime.of(18, 0))).isEqualTo(DayPart.Night)
        assertThat(DayPart.of(LocalTime.MIDNIGHT)).isEqualTo(DayPart.Night)
    }
}
