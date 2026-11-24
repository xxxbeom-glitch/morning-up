package com.lumian.morningup.core.model.engine

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.engine.RingEffect.CancelTimer
import com.lumian.morningup.core.model.engine.RingEffect.MarkPassed
import com.lumian.morningup.core.model.engine.RingEffect.PausedForCall
import com.lumian.morningup.core.model.engine.RingEffect.PlayChime
import com.lumian.morningup.core.model.engine.RingEffect.PlaySound
import com.lumian.morningup.core.model.engine.RingEffect.PostMissedNotification
import com.lumian.morningup.core.model.engine.RingEffect.RecheckScheduled
import com.lumian.morningup.core.model.engine.RingEffect.RescheduleAlarm
import com.lumian.morningup.core.model.engine.RingEffect.ScheduleTimer
import com.lumian.morningup.core.model.engine.RingEffect.SessionEnded
import com.lumian.morningup.core.model.engine.RingEffect.ShowMission
import com.lumian.morningup.core.model.engine.RingEffect.ShowRecheck
import com.lumian.morningup.core.model.engine.RingEffect.ShowRingScreen
import com.lumian.morningup.core.model.engine.RingEffect.ShowWakeCheck
import com.lumian.morningup.core.model.engine.RingEffect.ShowWakeUp
import com.lumian.morningup.core.model.engine.RingEffect.StopSound
import com.lumian.morningup.core.model.engine.RingEffect.WakeResultRecorded
import org.junit.Test

class RingStateMachineTest {
    private val t0 = 1_800_000_000_000L
    private val sec = 1_000L
    private val min = 60 * sec

    /** 한 번 울리는 길이(11-19 사용자 결정 2분, F4는 1분이었어요). */
    private val ring = 2 * min

    private fun plan(
        id: Long = 1,
        missions: List<MissionType> = listOf(MissionType.QuickMath, MissionType.PopQuiz),
        interval: Int = 5,
        count: Int? = 2,
        wakeCheck: Boolean = false,
        recheck: Boolean = false,
    ) = RingPlan(
        alarmId = id,
        missions = missions,
        intervalMinutes = interval,
        ringAgainCount = count,
        wakeCheckEnabled = wakeCheck,
        wakeCheckAskAfterMinutes = 3,
        wakeCheckAnswerSeconds = 60,
        recheckEnabled = recheck,
        recheckAnswerMinutes = 10,
    )

    private fun reduce(s: RingSession?, e: RingEvent, now: Long) = RingStateMachine.reduce(s, e, now)

    private fun fire(p: RingPlan = plan()) = reduce(null, RingEvent.AlarmFired(p), t0)

    // 알람 시각 ------------------------------------------------------------

    @Test
    fun alarmFired_startsRinging_fullVolume_andTwoMinuteTimer() {
        val t = fire()
        assertThat(t.session!!.phase).isEqualTo(RingPhase.Ringing)
        assertThat(t.session!!.ringsLeft).isEqualTo(2)
        assertThat(t.effects).containsAtLeast(
            PlaySound(1f),
            ShowRingScreen,
            ScheduleTimer(t0 + ring, TimerKind.RingTimeout),
            RescheduleAlarm(1),
        )
    }

    // 아무도 안 끔 ----------------------------------------------------------

    @Test
    fun ringTimeout_withRingsLeft_restsForInterval_andUsesOneRing() {
        val t = reduce(fire().session, RingEvent.TimerFired(TimerKind.RingTimeout), t0 + ring)
        assertThat(t.session!!.phase).isEqualTo(RingPhase.Resting)
        assertThat(t.session!!.ringsLeft).isEqualTo(1)
        assertThat(t.effects).containsAtLeast(StopSound, ScheduleTimer(t0 + ring + 5 * min, TimerKind.RestEnded))
    }

    @Test
    fun ringTimeout_afterOnlyOneMinute_isIgnored_keepsRinging() {
        // 11-19부터 한 번에 2분 울려요. 1분에 온 타이머는 무시해요.
        val s = fire().session
        val t = reduce(s, RingEvent.TimerFired(TimerKind.RingTimeout), t0 + min)
        assertThat(t.session).isEqualTo(s)
        assertThat(t.effects).isEmpty()
    }

    @Test
    fun ringTimeout_rest_closesScreen_andTellsNextRing() {
        // 다시 2번이면 1번째 울림 뒤 쉬기 → 다음은 2번째, 2번째 뒤 쉬기 → 다음은 3번째(결정 70)
        val first = reduce(fire(plan(count = 2)).session, RingEvent.TimerFired(TimerKind.RingTimeout), t0 + ring)
        assertThat(first.effects).contains(RingEffect.RestStarted(t0 + ring + 5 * min, 5, 2))
        val ringing = reduce(first.session, RingEvent.TimerFired(TimerKind.RestEnded), t0 + 7 * min).session
        val second = reduce(ringing, RingEvent.TimerFired(TimerKind.RingTimeout), t0 + 9 * min)
        assertThat(second.effects).contains(RingEffect.RestStarted(t0 + 14 * min, 5, 3))
        // 끌 때까지는 셀 수 없어 번째가 없어요.
        val endless = reduce(fire(plan(count = null)).session, RingEvent.TimerFired(TimerKind.RingTimeout), t0 + ring)
        assertThat(endless.effects).contains(RingEffect.RestStarted(t0 + 7 * min, 5, null))
    }

    @Test
    fun restEnded_ringsAgain() {
        var s = reduce(fire().session, RingEvent.TimerFired(TimerKind.RingTimeout), t0 + ring).session
        val now = t0 + ring + 5 * min
        val t = reduce(s, RingEvent.TimerFired(TimerKind.RestEnded), now)
        s = t.session
        assertThat(s!!.phase).isEqualTo(RingPhase.Ringing)
        assertThat(t.effects)
            .containsAtLeast(PlaySound(1f), ShowRingScreen, ScheduleTimer(now + ring, TimerKind.RingTimeout))
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
            now += ring
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
    fun ringAgainZero_missedAfterFirstRing() {
        val t = reduce(fire(plan(count = 0)).session, RingEvent.TimerFired(TimerKind.RingTimeout), t0 + ring)
        assertThat(t.session).isNull()
        assertThat(t.effects).contains(PostMissedNotification(1))
    }

    @Test
    fun untilDismissed_neverMisses() {
        var s = fire(plan(count = null)).session
        var now = t0
        repeat(50) {
            now += ring
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
        val resting = reduce(fire().session, RingEvent.TimerFired(TimerKind.RingTimeout), t0 + ring).session
        val t = reduce(resting, RingEvent.MissionStarted, t0 + ring + min)
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
        val s = reduce(fire().session, RingEvent.TimerFired(TimerKind.RingTimeout), t0 + ring).session
        val t = reduce(s, RingEvent.AlarmFired(plan(id = 3)), t0 + ring + min)
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
        val s = reduce(fire().session, RingEvent.TimerFired(TimerKind.RingTimeout), t0 + ring).session!!
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

    // 한 번 더 확인(W3, 결정 145) ---------------------------------------------

    /** 미션 1개, W2와 W3 켬. 미션을 t0+30초에 마쳐 W2를 t0+3분30초에 물어요. */
    private fun dismissedWithRecheck(): Transition {
        val s = fire(plan(missions = listOf(MissionType.QuickMath), wakeCheck = true, recheck = true)).session
        val m = reduce(s, RingEvent.MissionStarted, t0).session
        return reduce(m, RingEvent.MissionCompleted, t0 + 30 * sec)
    }

    private val w2At get() = t0 + 30 * sec + 3 * min

    private fun w2Asking() = reduce(dismissedWithRecheck().session, RingEvent.TimerFired(TimerKind.WakeCheckAsk), w2At)

    /** W2 '깼어요'를 w2At+10초에 눌러 W3를 기다려요. */
    private fun awaitingRecheck() = reduce(w2Asking().session, RingEvent.WakeCheckConfirmed, w2At + 10 * sec)

    private val w3At get() = w2At + 10 * sec + 3 * min

    private fun w3Asking() = reduce(awaitingRecheck().session, RingEvent.TimerFired(TimerKind.RecheckAsk), w3At)

    @Test
    fun wakeCheckAsk_playsChime() {
        assertThat(w2Asking().effects).containsExactly(
            ShowWakeCheck,
            PlayChime,
            ScheduleTimer(w2At + min, TimerKind.WakeCheckTimeout),
        )
    }

    @Test
    fun wakeCheckConfirmed_withRecheck_waitsSameInterval_quietly() {
        val t = awaitingRecheck()
        assertThat(t.session!!.phase).isEqualTo(RingPhase.AwaitingRecheck)
        assertThat(t.effects).containsExactly(RecheckScheduled(w3At), ScheduleTimer(w3At, TimerKind.RecheckAsk))
    }

    @Test
    fun recheckAsk_showsW3_withTenMinutes() {
        val t = w3Asking()
        assertThat(t.session!!.phase).isEqualTo(RingPhase.RecheckAsking)
        assertThat(t.effects)
            .containsExactly(ShowRecheck, PlayChime, ScheduleTimer(w3At + 10 * min, TimerKind.RecheckTimeout))
    }

    @Test
    fun recheckConfirmed_inTime_dismisses_andRecordsResult() {
        val t = reduce(w3Asking().session, RingEvent.RecheckConfirmed, w3At + 20 * sec)
        assertThat(t.session).isNull()
        assertThat(t.effects).containsExactly(
            StopSound,
            CancelTimer,
            WakeResultRecorded(1, WakeResult.RecheckPassed, 0),
            SessionEnded(1, RingOutcome.Dismissed),
        )
    }

    @Test
    fun recheckConfirmed_whileWaiting_endsEarly() {
        val t = reduce(awaitingRecheck().session, RingEvent.RecheckConfirmed, w2At + min)
        assertThat(t.session).isNull()
        assertThat(t.effects).contains(WakeResultRecorded(1, WakeResult.RecheckPassedEarly, 0))
    }

    @Test
    fun recheckConfirmed_inOtherPhase_isIgnored() {
        val s = w2Asking().session
        assertThat(reduce(s, RingEvent.RecheckConfirmed, w2At + sec).session).isEqualTo(s)
    }

    @Test
    fun wakeCheckConfirmed_withoutRecheck_recordsW2Pass() {
        val ask = reduce(dismissedWithWakeCheck().session, RingEvent.TimerFired(TimerKind.WakeCheckAsk), t0 + 4 * min)
        val t = reduce(ask.session, RingEvent.WakeCheckConfirmed, t0 + 4 * min + 10 * sec)
        assertThat(t.effects).contains(WakeResultRecorded(1, WakeResult.WakeCheckPassed, 0))
    }

    @Test
    fun recheckTimeout_ringsAgainFromStart_andCountsRound() {
        val t = reduce(w3Asking().session, RingEvent.TimerFired(TimerKind.RecheckTimeout), w3At + 10 * min)
        assertThat(t.session!!.phase).isEqualTo(RingPhase.Ringing)
        assertThat(t.session!!.missionsDone).isEqualTo(0)
        assertThat(t.session!!.ringsLeft).isEqualTo(2)
        assertThat(t.session!!.rounds).isEqualTo(1)
        assertThat(t.effects).containsAtLeast(PlaySound(1f), ShowRingScreen)
    }

    @Test
    fun afterTwoRounds_finishEndsAtW1_withoutChecks() {
        val s = fire(plan(missions = listOf(MissionType.QuickMath), wakeCheck = true, recheck = true)).session!!
        val capped = s.copy(rounds = RingStateMachine.MAX_ROUNDS)
        val m = reduce(capped, RingEvent.MissionStarted, t0).session
        val t = reduce(m, RingEvent.MissionCompleted, t0 + 30 * sec)
        assertThat(t.session).isNull()
        assertThat(t.effects).containsAtLeast(
            ShowWakeUp,
            WakeResultRecorded(1, WakeResult.EndedByCap, 2),
            SessionEnded(1, RingOutcome.Dismissed),
        )
    }

    @Test
    fun oneRound_stillAsks() {
        val s = fire(plan(missions = emptyList(), wakeCheck = true)).session!!.copy(rounds = 1)
        val t = reduce(s, RingEvent.DismissWithoutMission, t0)
        assertThat(t.session!!.phase).isEqualTo(RingPhase.AwaitingWakeCheck)
    }

    @Test
    fun missed_withWakeCheck_recordsMissed() {
        val s = fire(plan(count = 0, wakeCheck = true)).session
        val t = reduce(s, RingEvent.TimerFired(TimerKind.RingTimeout), t0 + ring)
        assertThat(t.effects).contains(WakeResultRecorded(1, WakeResult.Missed, 0))
    }

    @Test
    fun missed_withoutWakeCheck_recordsNothing() {
        val t = reduce(fire(plan(count = 0)).session, RingEvent.TimerFired(TimerKind.RingTimeout), t0 + ring)
        assertThat(t.effects.filterIsInstance<WakeResultRecorded>()).isEmpty()
    }

    // 통화 중 ---------------------------------------------------------------

    @Test
    fun askDuringCall_isPaused_withFullAnswerTime() {
        val waiting = dismissedWithRecheck().session
        val t = RingStateMachine.reduce(waiting, RingEvent.TimerFired(TimerKind.WakeCheckAsk), w2At, inCall = true)
        assertThat(t.session!!.phase).isEqualTo(RingPhase.WakeCheckAsking)
        assertThat(t.session!!.pausedLeftMillis).isEqualTo(min)
        assertThat(t.effects).containsExactly(PausedForCall)
    }

    @Test
    fun callStartsWhileAsking_pausesWithTimeLeft_thenResumes() {
        val asking = w2Asking().session
        val paused = reduce(asking, RingEvent.CallChanged(inCall = true), w2At + 20 * sec)
        assertThat(paused.session!!.pausedLeftMillis).isEqualTo(40 * sec)
        assertThat(paused.effects).containsExactly(CancelTimer, PausedForCall)
        // 멈춘 동안에는 '깼어요'도 받지 않아요.
        assertThat(reduce(paused.session, RingEvent.WakeCheckConfirmed, w2At + 30 * sec).session)
            .isEqualTo(paused.session)
        val ended = w2At + 5 * min
        val resumed = reduce(paused.session, RingEvent.CallChanged(inCall = false), ended)
        assertThat(resumed.session!!.pausedLeftMillis).isNull()
        assertThat(resumed.effects).containsExactly(
            ShowWakeCheck,
            PlayChime,
            ScheduleTimer(ended + 40 * sec, TimerKind.WakeCheckTimeout),
        )
    }

    @Test
    fun callChanged_whileRinging_doesNothing() {
        val s = fire().session
        assertThat(reduce(s, RingEvent.CallChanged(inCall = true), t0 + sec).effects).isEmpty()
    }

    @Test
    fun resume_paused_staysPaused() {
        val paused = reduce(w2Asking().session, RingEvent.CallChanged(inCall = true), w2At + sec).session!!
        assertThat(RingStateMachine.resume(paused)).containsExactly(PausedForCall)
    }

    @Test
    fun resume_recheckAsking_showsW3Again() {
        val s = w3Asking().session!!
        assertThat(RingStateMachine.resume(s))
            .containsExactly(ShowRecheck, ScheduleTimer(s.phaseEndsAt, TimerKind.RecheckTimeout))
    }

    @Test
    fun resume_awaitingRecheck_onlyRearmsTimer() {
        val s = awaitingRecheck().session!!
        assertThat(RingStateMachine.resume(s)).containsExactly(ScheduleTimer(s.phaseEndsAt, TimerKind.RecheckAsk))
    }

    // 남은 확인 취소 ----------------------------------------------------------

    @Test
    fun cancelChecks_forSameAlarm_endsQuietly() {
        val t = reduce(awaitingRecheck().session, RingEvent.CancelChecks(1), w2At + min)
        assertThat(t.session).isNull()
        assertThat(t.effects).containsExactly(
            StopSound,
            CancelTimer,
            WakeResultRecorded(1, WakeResult.Cancelled, 0),
            SessionEnded(1, RingOutcome.Cancelled),
        )
    }

    @Test
    fun cancelChecks_forOtherAlarm_orWhileRinging_isIgnored() {
        val waiting = awaitingRecheck().session
        assertThat(reduce(waiting, RingEvent.CancelChecks(9), w2At + min).session).isEqualTo(waiting)
        val ringing = fire().session
        assertThat(reduce(ringing, RingEvent.CancelChecks(1), t0 + sec).session).isEqualTo(ringing)
    }

    @Test
    fun recheckTurnedOff_cancelsW3_butKeepsW2() {
        assertThat(reduce(w3Asking().session, RingEvent.RecheckTurnedOff, w3At + sec).session).isNull()
        val w2 = reduce(w2Asking().session, RingEvent.RecheckTurnedOff, w2At + sec)
        assertThat(w2.session!!.phase).isEqualTo(RingPhase.WakeCheckAsking)
        assertThat(w2.session!!.plan.recheckEnabled).isFalse()
        assertThat(w2.effects).isEmpty()
        // 그 뒤 W2를 통과하면 W3 없이 끝나요.
        assertThat(reduce(w2.session, RingEvent.WakeCheckConfirmed, w2At + 2 * sec).session).isNull()
    }

    @Test
    fun newAlarm_duringChecks_cancelsOld_andRingsNew() {
        val t = reduce(awaitingRecheck().session, RingEvent.AlarmFired(plan(id = 2)), w2At + min)
        assertThat(t.session!!.plan.alarmId).isEqualTo(2)
        assertThat(t.session!!.phase).isEqualTo(RingPhase.Ringing)
        assertThat(t.effects).containsAtLeast(
            WakeResultRecorded(1, WakeResult.Cancelled, 0),
            SessionEnded(1, RingOutcome.Cancelled),
            PlaySound(1f),
            ShowRingScreen,
            RescheduleAlarm(2),
        ).inOrder()
    }
}
