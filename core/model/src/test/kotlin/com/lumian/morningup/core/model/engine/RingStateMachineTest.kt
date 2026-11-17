package com.lumian.morningup.core.model.engine

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.engine.RingEffect.CancelTimer
import com.lumian.morningup.core.model.engine.RingEffect.MarkPassed
import com.lumian.morningup.core.model.engine.RingEffect.PlaySound
import com.lumian.morningup.core.model.engine.RingEffect.PostMissedNotification
import com.lumian.morningup.core.model.engine.RingEffect.RescheduleAlarm
import com.lumian.morningup.core.model.engine.RingEffect.ScheduleTimer
import com.lumian.morningup.core.model.engine.RingEffect.SessionEnded
import com.lumian.morningup.core.model.engine.RingEffect.ShowMission
import com.lumian.morningup.core.model.engine.RingEffect.ShowRingScreen
import com.lumian.morningup.core.model.engine.RingEffect.ShowWakeCheck
import com.lumian.morningup.core.model.engine.RingEffect.ShowWakeUp
import com.lumian.morningup.core.model.engine.RingEffect.StopSound
import org.junit.Test

class RingStateMachineTest {
    private val t0 = 1_800_000_000_000L
    private val sec = 1_000L
    private val min = 60 * sec

    private fun plan(
        id: Long = 1,
        missions: List<MissionType> = listOf(MissionType.QuickMath, MissionType.PopQuiz),
        interval: Int = 5,
        count: Int? = 2,
        wakeCheck: Boolean = false,
    ) = RingPlan(
        alarmId = id,
        missions = missions,
        intervalMinutes = interval,
        ringAgainCount = count,
        wakeCheckEnabled = wakeCheck,
        wakeCheckAskAfterMinutes = 3,
        wakeCheckAnswerSeconds = 60,
    )

    private fun reduce(s: RingSession?, e: RingEvent, now: Long) = RingStateMachine.reduce(s, e, now)

    private fun fire(p: RingPlan = plan()) = reduce(null, RingEvent.AlarmFired(p), t0)

    // 알람 시각 ------------------------------------------------------------

    @Test
    fun alarmFired_startsRinging_fullVolume_andOneMinuteTimer() {
        val t = fire()
        assertThat(t.session!!.phase).isEqualTo(RingPhase.Ringing)
        assertThat(t.session!!.ringsLeft).isEqualTo(2)
        assertThat(t.effects).containsAtLeast(
            PlaySound(1f),
            ShowRingScreen,
            ScheduleTimer(t0 + 60 * sec, TimerKind.RingTimeout),
            RescheduleAlarm(1),
        )
    }

    // 아무도 안 끔 ----------------------------------------------------------

    @Test
    fun ringTimeout_withRingsLeft_restsForInterval_andUsesOneRing() {
        val t = reduce(fire().session, RingEvent.TimerFired(TimerKind.RingTimeout), t0 + 60 * sec)
        assertThat(t.session!!.phase).isEqualTo(RingPhase.Resting)
        assertThat(t.session!!.ringsLeft).isEqualTo(1)
        assertThat(t.effects).containsAtLeast(StopSound, ScheduleTimer(t0 + 60 * sec + 5 * min, TimerKind.RestEnded))
    }

    @Test
    fun ringTimeout_rest_closesScreen_andTellsNextRing() {
        // 다시 2번이면 1번째 울림 뒤 쉬기 → 다음은 2번째, 2번째 뒤 쉬기 → 다음은 3번째(결정 70)
        val first = reduce(fire(plan(count = 2)).session, RingEvent.TimerFired(TimerKind.RingTimeout), t0 + min)
        assertThat(first.effects).contains(RingEffect.RestStarted(t0 + min + 5 * min, 5, 2))
        val ringing = reduce(first.session, RingEvent.TimerFired(TimerKind.RestEnded), t0 + 6 * min).session
        val second = reduce(ringing, RingEvent.TimerFired(TimerKind.RingTimeout), t0 + 7 * min)
        assertThat(second.effects).contains(RingEffect.RestStarted(t0 + 12 * min, 5, 3))
        // 끌 때까지는 셀 수 없어 번째가 없어요.
        val endless = reduce(fire(plan(count = null)).session, RingEvent.TimerFired(TimerKind.RingTimeout), t0 + min)
        assertThat(endless.effects).contains(RingEffect.RestStarted(t0 + 6 * min, 5, null))
    }

    @Test
    fun restEnded_ringsAgain() {
        var s = reduce(fire().session, RingEvent.TimerFired(TimerKind.RingTimeout), t0 + min).session
        val now = t0 + min + 5 * min
        val t = reduce(s, RingEvent.TimerFired(TimerKind.RestEnded), now)
        s = t.session
        assertThat(s!!.phase).isEqualTo(RingPhase.Ringing)
        assertThat(t.effects)
            .containsAtLeast(PlaySound(1f), ShowRingScreen, ScheduleTimer(now + min, TimerKind.RingTimeout))
    }

    @Test
    fun nobodyDismisses_ringsCountPlusOneTimes_thenMissed() {
        // 처음 1번 + 다시 2번 = 3번 울리고 놓침
        var s = fire(plan(count = 2)).session
        var now = t0
        var rings = 1
        var last: Transition? = null
        repeat(10) {
            if (s == null) return@repeat
            now += min
            last = reduce(s, RingEvent.TimerFired(TimerKind.RingTimeout), now)
            s = last!!.session
            if (s?.phase == RingPhase.Resting) {
                now += 5 * min
                s = reduce(s, RingEvent.TimerFired(TimerKind.RestEnded), now).session
                rings++
            }
        }
        assertThat(rings).isEqualTo(3)
        assertThat(s).isNull()
        assertThat(last!!.effects)
            .containsAtLeast(StopSound, PostMissedNotification(1), SessionEnded(1, RingOutcome.Missed))
    }

    @Test
    fun ringAgainZero_missedAfterFirstMinute() {
        val t = reduce(fire(plan(count = 0)).session, RingEvent.TimerFired(TimerKind.RingTimeout), t0 + min)
        assertThat(t.session).isNull()
        assertThat(t.effects).contains(PostMissedNotification(1))
    }

    @Test
    fun untilDismissed_neverMisses() {
        var s = fire(plan(count = null)).session
        var now = t0
        repeat(50) {
            now += min
            s = reduce(s, RingEvent.TimerFired(TimerKind.RingTimeout), now).session
            assertThat(s!!.phase).isEqualTo(RingPhase.Resting)
            now += 5 * min
            s = reduce(s, RingEvent.TimerFired(TimerKind.RestEnded), now).session
        }
        assertThat(s!!.phase).isEqualTo(RingPhase.Ringing)
    }

    // 미션 ------------------------------------------------------------------

    @Test
    fun missionStarted_stopsSound_and59sTimer() {
        val t = reduce(fire().session, RingEvent.MissionStarted, t0 + 10 * sec)
        assertThat(t.session!!.phase).isEqualTo(RingPhase.InMission)
        assertThat(t.effects).containsAtLeast(
            StopSound,
            ShowMission(0),
            ScheduleTimer(t0 + 10 * sec + 59 * sec, TimerKind.MissionTimeout),
        )
    }

    @Test
    fun missionStarted_fromResting_isAllowed() {
        val resting = reduce(fire().session, RingEvent.TimerFired(TimerKind.RingTimeout), t0 + min).session
        val t = reduce(resting, RingEvent.MissionStarted, t0 + 2 * min)
        assertThat(t.session!!.phase).isEqualTo(RingPhase.InMission)
    }

    @Test
    fun missionInteraction_refills59s() {
        val s = reduce(fire().session, RingEvent.MissionStarted, t0).session
        val t = reduce(s, RingEvent.MissionInteraction, t0 + 40 * sec)
        assertThat(t.session!!.phaseEndsAt).isEqualTo(t0 + 99 * sec)
        assertThat(t.effects).contains(ScheduleTimer(t0 + 99 * sec, TimerKind.MissionTimeout))
    }

    @Test
    fun missionTimeout_ringsAgain_withoutUsingRingCount_keepingDoneMissions() {
        var s = reduce(fire().session, RingEvent.MissionStarted, t0).session
        s = reduce(s, RingEvent.MissionCompleted, t0 + 20 * sec).session
        assertThat(s!!.missionsDone).isEqualTo(1)
        val t = reduce(s, RingEvent.TimerFired(TimerKind.MissionTimeout), t0 + 79 * sec)
        assertThat(t.session!!.phase).isEqualTo(RingPhase.Ringing)
        assertThat(t.session!!.ringsLeft).isEqualTo(2)
        assertThat(t.session!!.missionsDone).isEqualTo(1)
        assertThat(t.effects).contains(PlaySound(1f))
        // 다시 시작하면 두 번째 미션부터
        assertThat(reduce(t.session, RingEvent.MissionStarted, t0 + 90 * sec).effects).contains(ShowMission(1))
    }

    @Test
    fun threeMissions_runInOrder_andTimeoutInMiddle_resumesFromUnfinishedOne() {
        val three = plan(missions = listOf(MissionType.DropWords, MissionType.PopQuiz, MissionType.ShakeShake))
        var t = reduce(fire(three).session, RingEvent.MissionStarted, t0)
        assertThat(t.effects).contains(ShowMission(0))
        t = reduce(t.session, RingEvent.MissionCompleted, t0 + 10 * sec)
        assertThat(t.effects).contains(ShowMission(1))
        // 두 번째 미션 중 손을 놓아 59초가 끝남
        t = reduce(t.session, RingEvent.TimerFired(TimerKind.MissionTimeout), t0 + 69 * sec)
        assertThat(t.session!!.phase).isEqualTo(RingPhase.Ringing)
        t = reduce(t.session, RingEvent.MissionStarted, t0 + 80 * sec)
        assertThat(t.effects).contains(ShowMission(1))
        t = reduce(t.session, RingEvent.MissionCompleted, t0 + 90 * sec)
        assertThat(t.effects).contains(ShowMission(2))
        t = reduce(t.session, RingEvent.MissionCompleted, t0 + 100 * sec)
        assertThat(t.session).isNull()
        assertThat(t.effects).contains(ShowWakeUp)
    }

    @Test
    fun staleMissionTimeout_beforeDeadline_isIgnored() {
        val started = reduce(fire().session, RingEvent.MissionStarted, t0).session
        val s = reduce(started, RingEvent.MissionInteraction, t0 + 50 * sec).session
        val t = reduce(s, RingEvent.TimerFired(TimerKind.MissionTimeout), t0 + 59 * sec)
        assertThat(t.session).isEqualTo(s)
        assertThat(t.effects).isEmpty()
    }

    @Test
    fun timerOfOtherPhase_isIgnored() {
        val s = reduce(fire().session, RingEvent.MissionStarted, t0).session
        val t = reduce(s, RingEvent.TimerFired(TimerKind.RingTimeout), t0 + 60 * sec)
        assertThat(t.session).isEqualTo(s)
        assertThat(t.effects).isEmpty()
    }

    @Test
    fun missionCompleted_notLast_goesToNextMission_new59s() {
        val s = reduce(fire().session, RingEvent.MissionStarted, t0).session
        val t = reduce(s, RingEvent.MissionCompleted, t0 + 30 * sec)
        assertThat(t.session!!.phase).isEqualTo(RingPhase.InMission)
        assertThat(t.effects).containsAtLeast(ShowMission(1), ScheduleTimer(t0 + 89 * sec, TimerKind.MissionTimeout))
    }

    @Test
    fun lastMissionCompleted_withoutWakeCheck_dismisses() {
        var s = reduce(fire().session, RingEvent.MissionStarted, t0).session
        s = reduce(s, RingEvent.MissionCompleted, t0 + 30 * sec).session
        val t = reduce(s, RingEvent.MissionCompleted, t0 + 50 * sec)
        assertThat(t.session).isNull()
        assertThat(t.effects)
            .containsAtLeast(StopSound, CancelTimer, ShowWakeUp, SessionEnded(1, RingOutcome.Dismissed))
    }

    @Test
    fun noMissionAlarm_dismissDirectly() {
        val s = fire(plan(missions = emptyList())).session
        val t = reduce(s, RingEvent.DismissWithoutMission, t0 + 5 * sec)
        assertThat(t.session).isNull()
        assertThat(t.effects).contains(SessionEnded(1, RingOutcome.Dismissed))
    }

    @Test
    fun dismissWithoutMission_isIgnored_whenAlarmHasMissions() {
        val s = fire().session
        val t = reduce(s, RingEvent.DismissWithoutMission, t0 + 5 * sec)
        assertThat(t.session).isEqualTo(s)
        assertThat(t.effects).isEmpty()
    }

    @Test
    fun missionStarted_onNoMissionAlarm_dismisses() {
        val t = reduce(fire(plan(missions = emptyList())).session, RingEvent.MissionStarted, t0)
        assertThat(t.session).isNull()
        assertThat(t.effects).contains(SessionEnded(1, RingOutcome.Dismissed))
    }

    // 다시 잠들기 방지(W2) ----------------------------------------------------

    private fun dismissedWithWakeCheck(): Transition {
        val s = fire(plan(missions = listOf(MissionType.QuickMath), wakeCheck = true)).session
        val m = reduce(s, RingEvent.MissionStarted, t0).session
        return reduce(m, RingEvent.MissionCompleted, t0 + 30 * sec)
    }

    @Test
    fun wakeCheck_waitsThenAsks() {
        val t = dismissedWithWakeCheck()
        assertThat(t.session!!.phase).isEqualTo(RingPhase.AwaitingWakeCheck)
        assertThat(t.effects)
            .containsAtLeast(StopSound, ShowWakeUp, ScheduleTimer(t0 + 30 * sec + 3 * min, TimerKind.WakeCheckAsk))
        val ask = reduce(t.session, RingEvent.TimerFired(TimerKind.WakeCheckAsk), t0 + 30 * sec + 3 * min)
        assertThat(ask.session!!.phase).isEqualTo(RingPhase.WakeCheckAsking)
        assertThat(ask.effects)
            .containsAtLeast(ShowWakeCheck, ScheduleTimer(t0 + 30 * sec + 4 * min, TimerKind.WakeCheckTimeout))
    }

    @Test
    fun wakeCheckConfirmed_dismisses() {
        val ask = reduce(dismissedWithWakeCheck().session, RingEvent.TimerFired(TimerKind.WakeCheckAsk), t0 + 4 * min)
        val t = reduce(ask.session, RingEvent.WakeCheckConfirmed, t0 + 4 * min + 10 * sec)
        assertThat(t.session).isNull()
        assertThat(t.effects).containsAtLeast(CancelTimer, SessionEnded(1, RingOutcome.Dismissed))
    }

    @Test
    fun wakeCheckTimeout_ringsAgain_fromFirstMission_withFullRingCount() {
        val ask = reduce(dismissedWithWakeCheck().session, RingEvent.TimerFired(TimerKind.WakeCheckAsk), t0 + 4 * min)
        val t = reduce(ask.session, RingEvent.TimerFired(TimerKind.WakeCheckTimeout), t0 + 5 * min)
        assertThat(t.session!!.phase).isEqualTo(RingPhase.Ringing)
        assertThat(t.session!!.missionsDone).isEqualTo(0)
        assertThat(t.session!!.ringsLeft).isEqualTo(2)
        assertThat(t.effects).containsAtLeast(PlaySound(1f), ShowRingScreen)
    }

    // 다른 알람 -------------------------------------------------------------

    @Test
    fun otherAlarmDuringMission_isMarkedPassed_andSessionContinues() {
        val s = reduce(fire().session, RingEvent.MissionStarted, t0).session
        val t = reduce(s, RingEvent.AlarmFired(plan(id = 2)), t0 + 10 * sec)
        assertThat(t.session).isEqualTo(s)
        assertThat(t.effects).containsExactly(MarkPassed(2), RescheduleAlarm(2))
    }

    @Test
    fun otherAlarmWhileResting_isMarkedPassed() {
        val s = reduce(fire().session, RingEvent.TimerFired(TimerKind.RingTimeout), t0 + min).session
        val t = reduce(s, RingEvent.AlarmFired(plan(id = 3)), t0 + 2 * min)
        assertThat(t.session).isEqualTo(s)
        assertThat(t.effects).containsExactly(MarkPassed(3), RescheduleAlarm(3))
    }

    // 세션 없음 -------------------------------------------------------------

    @Test
    fun eventsWithoutSession_doNothing() {
        RingEvent.userEvents.forEach { e ->
            val t = reduce(null, e, t0)
            assertThat(t.session).isNull()
            assertThat(t.effects).isEmpty()
        }
        TimerKind.entries.forEach { k ->
            assertThat(reduce(null, RingEvent.TimerFired(k), t0).effects).isEmpty()
        }
    }

    // 앱이 죽었다 살아남 ------------------------------------------------------

    @Test
    fun resume_ringing_restartsSoundScreenAndTimer() {
        val s = fire().session!!
        assertThat(RingStateMachine.resume(s)).containsExactly(
            PlaySound(1f),
            ShowRingScreen,
            ScheduleTimer(s.phaseEndsAt, TimerKind.RingTimeout),
        )
    }

    @Test
    fun resume_inMission_returnsToSameMission_quietly() {
        var s = reduce(fire().session, RingEvent.MissionStarted, t0).session
        s = reduce(s, RingEvent.MissionCompleted, t0 + 10 * sec).session!!
        assertThat(RingStateMachine.resume(s)).containsExactly(
            StopSound,
            ShowMission(1),
            ScheduleTimer(s.phaseEndsAt, TimerKind.MissionTimeout),
        )
    }

    @Test
    fun resume_resting_onlyRearmsTimer() {
        val s = reduce(fire().session, RingEvent.TimerFired(TimerKind.RingTimeout), t0 + min).session!!
        assertThat(RingStateMachine.resume(s)).containsExactly(ScheduleTimer(s.phaseEndsAt, TimerKind.RestEnded))
    }

    @Test
    fun resume_wakeCheckAsking_showsQuestionAgain() {
        val ask = reduce(dismissedWithWakeCheck().session, RingEvent.TimerFired(TimerKind.WakeCheckAsk), t0 + 4 * min)
        val s = ask.session!!
        assertThat(RingStateMachine.resume(s))
            .containsExactly(ShowWakeCheck, ScheduleTimer(s.phaseEndsAt, TimerKind.WakeCheckTimeout))
    }

    @Test
    fun resume_awaitingWakeCheck_onlyRearmsTimer() {
        val s = dismissedWithWakeCheck().session!!
        assertThat(RingStateMachine.resume(s)).containsExactly(ScheduleTimer(s.phaseEndsAt, TimerKind.WakeCheckAsk))
    }
}
