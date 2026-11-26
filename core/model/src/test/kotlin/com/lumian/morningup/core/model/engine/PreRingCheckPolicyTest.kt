package com.lumian.morningup.core.model.engine

import com.google.common.truth.Truth.assertThat
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Test

class PreRingCheckPolicyTest {
    private val zone = ZoneId.of("Asia/Seoul")
    private fun at(hour: Int, minute: Int, second: Int = 0) =
        ZonedDateTime.of(2026, 11, 27, hour, minute, second, 0, zone)

    @Test
    fun checkAt_isTenMinutesBefore() {
        assertThat(PreRingCheckPolicy.checkAt(at(6, 30))).isEqualTo(at(6, 20))
        assertThat(PreRingCheckPolicy.checkAt(null)).isNull()
    }

    @Test
    fun searchFrom_skipsAlarmWithinTenMinutes_andTheOneJustChecked() {
        // 6:20 점검이 끝난 때 찾으면 6:30 알람은 건너뛰고 그다음을 봐요.
        assertThat(PreRingCheckPolicy.searchFrom(at(6, 20))).isEqualTo(at(6, 30, 1))
    }
}
