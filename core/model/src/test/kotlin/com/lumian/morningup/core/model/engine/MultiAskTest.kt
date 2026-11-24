package com.lumian.morningup.core.model.engine

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.LabSettings
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.TrickLevel
import com.lumian.morningup.core.model.TrickSpeed
import com.lumian.morningup.core.model.WakeCheckSettings
import com.lumian.morningup.core.model.engine.RingEffect.MultiAskRecorded
import com.lumian.morningup.core.model.engine.RingEffect.RecheckScheduled
import com.lumian.morningup.core.model.engine.RingEffect.ScheduleTimer
import com.lumian.morningup.core.model.engine.RingEffect.SessionEnded
import com.lumian.morningup.core.model.engine.RingEffect.ShowRecheck
import com.lumian.morningup.core.model.engine.RingEffect.ShowWakeUp
import com.lumian.morningup.core.model.engine.RingEffect.WakeResultRecorded
import kotlin.random.Random
import org.junit.Test

/** 실험실 여러 번 묻기(결정 146 3절). */
class MultiAskTest {
    private val t0 = 1_800_000_000_000L
    private val sec = 1_000L
    private val min = 60 * sec

    private fun plan(total: Int = 3, minute: Long = min) = RingPlan(
        alarmId = 7,
        missions = listOf(MissionType.QuickMath),
        intervalMinutes = 5,
        ringAgainCount = 0,
        wakeCheckEnabled = true,
        wakeCheckAskAfterMinutes = 3,
        wakeCheckAnswerSeconds = 60,
        recheckEnabled = true,
        recheckAnswerMinutes = 10,
        askTotal = total,
        minuteMillis = minute,
    )

    /** 늘 같은 간격을 고르는 무작위(5~15 중 [minutes]). */
    private fun fixed(minutes: Int) = object : Random() {
        override fun nextBits(bitCount: Int) = 0
        override fun nextInt(from: Int, until: Int) = minutes.coerceIn(from, until - 1)
    }

    private fun reduce(s: RingSession?, e: RingEvent, now: Long, gap: Int = 5) =
        RingStateMachine.reduce(s, e, now, random = fixed(gap))

    /** 울림 → 미션 → 미션 끝(t0 + 1분) → W2 묻기(t0 + 4분). */
    private fun w2Asking(p: RingPlan = plan()): RingSession {
        var s = reduce(null, RingEvent.AlarmFired(p), t0).session
        s = reduce(s, RingEvent.MissionStarted, t0 + 30 * sec).session
        s = reduce(s, RingEvent.MissionCompleted, t0 + min).session
        assertThat(s!!.roundStartedAt).isEqualTo(t0 + min)
        return reduce(s, RingEvent.TimerFired(TimerKind.WakeCheckAsk), t0 + 4 * min).session!!
    }

    @Test
    fun w2Passed_countsOne_andWaitsRandomGap_withoutClockInNotification() {
        val t = reduce(w2Asking(), RingEvent.WakeCheckConfirmed, t0 + 5 * min, gap = 9)
        assertThat(t.session!!.phase).isEqualTo(RingPhase.AwaitingRecheck)
        assertThat(t.session!!.checksDone).isEqualTo(1)
        assertThat(t.session!!.checksLeft).isEqualTo(2)
        val askAt = t0 + 14 * min
        assertThat(t.effects).containsAtLeast(
            MultiAskRecorded(7, MultiAskKind.Pass, 1, 3),
            RecheckScheduled(askAt, number = 2, total = 3, gapMinutes = 9),
            ScheduleTimer(askAt, TimerKind.RecheckAsk),
        )
    }

    @Test
    fun randomGap_isAlwaysBetweenFiveAndFifteenMinutes() {
        val w2 = w2Asking()
        repeat(200) { seed ->
            val t = RingStateMachine.reduce(w2, RingEvent.WakeCheckConfirmed, t0 + 5 * min, random = Random(seed))
            val gap = (t.session!!.phaseEndsAt - (t0 + 5 * min)) / min
            assertThat(gap).isIn(5L..15L)
        }
    }

    @Test
    fun threeChecks_endAfterThird_withRecordsForEach() {
        var s: RingSession? = reduce(w2Asking(), RingEvent.WakeCheckConfirmed, t0 + 5 * min).session
        // 둘째(W3) t0 + 10분
        s = reduce(s, RingEvent.TimerFired(TimerKind.RecheckAsk), t0 + 10 * min).session
        assertThat(s!!.phase).isEqualTo(RingPhase.RecheckAsking)
        s = reduce(s, RingEvent.RecheckConfirmed, t0 + 11 * min).session
        assertThat(s!!.checksDone).isEqualTo(2)
        // 셋째(W3) t0 + 16분
        s = reduce(s, RingEvent.TimerFired(TimerKind.RecheckAsk), t0 + 16 * min).session
        val end = reduce(s, RingEvent.RecheckConfirmed, t0 + 17 * min)
        assertThat(end.session).isNull()
        assertThat(end.effects).containsAtLeast(
            MultiAskRecorded(7, MultiAskKind.Pass, 3, 3),
            WakeResultRecorded(7, WakeResult.RecheckPassed, 0),
            SessionEnded(7, RingOutcome.Dismissed),
        )
    }

    @Test
    fun missedSecond_ringsAgain_thenAsksOnlyRemaining_startingFromW2() {
        var s: RingSession? = reduce(w2Asking(), RingEvent.WakeCheckConfirmed, t0 + 5 * min).session
        s = reduce(s, RingEvent.TimerFired(TimerKind.RecheckAsk), t0 + 10 * min).session
        val missed = reduce(s, RingEvent.TimerFired(TimerKind.RecheckTimeout), t0 + 20 * min)
        assertThat(missed.session!!.phase).isEqualTo(RingPhase.Ringing)
        assertThat(missed.session!!.rounds).isEqualTo(1)
        assertThat(missed.session!!.checksDone).isEqualTo(1)
        assertThat(missed.effects).contains(MultiAskRecorded(7, MultiAskKind.Miss, 2, 3))
        // 다시 미션 → W2(둘째로 셈) → W3(셋째) → 끝
        s = reduce(missed.session, RingEvent.MissionStarted, t0 + 21 * min).session
        s = reduce(s, RingEvent.MissionCompleted, t0 + 22 * min).session
        assertThat(s!!.phase).isEqualTo(RingPhase.AwaitingWakeCheck)
        assertThat(s.roundStartedAt).isEqualTo(t0 + 22 * min)
        s = reduce(s, RingEvent.TimerFired(TimerKind.WakeCheckAsk), t0 + 25 * min).session
        val second = reduce(s, RingEvent.WakeCheckConfirmed, t0 + 26 * min)
        assertThat(second.effects).contains(MultiAskRecorded(7, MultiAskKind.Pass, 2, 3))
        s = reduce(second.session, RingEvent.TimerFired(TimerKind.RecheckAsk), t0 + 31 * min).session
        val end = reduce(s, RingEvent.RecheckConfirmed, t0 + 32 * min)
        assertThat(end.session).isNull()
        assertThat(end.effects).contains(MultiAskRecorded(7, MultiAskKind.Pass, 3, 3))
    }

    @Test
    fun roundCapStaysTwo_thirdRingEndsAtW1() {
        var s: RingSession? = w2Asking()
        repeat(RingStateMachine.MAX_ROUNDS) { round ->
            val base = t0 + (round + 1) * 30 * min
            s = reduce(s, RingEvent.TimerFired(TimerKind.WakeCheckTimeout), base).session
            s = reduce(s, RingEvent.MissionStarted, base + min).session
            val done = reduce(s, RingEvent.MissionCompleted, base + 2 * min)
            if (round == RingStateMachine.MAX_ROUNDS - 1) {
                assertThat(done.session).isNull()
                assertThat(done.effects).contains(ShowWakeUp)
                assertThat(done.effects).contains(WakeResultRecorded(7, WakeResult.EndedByCap, 2))
            } else {
                s = reduce(done.session, RingEvent.TimerFired(TimerKind.WakeCheckAsk), base + 5 * min).session
            }
        }
    }

    @Test
    fun nextAskPastSixtyMinutes_dropsRemaining_andEnds() {
        // 미션 끝 t0 + 1분. 다섯 번 중 둘째를 t0 + 50분에 마치고 15분 뒤(66분)를 고르면 상한(61분)을 넘어요.
        var s: RingSession? = reduce(w2Asking(plan(total = 5)), RingEvent.WakeCheckConfirmed, t0 + 5 * min).session
        s = reduce(s, RingEvent.TimerFired(TimerKind.RecheckAsk), t0 + 10 * min).session
        s = s!!.copy(phaseEndsAt = t0 + 59 * min)
        val t = reduce(s, RingEvent.RecheckConfirmed, t0 + 50 * min, gap = 15)
        assertThat(t.session).isNull()
        assertThat(t.effects).containsAtLeast(
            MultiAskRecorded(7, MultiAskKind.Pass, 2, 5),
            MultiAskRecorded(7, MultiAskKind.Cap, 2, 5, left = 3),
            WakeResultRecorded(7, WakeResult.EndedByTimeCap, 0),
        )
    }

    @Test
    fun earlyFinish_onlyOneAtATime() {
        val waiting = reduce(w2Asking(), RingEvent.WakeCheckConfirmed, t0 + 5 * min).session!!
        assertThat(waiting.canFinishEarly).isTrue()
        val early = reduce(waiting, RingEvent.RecheckConfirmed, t0 + 6 * min)
        assertThat(early.session!!.checksDone).isEqualTo(2)
        assertThat(early.session!!.phase).isEqualTo(RingPhase.AwaitingRecheck)
        assertThat(early.session!!.canFinishEarly).isFalse()
        // 같은 기다림에서 또 미리 끝내려 해도 바뀌지 않아요.
        val again = reduce(early.session, RingEvent.RecheckConfirmed, t0 + 7 * min)
        assertThat(again.session).isEqualTo(early.session)
        // 실제로 물을 때는 끝낼 수 있어요.
        val asked = reduce(early.session, RingEvent.TimerFired(TimerKind.RecheckAsk), t0 + 11 * min)
        assertThat(asked.effects).contains(ShowRecheck)
        val end = reduce(asked.session, RingEvent.RecheckConfirmed, t0 + 12 * min)
        assertThat(end.session).isNull()
    }

    @Test
    fun recheckTurnedOff_isIgnored_whileMultiAsk() {
        val waiting = reduce(w2Asking(), RingEvent.WakeCheckConfirmed, t0 + 5 * min).session
        assertThat(reduce(waiting, RingEvent.RecheckTurnedOff, t0 + 6 * min).session).isEqualTo(waiting)
    }

    @Test
    fun debugTimeScale_shortensGapAndCap() {
        val sixSeconds = 6 * sec
        val w2 = w2Asking(plan(minute = sixSeconds))
        val t = reduce(w2, RingEvent.WakeCheckConfirmed, t0 + 5 * min, gap = 10)
        assertThat(t.session!!.phaseEndsAt).isEqualTo(t0 + 5 * min + 10 * sixSeconds)
    }

    @Test
    fun planFrom_labSettings() {
        val alarm = Alarm(id = 3, time = java.time.LocalTime.of(6, 30))
        val off = RingPlan.from(alarm, WakeCheckSettings(recheckEnabled = false))
        assertThat(off.askTotal).isNull()
        assertThat(off.trick).isNull()
        assertThat(off.recheckEnabled).isFalse()
        val lab = LabSettings(
            trickEnabled = true,
            trickLevel = TrickLevel.Strong,
            trickSpeed = TrickSpeed.Fast,
            multiAskEnabled = true,
            multiAskCount = 4,
        )
        val on = RingPlan.from(alarm, WakeCheckSettings(recheckEnabled = false, lab = lab))
        assertThat(on.askTotal).isEqualTo(4)
        // 여러 번 묻기를 켜면 '한 번 더 확인'이 꺼져 있어도 W3를 물어요.
        assertThat(on.recheckEnabled).isTrue()
        assertThat(on.trick!!.level).isEqualTo(TrickLevel.Strong)
        assertThat(on.trick!!.speed).isEqualTo(TrickSpeed.Fast)
    }
}
