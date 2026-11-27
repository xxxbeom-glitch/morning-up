package com.lumian.morningup.core.model.engine

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.engine.RingEffect.MarkPassed
import com.lumian.morningup.core.model.engine.RingEffect.PlaySound
import com.lumian.morningup.core.model.engine.RingEffect.RescheduleAlarm
import com.lumian.morningup.core.model.engine.RingEffect.ScheduleTimer
import com.lumian.morningup.core.model.engine.RingEffect.SessionEnded
import com.lumian.morningup.core.model.engine.RingEffect.ShowRingScreen
import com.lumian.morningup.core.model.engine.RingEffect.WakeResultRecorded
import kotlinx.serialization.json.Json
import org.junit.Test

/** 같은 시각, 가까운 시각 알람 겹침(결정 200, '더 엄격한 쪽을 남겨요'). */
class SameTimeAlarmsTest {
    /** 분의 시작(알람은 hh:mm:00에 울려요). */
    private val t0 = 1_800_000_000_000L
    private val sec = 1_000L
    private val min = 60 * sec
    private val ring = RingStateMachine.RING_MILLIS

    private val none = emptyList<MissionType>()
    private val one = listOf(MissionType.QuickMath)
    private val two = listOf(MissionType.QuickMath, MissionType.PopQuiz)

    private fun plan(
        id: Long,
        missions: List<MissionType> = none,
        count: Int? = 2,
        wakeCheck: Boolean = false,
        recheck: Boolean = false,
    ) = RingPlan(
        alarmId = id,
        missions = missions,
        intervalMinutes = 5,
        ringAgainCount = count,
        wakeCheckEnabled = wakeCheck,
        wakeCheckAskAfterMinutes = 3,
        wakeCheckAnswerSeconds = 60,
        recheckEnabled = recheck,
    )

    private fun reduce(s: RingSession?, e: RingEvent, now: Long) = RingStateMachine.reduce(s, e, now)

    private fun fire(s: RingSession?, p: RingPlan, now: Long) = reduce(s, RingEvent.AlarmFired(p), now)

    /** 미션을 모두 끝내거나 미션 없는 알람을 꺼요. */
    private fun dismiss(s: RingSession, now: Long): Transition {
        if (s.plan.missions.isEmpty()) return reduce(s, RingEvent.DismissWithoutMission, now)
        var t = reduce(s, RingEvent.MissionStarted, now)
        repeat(s.plan.missions.size) { t = reduce(t.session, RingEvent.MissionCompleted, now) }
        return t
    }

    // 1번: 같은 분 -----------------------------------------------------------

    @Test
    fun sameMinute_missionAlarmWins_overNoMissionAlarm() {
        // ISS-069: 미션 없는 알람이 먼저 와도 미션 있는 알람으로 울려요.
        val a = fire(null, plan(id = 1, missions = none), t0).session
        val t = fire(a, plan(id = 2, missions = one), t0 + sec)
        assertThat(t.session!!.plan.alarmId).isEqualTo(2)
        assertThat(t.session!!.phase).isEqualTo(RingPhase.Ringing)
        assertThat(t.session!!.suspended).isNull()
        assertThat(t.effects).containsExactly(
            MarkPassed(1),
            PlaySound(1f),
            ShowRingScreen,
            ScheduleTimer(t0 + sec + ring, TimerKind.RingTimeout),
            RescheduleAlarm(2),
        ).inOrder()
        // '알람 끄기'로는 꺼지지 않고 미션을 해야 꺼져요. 끝나면 함께 울린 알람도 같이 끝나요.
        assertThat(reduce(t.session, RingEvent.DismissWithoutMission, t0 + 2 * sec).session).isEqualTo(t.session)
        assertThat(dismiss(t.session!!, t0 + 5 * sec).session).isNull()
    }

    @Test
    fun sameMinute_noMissionAlarmArrivingLater_isPassed() {
        val a = fire(null, plan(id = 2, missions = one), t0).session
        val t = fire(a, plan(id = 1, missions = none), t0 + sec)
        assertThat(t.session).isEqualTo(a)
        assertThat(t.effects).containsExactly(MarkPassed(1), RescheduleAlarm(1))
    }

    @Test
    fun sameMinute_bothMissions_moreMissionsWins() {
        val a = fire(null, plan(id = 1, missions = one), t0).session
        val t = fire(a, plan(id = 2, missions = two), t0 + sec)
        assertThat(t.session!!.plan.alarmId).isEqualTo(2)
        assertThat(t.effects).contains(MarkPassed(1))
        // 미션 수가 적은 쪽이 뒤에 오면 그쪽이 지나감이에요.
        val b = fire(null, plan(id = 2, missions = two), t0).session
        assertThat(fire(b, plan(id = 1, missions = one), t0 + sec).session).isEqualTo(b)
    }

    @Test
    fun sameMinute_sameMissionCount_earlierCreatedAlarmWins() {
        // 먼저 만든 알람(id가 작은 쪽)의 미션 하나만 해요. 시스템이 나중 알람을 먼저 보내도 같아요.
        val later = fire(null, plan(id = 5, missions = one), t0).session
        val t = fire(later, plan(id = 3, missions = one), t0 + sec)
        assertThat(t.session!!.plan.alarmId).isEqualTo(3)
        assertThat(t.effects).contains(MarkPassed(5))
        val earlier = fire(null, plan(id = 3, missions = one), t0).session
        assertThat(fire(earlier, plan(id = 5, missions = one), t0 + sec).effects)
            .containsExactly(MarkPassed(5), RescheduleAlarm(5))
    }

    @Test
    fun sameMinute_noMissionsEither_keepsFirst() {
        val a = fire(null, plan(id = 5), t0).session
        assertThat(fire(a, plan(id = 3), t0 + sec).session).isEqualTo(a)
    }

    @Test
    fun nextMinute_whileStillRinging_isPassed_evenWithMoreMissions() {
        // 2번: 1분 뒤 알람은 앞 알람이 울리는 중이면 미션이 더 많아도 지나감이에요.
        val a = fire(null, plan(id = 1, missions = none), t0).session
        val t = fire(a, plan(id = 2, missions = two), t0 + min)
        assertThat(t.session).isEqualTo(a)
        assertThat(t.effects).containsExactly(MarkPassed(2), RescheduleAlarm(2))
    }

    // 2번: 울리거나 미션 중 ------------------------------------------------------

    @Test
    fun otherAlarmDuringMission_isPassed() {
        val a = reduce(fire(null, plan(id = 1, missions = one), t0).session, RingEvent.MissionStarted, t0).session
        val t = fire(a, plan(id = 2, missions = two), t0 + sec)
        assertThat(t.session).isEqualTo(a)
        assertThat(t.effects).containsExactly(MarkPassed(2), RescheduleAlarm(2))
    }

    // 3번: 쉬는 중 -------------------------------------------------------------

    private fun restingA(): RingSession {
        val a = fire(null, plan(id = 1, missions = one, count = 2), t0).session
        return reduce(a, RingEvent.TimerFired(TimerKind.RingTimeout), t0 + ring).session!!
    }

    @Test
    fun alarmDuringRest_ringsNew_thenRestingAlarmContinues() {
        val a = restingA()
        val restEnd = a.phaseEndsAt
        val bAt = t0 + ring + min
        val b = fire(a, plan(id = 2), bAt)
        assertThat(b.session!!.plan.alarmId).isEqualTo(2)
        assertThat(b.session!!.phase).isEqualTo(RingPhase.Ringing)
        assertThat(b.session!!.suspended).isEqualTo(a)
        // 앞 알람은 끝나지 않아요(예전에는 Cancelled로 사라졌어요).
        assertThat(b.effects.filterIsInstance<SessionEnded>()).isEmpty()
        assertThat(b.effects.filterIsInstance<WakeResultRecorded>()).isEmpty()

        val off = dismiss(b.session!!, bAt + 10 * sec)
        assertThat(off.session).isEqualTo(a)
        assertThat(off.effects).containsAtLeast(
            SessionEnded(2, RingOutcome.Dismissed),
            ScheduleTimer(restEnd, TimerKind.RestEnded),
        ).inOrder()

        // 쉬는 시간이 끝나면 앞 알람이 남은 횟수대로 다시 울려요.
        val again = reduce(off.session, RingEvent.TimerFired(TimerKind.RestEnded), restEnd)
        assertThat(again.session!!.plan.alarmId).isEqualTo(1)
        assertThat(again.session!!.phase).isEqualTo(RingPhase.Ringing)
        assertThat(again.session!!.ringsLeft).isEqualTo(1)
    }

    @Test
    fun alarmDuringRest_endingAfterRestTime_ringsRestingAlarmRightAway() {
        val a = restingA()
        val b = fire(a, plan(id = 2), t0 + ring + min).session!!
        val late = a.phaseEndsAt + min
        val off = reduce(b, RingEvent.DismissWithoutMission, late)
        assertThat(off.session!!.phaseEndsAt).isEqualTo(late)
        assertThat(off.effects.last()).isEqualTo(ScheduleTimer(late, TimerKind.RestEnded))
        assertThat(reduce(off.session, RingEvent.TimerFired(TimerKind.RestEnded), late).session!!.phase)
            .isEqualTo(RingPhase.Ringing)
    }

    @Test
    fun alarmDuringRest_newAlarmWaitingForWakeCheck_letsRestingAlarmRingFirst() {
        // 끼어든 알람이 W2를 기다리게 되면, 다시 울려야 하는 앞 알람을 먼저 이어가고 W2는 그 뒤에 물어요.
        val a = restingA()
        val bAt = t0 + ring + min
        val b = fire(a, plan(id = 2, missions = one, wakeCheck = true), bAt).session!!
        val done = dismiss(b, bAt + 10 * sec)
        assertThat(done.session!!.plan.alarmId).isEqualTo(1)
        assertThat(done.session!!.phase).isEqualTo(RingPhase.Resting)
        assertThat(done.session!!.suspended!!.plan.alarmId).isEqualTo(2)
        assertThat(done.session!!.suspended!!.phase).isEqualTo(RingPhase.AwaitingWakeCheck)
        assertThat(done.effects.last()).isEqualTo(ScheduleTimer(a.phaseEndsAt, TimerKind.RestEnded))

        // 앞 알람을 끄면 끼어든 알람의 W2를 이어서 물어요.
        val rang = reduce(done.session, RingEvent.TimerFired(TimerKind.RestEnded), a.phaseEndsAt).session!!
        val aOff = dismiss(rang, a.phaseEndsAt + 10 * sec)
        assertThat(aOff.session!!.plan.alarmId).isEqualTo(2)
        assertThat(aOff.session!!.phase).isEqualTo(RingPhase.AwaitingWakeCheck)
        assertThat(aOff.effects).contains(SessionEnded(1, RingOutcome.Dismissed))
    }

    // 4번: 확인(W2, W3) 기다림 ---------------------------------------------------

    private fun awaitingWakeCheckA(): RingSession =
        dismiss(fire(null, plan(id = 1, missions = one, wakeCheck = true), t0).session!!, t0 + 10 * sec).session!!

    @Test
    fun alarmDuringWakeCheckWait_keepsCheck_andAsksAfterNewAlarmEnds() {
        val a = awaitingWakeCheckA()
        assertThat(a.phase).isEqualTo(RingPhase.AwaitingWakeCheck)
        val bAt = t0 + min
        val b = fire(a, plan(id = 2), bAt)
        assertThat(b.session!!.plan.alarmId).isEqualTo(2)
        assertThat(b.session!!.suspended).isEqualTo(a)
        assertThat(b.effects).doesNotContain(WakeResultRecorded(1, WakeResult.Cancelled, 0))
        assertThat(b.effects).doesNotContain(SessionEnded(1, RingOutcome.Cancelled))

        // B가 끝나면 A의 W2 기다림을 이어가요. 이미 지난 때면 바로 물어요.
        val late = a.phaseEndsAt + min
        val off = reduce(b.session, RingEvent.DismissWithoutMission, late)
        assertThat(off.session!!.plan.alarmId).isEqualTo(1)
        assertThat(off.session!!.phase).isEqualTo(RingPhase.AwaitingWakeCheck)
        assertThat(off.effects.last()).isEqualTo(ScheduleTimer(late, TimerKind.WakeCheckAsk))
        val asked = reduce(off.session, RingEvent.TimerFired(TimerKind.WakeCheckAsk), late)
        assertThat(asked.session!!.phase).isEqualTo(RingPhase.WakeCheckAsking)
    }

    @Test
    fun alarmWhileAskingW2_asksAgainWithFullTimeAfterNewAlarmEnds() {
        val a = awaitingWakeCheckA()
        val askAt = a.phaseEndsAt
        val asking = reduce(a, RingEvent.TimerFired(TimerKind.WakeCheckAsk), askAt).session!!
        val b = fire(asking, plan(id = 2), askAt + 20 * sec).session!!
        assertThat(b.suspended!!.phase).isEqualTo(RingPhase.AwaitingWakeCheck)
        val endAt = askAt + min
        val off = reduce(b, RingEvent.DismissWithoutMission, endAt)
        assertThat(off.effects.last()).isEqualTo(ScheduleTimer(endAt, TimerKind.WakeCheckAsk))
    }

    @Test
    fun alarmDuringRecheckWait_keepsW3() {
        val p = plan(id = 1, missions = one, wakeCheck = true, recheck = true)
        val a0 = dismiss(fire(null, p, t0).session!!, t0 + 10 * sec).session!!
        val asking = reduce(a0, RingEvent.TimerFired(TimerKind.WakeCheckAsk), a0.phaseEndsAt).session
        val a = reduce(asking, RingEvent.WakeCheckConfirmed, a0.phaseEndsAt + 5 * sec).session!!
        assertThat(a.phase).isEqualTo(RingPhase.AwaitingRecheck)
        val bAt = a0.phaseEndsAt + min
        val b = fire(a, plan(id = 2), bAt).session!!
        val off = reduce(b, RingEvent.DismissWithoutMission, bAt + 10 * sec)
        assertThat(off.session).isEqualTo(a)
        assertThat(off.effects.last()).isEqualTo(ScheduleTimer(a.phaseEndsAt, TimerKind.RecheckAsk))
    }

    @Test
    fun cancelChecks_ofSuspendedAlarm_dropsIt_withoutTouchingRingingOne() {
        // 멈춰 둔 알람을 끄거나 지우면 그 확인도 멈춰요. 지금 울리는 알람은 그대로예요.
        val a = awaitingWakeCheckA()
        val b = fire(a, plan(id = 2), t0 + min).session!!
        val t = reduce(b, RingEvent.CancelChecks(1), t0 + min + sec)
        assertThat(t.session).isEqualTo(b.copy(suspended = null))
        assertThat(t.effects).containsExactly(WakeResultRecorded(1, WakeResult.Cancelled, 0))
        assertThat(reduce(t.session, RingEvent.DismissWithoutMission, t0 + 2 * min).session).isNull()
    }

    // 5번: 완전히 끈 뒤 ----------------------------------------------------------

    @Test
    fun alarmAfterFirstFullyDismissed_ringsNormally() {
        val a = fire(null, plan(id = 1, missions = one), t0).session!!
        assertThat(dismiss(a, t0 + 10 * sec).session).isNull()
        val t = fire(null, plan(id = 2), t0 + min)
        assertThat(t.session!!.plan.alarmId).isEqualTo(2)
        assertThat(t.session!!.suspended).isNull()
        assertThat(t.effects).containsExactly(
            PlaySound(1f),
            ShowRingScreen,
            ScheduleTimer(t0 + min + ring, TimerKind.RingTimeout),
            RescheduleAlarm(2),
        ).inOrder()
    }

    // 저장 ---------------------------------------------------------------------

    @Test
    fun suspendedAlarm_survivesSaveAndLoad() {
        val b = fire(restingA(), plan(id = 2), t0 + ring + min).session!!
        val json = Json.encodeToString(RingSession.serializer(), b)
        assertThat(Json.decodeFromString(RingSession.serializer(), json)).isEqualTo(b)
    }
}
