package com.lumian.morningup.core.model.engine

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.Snooze
import com.lumian.morningup.core.model.WakeCheck
import com.lumian.morningup.core.model.engine.RingEffect.PlaySound
import com.lumian.morningup.core.model.engine.RingEffect.ScheduleTimer
import com.lumian.morningup.core.model.engine.RingEffect.ShowRingScreen
import com.lumian.morningup.core.model.engine.RingEffect.SnoozeStarted
import com.lumian.morningup.core.model.engine.RingEffect.StopSound
import org.junit.Test
import java.time.LocalTime

/** 미루기(결정 195, TSK-282). */
class SnoozeTest {
    private val t0 = 1_800_000_000_000L
    private val min = 60_000L

    private fun alarm(snooze: Snooze, wakeCheck: Boolean = false) = Alarm(
        id = 7,
        time = LocalTime.of(6, 30),
        missions = listOf(MissionSetting.default(MissionType.QuickMath)),
        snooze = snooze,
        wakeCheck = WakeCheck(enabled = wakeCheck),
    )

    private fun fire(snooze: Snooze, wakeCheck: Boolean = false) =
        RingStateMachine.reduce(null, RingEvent.AlarmFired(RingPlan.from(alarm(snooze, wakeCheck))), t0).session!!

    private fun snooze(s: RingSession, at: Long) = RingStateMachine.reduce(s, RingEvent.Snoozed, at)

    @Test
    fun newAlarm_snoozeIsOff_withFiveMinutesThreeTimes() {
        val s = Alarm(time = LocalTime.of(6, 0)).snooze
        assertThat(s.enabled).isFalse()
        assertThat(s.minutes).isEqualTo(5)
        assertThat(s.count).isEqualTo(3)
    }

    @Test
    fun normalized_fixesUnknownMinutes_andCountBelowOne() {
        val s = Snooze(enabled = true, minutes = 7, count = 0).normalized()
        assertThat(s.minutes).isEqualTo(Snooze.DEFAULT_MINUTES)
        assertThat(s.count).isEqualTo(1)
    }

    @Test
    fun snoozeOff_noButton_andEventIgnored() {
        val s = fire(Snooze(enabled = false))
        assertThat(s.canSnooze).isFalse()
        val t = snooze(s, t0 + 10_000)
        assertThat(t.session).isEqualTo(s)
        assertThat(t.effects).isEmpty()
    }

    @Test
    fun snooze_stopsSound_andRingsAgainAfterInterval() {
        val s = fire(Snooze(enabled = true, minutes = 3, count = 2))
        assertThat(s.canSnooze).isTrue()
        val at = t0 + 10_000
        val t = snooze(s, at)
        val rest = t.session!!
        assertThat(rest.phase).isEqualTo(RingPhase.Resting)
        assertThat(rest.snoozed).isTrue()
        assertThat(rest.snoozesLeft).isEqualTo(1)
        // 다시 울림 횟수는 줄지 않아요.
        assertThat(rest.ringsLeft).isEqualTo(s.ringsLeft)
        assertThat(t.effects).containsExactly(
            StopSound,
            SnoozeStarted(at + 3 * min, 3, 1),
            ScheduleTimer(at + 3 * min, TimerKind.RestEnded),
        ).inOrder()
        // 쉬는 중에는 버튼이 없어요(이미 미룬 중).
        assertThat(rest.canSnooze).isFalse()

        val again = RingStateMachine.reduce(rest, RingEvent.TimerFired(TimerKind.RestEnded), at + 3 * min)
        assertThat(again.session!!.phase).isEqualTo(RingPhase.Ringing)
        assertThat(again.session!!.snoozed).isFalse()
        assertThat(again.session!!.canSnooze).isTrue()
        assertThat(again.effects).containsAtLeast(PlaySound(1f), ShowRingScreen)
    }

    @Test
    fun usingAllSnoozes_hidesButton() {
        var s = fire(Snooze(enabled = true, minutes = 1, count = 1))
        var now = t0 + 1_000
        s = snooze(s, now).session!!
        now += min
        s = RingStateMachine.reduce(s, RingEvent.TimerFired(TimerKind.RestEnded), now).session!!
        assertThat(s.phase).isEqualTo(RingPhase.Ringing)
        assertThat(s.snoozesLeft).isEqualTo(0)
        assertThat(s.canSnooze).isFalse()
        assertThat(snooze(s, now + 1_000).effects).isEmpty()
    }

    @Test
    fun missionStillPossible_whileSnoozed_earlyFinish() {
        val s = snooze(fire(Snooze(enabled = true)), t0 + 1_000).session!!
        val t = RingStateMachine.reduce(s, RingEvent.MissionStarted, t0 + 2_000)
        assertThat(t.session!!.phase).isEqualTo(RingPhase.InMission)
    }

    @Test
    fun noSnooze_duringMission_orWakeCheck() {
        val ringing = fire(Snooze(enabled = true), wakeCheck = true)
        val mission = RingStateMachine.reduce(ringing, RingEvent.MissionStarted, t0 + 1_000).session!!
        assertThat(mission.canSnooze).isFalse()
        assertThat(snooze(mission, t0 + 2_000).effects).isEmpty()
        // 미션을 끝내면 W2 기다림. 여기서도 미루기는 받지 않아요(결정 195).
        val waiting = RingStateMachine.reduce(mission, RingEvent.MissionCompleted, t0 + 3_000).session!!
        assertThat(waiting.phase).isEqualTo(RingPhase.AwaitingWakeCheck)
        assertThat(snooze(waiting, t0 + 4_000).effects).isEmpty()
    }

    @Test
    fun ringAgainRest_isNotSnoozed() {
        val s = fire(Snooze(enabled = true))
        val t = RingStateMachine.reduce(s, RingEvent.TimerFired(TimerKind.RingTimeout), t0 + RingStateMachine.RING_MILLIS)
        assertThat(t.session!!.phase).isEqualTo(RingPhase.Resting)
        assertThat(t.session!!.snoozed).isFalse()
        assertThat(t.session!!.snoozesLeft).isEqualTo(3)
    }

    @Test
    fun snoozedEvent_hasStableKey() {
        assertThat(RingEvent.keyOf(RingEvent.Snoozed)).isEqualTo("snoozed")
    }
}
