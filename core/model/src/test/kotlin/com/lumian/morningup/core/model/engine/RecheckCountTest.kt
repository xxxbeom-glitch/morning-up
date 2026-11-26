package com.lumian.morningup.core.model.engine

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.LabSettings
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.WakeCheck
import com.lumian.morningup.core.model.WakeCheckSettings
import com.lumian.morningup.core.model.engine.RingEffect.RecheckScheduled
import com.lumian.morningup.core.model.engine.RingEffect.ScheduleTimer
import com.lumian.morningup.core.model.engine.RingEffect.SessionEnded
import com.lumian.morningup.core.model.engine.RingEffect.ShowRecheck
import com.lumian.morningup.core.model.engine.RingEffect.StopSound
import com.lumian.morningup.core.model.engine.RingEffect.WakeResultRecorded
import java.time.LocalTime
import org.junit.Test

/** W3 기다리는 시간, 묻는 횟수, 대답할 시간 1분(결정 176). 실험실 여러 번 묻기는 없앴어요(결정 177). */
class RecheckCountTest {
    private val t0 = 1_800_000_000_000L
    private val sec = 1_000L
    private val min = 60 * sec

    private fun plan(count: Int = 1, wait: Int = 3, minute: Long = min) = RingPlan(
        alarmId = 7,
        missions = listOf(MissionType.QuickMath),
        intervalMinutes = 5,
        ringAgainCount = 0,
        wakeCheckEnabled = true,
        wakeCheckAskAfterMinutes = 5,
        wakeCheckAnswerSeconds = 60,
        recheckEnabled = true,
        recheckWaitMinutes = wait,
        recheckCount = count,
        minuteMillis = minute,
    )

    private fun reduce(s: RingSession?, e: RingEvent, now: Long) = RingStateMachine.reduce(s, e, now)

    /** 울림 → 미션 → 미션 끝(t0 + 1분) → W2 묻기(t0 + 6분, W2 다시 물어보기 5분). */
    private fun w2Asking(p: RingPlan = plan()): RingSession {
        var s = reduce(null, RingEvent.AlarmFired(p), t0).session
        s = reduce(s, RingEvent.MissionStarted, t0 + 30 * sec).session
        s = reduce(s, RingEvent.MissionCompleted, t0 + min).session
        assertThat(s!!.phaseEndsAt).isEqualTo(t0 + 6 * min)
        return reduce(s, RingEvent.TimerFired(TimerKind.WakeCheckAsk), t0 + 6 * min).session!!
    }

    @Test
    fun planFromAlarm_takesWaitAndCount_andFixedAnswerMinute() {
        // 결정 180: W2, W3 값은 알람마다예요. 전체 설정(처음 값)이 아니라 알람 값을 써요.
        val check = WakeCheck(enabled = true, askAfterMinutes = 10, recheckWaitMinutes = 1, recheckCount = 3)
        val p = RingPlan.from(Alarm(time = LocalTime.of(6, 30), wakeCheck = check), WakeCheckSettings())
        assertThat(p.wakeCheckAskAfterMinutes).isEqualTo(10)
        assertThat(p.recheckWaitMinutes).isEqualTo(1)
        assertThat(p.recheckCount).isEqualTo(3)
        assertThat(p.recheckAnswerMinutes).isEqualTo(1)
    }

    @Test
    fun w2Passed_waitsRecheckMinutes_notW2Interval() {
        // W2 다시 물어보기는 5분이지만 W3까지는 따로 정한 3분이에요.
        val t = reduce(w2Asking(), RingEvent.WakeCheckConfirmed, t0 + 7 * min)
        val askAt = t0 + 10 * min
        assertThat(t.session!!.phase).isEqualTo(RingPhase.AwaitingRecheck)
        assertThat(t.session!!.checksLeft).isNull()
        assertThat(t.effects).containsExactly(RecheckScheduled(askAt), ScheduleTimer(askAt, TimerKind.RecheckAsk))
    }

    @Test
    fun recheckAnswer_isOneMinute() {
        val waiting = reduce(w2Asking(), RingEvent.WakeCheckConfirmed, t0 + 7 * min).session!!
        val t = reduce(waiting, RingEvent.TimerFired(TimerKind.RecheckAsk), t0 + 10 * min)
        assertThat(t.session!!.phase).isEqualTo(RingPhase.RecheckAsking)
        assertThat(t.session!!.phaseEndsAt).isEqualTo(t0 + 11 * min)
        assertThat(t.effects).containsAtLeast(ShowRecheck, ScheduleTimer(t0 + 11 * min, TimerKind.RecheckTimeout))
    }

    @Test
    fun countOne_endsAfterFirstRecheck() {
        var s = reduce(w2Asking(), RingEvent.WakeCheckConfirmed, t0 + 7 * min).session!!
        s = reduce(s, RingEvent.TimerFired(TimerKind.RecheckAsk), t0 + 10 * min).session!!
        val t = reduce(s, RingEvent.RecheckConfirmed, t0 + 10 * min + 20 * sec)
        assertThat(t.session).isNull()
        assertThat(t.effects).contains(WakeResultRecorded(7, WakeResult.RecheckPassed, 0))
    }

    @Test
    fun countThree_asksThreeTimes_withSameWait() {
        val p = plan(count = 3, wait = 5)
        var now = t0 + 7 * min
        var t = reduce(w2Asking(p), RingEvent.WakeCheckConfirmed, now)
        assertThat(t.effects).contains(RecheckScheduled(now + 5 * min, number = 1, total = 3))
        assertThat(t.session!!.checksLeft).isEqualTo(3)
        repeat(2) { i ->
            now = t.session!!.phaseEndsAt
            val asking = reduce(t.session, RingEvent.TimerFired(TimerKind.RecheckAsk), now).session!!
            now += 10 * sec
            t = reduce(asking, RingEvent.RecheckConfirmed, now)
            val askAt = now + 5 * min
            assertThat(t.session!!.phase).isEqualTo(RingPhase.AwaitingRecheck)
            assertThat(t.session!!.checksDone).isEqualTo(i + 1)
            assertThat(t.session!!.checksLeft).isEqualTo(2 - i)
            assertThat(t.effects).containsExactly(
                StopSound,
                RecheckScheduled(askAt, number = i + 2, total = 3),
                ScheduleTimer(askAt, TimerKind.RecheckAsk),
            ).inOrder()
        }
        now = t.session!!.phaseEndsAt
        val last = reduce(t.session, RingEvent.TimerFired(TimerKind.RecheckAsk), now).session!!
        val end = reduce(last, RingEvent.RecheckConfirmed, now + 10 * sec)
        assertThat(end.session).isNull()
        assertThat(end.effects).contains(SessionEnded(7, RingOutcome.Dismissed))
    }

    @Test
    fun earlyFinish_countsOne_onlyOncePerWait() {
        val waiting = reduce(w2Asking(plan(count = 2)), RingEvent.WakeCheckConfirmed, t0 + 7 * min).session!!
        assertThat(waiting.canFinishEarly).isTrue()
        val early = reduce(waiting, RingEvent.RecheckConfirmed, t0 + 8 * min).session!!
        assertThat(early.checksDone).isEqualTo(1)
        assertThat(early.canFinishEarly).isFalse()
        // 같은 기다림에서 또 미리 끝낼 수는 없어요.
        assertThat(reduce(early, RingEvent.RecheckConfirmed, t0 + 9 * min).session).isEqualTo(early)
        // 다음 W3가 실제로 물을 때 끝내면 다 마쳐요.
        val asking = reduce(early, RingEvent.TimerFired(TimerKind.RecheckAsk), early.phaseEndsAt).session!!
        val end = reduce(asking, RingEvent.RecheckConfirmed, early.phaseEndsAt + 5 * sec)
        assertThat(end.session).isNull()
        assertThat(end.effects).contains(WakeResultRecorded(7, WakeResult.RecheckPassed, 0))
    }

    @Test
    fun recheckTimeout_ringsAgain_andCountsFromZero() {
        var s = reduce(w2Asking(plan(count = 3)), RingEvent.WakeCheckConfirmed, t0 + 7 * min).session!!
        s = reduce(s, RingEvent.TimerFired(TimerKind.RecheckAsk), s.phaseEndsAt).session!!
        s = reduce(s, RingEvent.RecheckConfirmed, s.phaseEndsAt - 30 * sec).session!!
        assertThat(s.checksDone).isEqualTo(1)
        s = reduce(s, RingEvent.TimerFired(TimerKind.RecheckAsk), s.phaseEndsAt).session!!
        val again = reduce(s, RingEvent.TimerFired(TimerKind.RecheckTimeout), s.phaseEndsAt).session!!
        assertThat(again.phase).isEqualTo(RingPhase.Ringing)
        assertThat(again.rounds).isEqualTo(1)
        assertThat(again.checksDone).isEqualTo(0)
        assertThat(again.checksLeft).isEqualTo(3)
    }

    @Test
    fun recheckOff_endsAtW2() {
        val t = reduce(w2Asking(plan().copy(recheckEnabled = false)), RingEvent.WakeCheckConfirmed, t0 + 7 * min)
        assertThat(t.session).isNull()
        assertThat(t.effects).contains(WakeResultRecorded(7, WakeResult.WakeCheckPassed, 0))
    }

    @Test
    fun debugMinute_scalesRecheckWaitAndAnswer() {
        val minute = 6 * sec
        val waiting = reduce(w2Asking(plan(minute = minute)), RingEvent.WakeCheckConfirmed, t0 + 7 * min).session!!
        assertThat(waiting.phaseEndsAt).isEqualTo(t0 + 7 * min + 3 * minute)
        val asking = reduce(waiting, RingEvent.TimerFired(TimerKind.RecheckAsk), waiting.phaseEndsAt).session!!
        assertThat(asking.phaseEndsAt).isEqualTo(waiting.phaseEndsAt + minute)
        assertThat(LabSettings().debugMinuteMillis).isEqualTo(min)
    }
}
