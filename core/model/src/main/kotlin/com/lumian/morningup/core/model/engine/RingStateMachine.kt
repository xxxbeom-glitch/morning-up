package com.lumian.morningup.core.model.engine

import com.lumian.morningup.core.model.engine.RingEffect.CancelTimer
import com.lumian.morningup.core.model.engine.RingEffect.MarkPassed
import com.lumian.morningup.core.model.engine.RingEffect.MultiAskRecorded
import com.lumian.morningup.core.model.engine.RingEffect.PausedForCall
import com.lumian.morningup.core.model.engine.RingEffect.PlayChime
import com.lumian.morningup.core.model.engine.RingEffect.PlaySound
import com.lumian.morningup.core.model.engine.RingEffect.PostMissedNotification
import com.lumian.morningup.core.model.engine.RingEffect.RecheckScheduled
import com.lumian.morningup.core.model.engine.RingEffect.RescheduleAlarm
import com.lumian.morningup.core.model.engine.RingEffect.RestStarted
import com.lumian.morningup.core.model.engine.RingEffect.ScheduleTimer
import com.lumian.morningup.core.model.engine.RingEffect.SessionEnded
import com.lumian.morningup.core.model.engine.RingEffect.ShowMission
import com.lumian.morningup.core.model.engine.RingEffect.ShowRecheck
import com.lumian.morningup.core.model.engine.RingEffect.ShowRingScreen
import com.lumian.morningup.core.model.engine.RingEffect.ShowWakeCheck
import com.lumian.morningup.core.model.engine.RingEffect.ShowWakeUp
import com.lumian.morningup.core.model.engine.RingEffect.StopSound
import com.lumian.morningup.core.model.engine.RingEffect.WakeResultRecorded
import kotlin.random.Random

/**
 * 울림 상태표(ADR 0002). 같은 입력이면 늘 같은 결과를 내는 순수 함수예요.
 * 시각은 epoch 밀리초예요.
 */
object RingStateMachine {
    /** 아무도 안 끌 때 한 번에 울리는 길이. 11-16 F4는 1분, 11-19 사용자 결정으로 2분이에요. */
    const val RING_MILLIS = 120_000L
    const val MISSION_MILLIS = 59_000L

    /** W2, W3에 답하지 못해 처음부터 다시 울리는 것은 알람 하나에 2번까지예요(결정 145 6번). */
    const val MAX_ROUNDS = 2
    private const val FULL_VOLUME = 1f
    private const val MINUTE = 60_000L
    private const val SECOND = 1_000L

    /** 여러 번 묻기: 둘째부터는 앞 확인 뒤 5~15분 사이 무작위(결정 146). */
    const val MULTI_GAP_MIN_MINUTES = 5
    const val MULTI_GAP_MAX_MINUTES = 15

    /** 여러 번 묻기: 한 바퀴는 그 바퀴의 미션 끝에서 60분 안에서만 물어요. */
    const val MULTI_CAP_MINUTES = 60

    /** AlarmManager가 몇 밀리초 일찍 깨워도 받아들이는 여유. */
    private const val TIMER_SLACK_MILLIS = 2_000L

    private val NoChange: (RingSession?) -> Transition = { Transition(it, emptyList()) }

    /**
     * [inCall]은 지금 통화 중인지예요. 통화 중이면 W2, W3를 띄우지 않고 멈춰 둬요.
     * [random]은 여러 번 묻기의 무작위 간격을 고를 때만 써요(시험에서는 씨앗을 정해 넣어요).
     */
    fun reduce(
        session: RingSession?,
        event: RingEvent,
        now: Long,
        inCall: Boolean = false,
        random: Random = Random.Default,
    ): Transition {
        if (event is RingEvent.AlarmFired) return alarmFired(session, event.plan, now)
        if (session == null) return NoChange(null)
        return when (event) {
            is RingEvent.TimerFired -> timerFired(session, event.kind, now, inCall)
            RingEvent.MissionStarted -> missionStarted(session, now)
            RingEvent.MissionInteraction -> missionInteraction(session, now)
            RingEvent.MissionCompleted -> missionCompleted(session, now)
            RingEvent.DismissWithoutMission -> dismissWithoutMission(session, now)
            RingEvent.WakeCheckConfirmed ->
                if (session.phase == RingPhase.WakeCheckAsking) wakeCheckPassed(session, now, random) else NoChange(session)
            RingEvent.RecheckConfirmed -> recheckConfirmed(session, now, random)
            is RingEvent.CallChanged -> callChanged(session, event.inCall, now)
            is RingEvent.CancelChecks -> cancelChecks(session, event.alarmId)
            RingEvent.RecheckTurnedOff -> recheckTurnedOff(session)
            is RingEvent.AlarmFired -> error("위에서 처리했어요")
        }
    }

    /**
     * 앱이나 서비스가 죽었다 다시 살아났을 때 저장된 상태에서 다시 할 일. 소리, 화면, 타이머를 되살려요.
     * 이미 지난 타이머는 AlarmManager가 바로 깨워서 상태표가 이어서 처리해요.
     * 통화 때문에 멈춰 둔 W2, W3는 그대로 멈춰 두고, 컨트롤러가 지금 통화 상태를 보내 이어가요.
     */
    fun resume(session: RingSession): List<RingEffect> {
        if (session.pausedLeftMillis != null) return listOf(PausedForCall)
        val timer = ScheduleTimer(session.phaseEndsAt, TimerKind.entries.first { it.phase == session.phase })
        return when (session.phase) {
            RingPhase.Ringing -> listOf(PlaySound(FULL_VOLUME), ShowRingScreen, timer)
            RingPhase.InMission -> listOf(StopSound, ShowMission(session.missionsDone), timer)
            RingPhase.WakeCheckAsking -> listOf(ShowWakeCheck, timer)
            RingPhase.RecheckAsking -> listOf(ShowRecheck, timer)
            RingPhase.Resting, RingPhase.AwaitingWakeCheck, RingPhase.AwaitingRecheck -> listOf(timer)
        }
    }

    private fun dismissWithoutMission(session: RingSession, now: Long): Transition =
        if (session.plan.missions.isEmpty() && session.phase.isAlarmPhase) finish(session, now) else NoChange(session)

    private val RingPhase.isAlarmPhase get() = this == RingPhase.Ringing || this == RingPhase.Resting

    /**
     * 알람 시각. 다른 알람이 울리거나 미션 중이면 뒤 알람은 '지나감'이에요. 다만 앞 알람이 W2, W3를 기다리거나
     * 묻는 중이면 앞 알람의 남은 확인을 멈추고 새 알람이 울려요(결정 145 7번).
     */
    private fun alarmFired(session: RingSession?, plan: RingPlan, now: Long): Transition {
        if (session != null && !session.phase.isCheck) {
            return Transition(session, listOf(MarkPassed(plan.alarmId), RescheduleAlarm(plan.alarmId)))
        }
        val ringing = RingSession(
            plan = plan,
            phase = RingPhase.Ringing,
            ringsLeft = plan.ringAgainCount,
            missionsDone = 0,
            phaseEndsAt = now + RING_MILLIS,
        )
        val previous = session?.let { cancelled(it).effects }.orEmpty()
        return Transition(ringing, previous + ringEffects(ringing) + RescheduleAlarm(plan.alarmId))
    }

    private fun ring(session: RingSession, now: Long, reset: Boolean = false): Transition {
        val next = session.copy(
            phase = RingPhase.Ringing,
            phaseEndsAt = now + RING_MILLIS,
            ringsLeft = if (reset) session.plan.ringAgainCount else session.ringsLeft,
            missionsDone = if (reset) 0 else session.missionsDone,
            rounds = if (reset) session.rounds + 1 else session.rounds,
            pausedLeftMillis = null,
        )
        return Transition(next, ringEffects(next))
    }

    private fun ringEffects(s: RingSession) =
        listOf(PlaySound(FULL_VOLUME), ShowRingScreen, ScheduleTimer(s.phaseEndsAt, TimerKind.RingTimeout))

    private fun timerFired(session: RingSession, kind: TimerKind, now: Long, inCall: Boolean): Transition {
        val valid = session.phase == kind.phase &&
            now >= session.phaseEndsAt - TIMER_SLACK_MILLIS &&
            session.pausedLeftMillis == null
        if (!valid) return NoChange(session)
        return when (kind) {
            TimerKind.RingTimeout -> ringTimeout(session, now)
            TimerKind.RestEnded -> ring(session, now)
            TimerKind.MissionTimeout -> ring(session, now)
            TimerKind.WakeCheckAsk ->
                ask(session, RingPhase.WakeCheckAsking, session.plan.wakeCheckAnswerSeconds * SECOND, now, inCall)
            TimerKind.RecheckAsk -> ask(
                session.copy(earlyUsed = false),
                RingPhase.RecheckAsking,
                session.plan.recheckAnswerMinutes * MINUTE,
                now,
                inCall,
            )
            // 시간 안에 못 하면 처음부터 다시 울리고, 미션과 다시 울림 횟수도 처음 값이에요(결정 136). 바퀴를 하나 세요.
            // 여러 번 묻기면 마친 확인 수는 그대로 두고, 다시 미션을 끝낸 뒤 남은 확인만 물어요(결정 146).
            TimerKind.WakeCheckTimeout, TimerKind.RecheckTimeout -> {
                val again = ring(session.copy(earlyUsed = false), now, reset = true)
                again.copy(effects = multiRecord(session, MultiAskKind.Miss, session.checksDone + 1) + again.effects)
            }
        }
    }

    /** 여러 번 묻기 기록 하나. 꺼져 있으면 빈 목록이에요. */
    private fun multiRecord(session: RingSession, kind: MultiAskKind, number: Int, left: Int? = null) =
        session.plan.askTotal?.let { listOf(MultiAskRecorded(session.plan.alarmId, kind, number, it, left)) }
            .orEmpty()

    /** W2나 W3를 띄워요. 통화 중이면 띄우지 않고 시간째로 멈춰 둬요(결정 145 '통화 중'). */
    private fun ask(
        session: RingSession,
        phase: RingPhase,
        answerMillis: Long,
        now: Long,
        inCall: Boolean,
    ): Transition {
        val asking = session.copy(phase = phase, phaseEndsAt = now + answerMillis)
        if (inCall) return Transition(asking.copy(pausedLeftMillis = answerMillis), listOf(PausedForCall))
        return Transition(asking, askEffects(asking))
    }

    private fun askEffects(asking: RingSession): List<RingEffect> {
        val recheck = asking.phase == RingPhase.RecheckAsking
        val timeout = if (recheck) TimerKind.RecheckTimeout else TimerKind.WakeCheckTimeout
        return listOf(
            if (recheck) ShowRecheck else ShowWakeCheck,
            PlayChime,
            ScheduleTimer(asking.phaseEndsAt, timeout),
        )
    }

    /** 통화가 시작되면 묻던 W2, W3를 남은 시간째로 멈추고, 끝나면 남은 시간으로 다시 물어요. */
    private fun callChanged(session: RingSession, inCall: Boolean, now: Long): Transition {
        val paused = session.pausedLeftMillis
        return when {
            inCall && paused == null && session.phase.isAsking -> {
                val left = (session.phaseEndsAt - now).coerceAtLeast(SECOND)
                Transition(session.copy(pausedLeftMillis = left), listOf(CancelTimer, PausedForCall))
            }
            !inCall && paused != null -> {
                val resumed = session.copy(phaseEndsAt = now + paused, pausedLeftMillis = null)
                Transition(resumed, askEffects(resumed))
            }
            else -> NoChange(session)
        }
    }

    private fun ringTimeout(session: RingSession, now: Long): Transition {
        val left = session.ringsLeft
        if (left != null && left <= 0) {
            val id = session.plan.alarmId
            val result = if (session.plan.wakeCheckEnabled) {
                listOf(WakeResultRecorded(id, WakeResult.Missed, session.rounds))
            } else {
                emptyList()
            }
            return Transition(
                null,
                listOf(StopSound, CancelTimer, PostMissedNotification(id)) + result +
                    SessionEnded(id, RingOutcome.Missed),
            )
        }
        val resting = session.copy(
            phase = RingPhase.Resting,
            ringsLeft = left?.minus(1),
            phaseEndsAt = now + session.plan.intervalMinutes * MINUTE,
        )
        val total = session.plan.ringAgainCount
        val nextNumber = if (total != null && resting.ringsLeft != null) total - resting.ringsLeft + 1 else null
        val rest = RestStarted(resting.phaseEndsAt, session.plan.intervalMinutes, nextNumber)
        return Transition(resting, listOf(StopSound, rest, ScheduleTimer(resting.phaseEndsAt, TimerKind.RestEnded)))
    }

    private fun missionStarted(session: RingSession, now: Long): Transition {
        if (!session.phase.isAlarmPhase) return NoChange(session)
        if (session.plan.missions.isEmpty()) return finish(session, now)
        return startMission(session, now)
    }

    private fun startMission(session: RingSession, now: Long): Transition {
        val mission = session.copy(phase = RingPhase.InMission, phaseEndsAt = now + MISSION_MILLIS)
        return Transition(
            mission,
            listOf(
                // 미션 중에는 사용자가 손을 쓰는 중이라 소리를 끄고 진동도 멈춰요(11-17 사용자 지시).
                StopSound,
                ShowMission(mission.missionsDone),
                ScheduleTimer(mission.phaseEndsAt, TimerKind.MissionTimeout),
            ),
        )
    }

    private fun missionInteraction(session: RingSession, now: Long): Transition {
        if (session.phase != RingPhase.InMission) return NoChange(session)
        val refilled = session.copy(phaseEndsAt = now + MISSION_MILLIS)
        return Transition(refilled, listOf(ScheduleTimer(refilled.phaseEndsAt, TimerKind.MissionTimeout)))
    }

    private fun missionCompleted(session: RingSession, now: Long): Transition {
        if (session.phase != RingPhase.InMission) return NoChange(session)
        val done = session.copy(missionsDone = session.missionsDone + 1)
        return if (done.missionsDone >= done.plan.missions.size) finish(done, now) else startMission(done, now)
    }

    /**
     * 미션을 다 끝냈거나 미션 없는 알람을 껐어요. W2가 켜져 있으면 기다리고, 아니면 끝나요.
     * 이미 [MAX_ROUNDS]번 다시 울렸으면 W2, W3 없이 W1에서 끝나요(결정 145 6번).
     */
    private fun finish(session: RingSession, now: Long): Transition {
        val capped = session.rounds >= MAX_ROUNDS
        if (!session.plan.wakeCheckEnabled || capped) {
            val id = session.plan.alarmId
            val result = if (session.plan.wakeCheckEnabled) {
                listOf(WakeResultRecorded(id, WakeResult.EndedByCap, session.rounds))
            } else {
                emptyList()
            }
            val ended = SessionEnded(id, RingOutcome.Dismissed)
            return Transition(null, listOf(StopSound, CancelTimer, ShowWakeUp) + result + ended)
        }
        val waiting = session.copy(
            phase = RingPhase.AwaitingWakeCheck,
            phaseEndsAt = now + session.plan.wakeCheckAskAfterMinutes * MINUTE,
            roundStartedAt = now,
        )
        val ask = ScheduleTimer(waiting.phaseEndsAt, TimerKind.WakeCheckAsk)
        return Transition(waiting, listOf(StopSound, ShowWakeUp, ask))
    }

    /** W2 '깼어요'. 한 번 더 확인이 켜져 있으면 같은 간격 뒤 W3를 물어요(결정 145). */
    private fun wakeCheckPassed(session: RingSession, now: Long, random: Random): Transition {
        if (session.pausedLeftMillis != null) return NoChange(session)
        if (session.plan.askTotal != null) return multiPassed(session, now, random, WakeResult.WakeCheckPassed)
        if (!session.plan.recheckEnabled) return dismissed(session, WakeResult.WakeCheckPassed)
        val waiting = session.copy(
            phase = RingPhase.AwaitingRecheck,
            phaseEndsAt = now + session.plan.wakeCheckAskAfterMinutes * MINUTE,
        )
        return Transition(
            waiting,
            listOf(RecheckScheduled(waiting.phaseEndsAt), ScheduleTimer(waiting.phaseEndsAt, TimerKind.RecheckAsk)),
        )
    }

    /** W3 10초 길게 누르기를 마쳤어요. 기다리는 중에 알림이나 홈 띠로 연 경우는 미리 끝낸 거예요. */
    private fun recheckConfirmed(session: RingSession, now: Long, random: Random): Transition {
        val early = session.phase == RingPhase.AwaitingRecheck
        val asking = session.phase == RingPhase.RecheckAsking && session.pausedLeftMillis == null
        val result = if (early) WakeResult.RecheckPassedEarly else WakeResult.RecheckPassed
        return when {
            session.plan.askTotal != null && (asking || (early && !session.earlyUsed)) ->
                multiPassed(session.copy(earlyUsed = early), now, random, result)
            session.plan.askTotal != null -> NoChange(session)
            early || asking -> dismissed(session, result)
            else -> NoChange(session)
        }
    }

    /**
     * 여러 번 묻기에서 확인 하나를 마쳤어요(결정 146 3절). 다 마쳤으면 끝, 아니면 5~15분 무작위 뒤 다음 W3를 물어요.
     * 다음 물을 때가 이 바퀴의 미션 끝에서 60분을 넘으면 남은 확인을 없애고 끝나요.
     */
    private fun multiPassed(session: RingSession, now: Long, random: Random, result: WakeResult): Transition {
        val total = session.plan.askTotal ?: return dismissed(session, result)
        val done = session.copy(checksDone = session.checksDone + 1)
        val passed = multiRecord(session, MultiAskKind.Pass, done.checksDone)
        if (done.checksDone >= total) {
            val end = dismissed(done, result)
            return end.copy(effects = passed + end.effects)
        }
        val minute = session.plan.minuteMillis
        val gap = random.nextInt(MULTI_GAP_MIN_MINUTES, MULTI_GAP_MAX_MINUTES + 1)
        val askAt = now + gap * minute
        val roundStart = session.roundStartedAt ?: now
        if (askAt > roundStart + MULTI_CAP_MINUTES * minute) {
            val left = total - done.checksDone
            val end = dismissed(done, WakeResult.EndedByTimeCap)
            return end.copy(effects = passed + multiRecord(done, MultiAskKind.Cap, done.checksDone, left) + end.effects)
        }
        val waiting = done.copy(phase = RingPhase.AwaitingRecheck, phaseEndsAt = askAt, roundStartedAt = roundStart)
        return Transition(
            waiting,
            passed + listOf(
                StopSound,
                RecheckScheduled(askAt, number = done.checksDone + 1, total = total, gapMinutes = gap),
                ScheduleTimer(askAt, TimerKind.RecheckAsk),
            ),
        )
    }

    private fun cancelChecks(session: RingSession, alarmId: Long): Transition =
        if (session.plan.alarmId == alarmId && session.phase.isCheck) cancelled(session) else NoChange(session)

    private fun recheckTurnedOff(session: RingSession): Transition = when {
        // 여러 번 묻기를 켜면 '한 번 더 확인'은 잠겨 있어 끌 수 없어요. 이미 시작한 울림은 그대로 가요.
        session.plan.askTotal != null -> NoChange(session)
        session.phase == RingPhase.AwaitingRecheck || session.phase == RingPhase.RecheckAsking -> cancelled(session)
        session.plan.recheckEnabled -> {
            val plan = session.plan.copy(recheckEnabled = false)
            Transition(session.copy(plan = plan), emptyList())
        }
        else -> NoChange(session)
    }

    private fun dismissed(session: RingSession, result: WakeResult): Transition {
        val id = session.plan.alarmId
        return Transition(
            null,
            listOf(
                StopSound,
                CancelTimer,
                WakeResultRecorded(id, result, session.rounds),
                SessionEnded(id, RingOutcome.Dismissed),
            ),
        )
    }

    /** 남은 확인을 멈춰요. 알림과 타이머를 지우고 조용히 끝나요. */
    private fun cancelled(session: RingSession): Transition {
        val id = session.plan.alarmId
        return Transition(
            null,
            listOf(
                StopSound,
                CancelTimer,
                WakeResultRecorded(id, WakeResult.Cancelled, session.rounds),
                SessionEnded(id, RingOutcome.Cancelled),
            ),
        )
    }
}
