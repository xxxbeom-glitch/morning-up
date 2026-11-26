package com.lumian.morningup.core.alarm.ring

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.model.engine.RingPhase
import com.lumian.morningup.core.model.engine.RingPlan
import com.lumian.morningup.core.model.engine.RingSession
import org.junit.Test

/** 결정 185: W2도 W3처럼 증기 사이렌을 뜰 때 한 번, 1분 중 30초 지점에 한 번 더 울려요. */
class ChimeNudgeTest {
    private val plan = RingPlan(
        alarmId = 7,
        missions = emptyList(),
        intervalMinutes = 5,
        ringAgainCount = 3,
        wakeCheckEnabled = true,
        wakeCheckAskAfterMinutes = 3,
        wakeCheckAnswerSeconds = 60,
    )

    private fun session(phase: RingPhase, endsAt: Long = 60_000L, paused: Long? = null) =
        RingSession(plan, phase, 3, 0, endsAt, pausedLeftMillis = paused)

    @Test
    fun label_w2AndW3_onlyWhileAsking() {
        assertThat(ChimeNudge.label(RingPhase.WakeCheckAsking)).isEqualTo("w2")
        assertThat(ChimeNudge.label(RingPhase.RecheckAsking)).isEqualTo("w3")
        assertThat(ChimeNudge.label(RingPhase.Ringing)).isNull()
        assertThat(ChimeNudge.label(null)).isNull()
    }

    @Test
    fun delay_w2_firesAtHalfOfOneMinute() {
        // 1분 대답 시간이 막 시작됐으면 30초 뒤에 한 번 더 울려요.
        assertThat(ChimeNudge.delayMillis(session(RingPhase.WakeCheckAsking), now = 0L)).isEqualTo(30_000L)
        assertThat(ChimeNudge.delayMillis(session(RingPhase.RecheckAsking), now = 10_000L)).isEqualTo(20_000L)
    }

    @Test
    fun delay_nullWhenPassedPausedOrNotAsking() {
        assertThat(ChimeNudge.delayMillis(session(RingPhase.WakeCheckAsking), now = 30_000L)).isNull()
        assertThat(ChimeNudge.delayMillis(session(RingPhase.WakeCheckAsking, paused = 40_000L), now = 0L)).isNull()
        assertThat(ChimeNudge.delayMillis(session(RingPhase.Ringing), now = 0L)).isNull()
        assertThat(ChimeNudge.delayMillis(null, now = 0L)).isNull()
    }
}
