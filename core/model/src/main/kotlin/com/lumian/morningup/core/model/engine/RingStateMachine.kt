package com.lumian.morningup.core.model.engine

import com.lumian.morningup.core.model.engine.RingEffect.CancelTimer
import com.lumian.morningup.core.model.engine.RingEffect.ClearEscape
import com.lumian.morningup.core.model.engine.RingEffect.EscapeUsed
import com.lumian.morningup.core.model.engine.RingEffect.MarkPassed
import com.lumian.morningup.core.model.engine.RingEffect.PausedForCall
import com.lumian.morningup.core.model.engine.RingEffect.PlayChime
import com.lumian.morningup.core.model.engine.RingEffect.PlaySound
import com.lumian.morningup.core.model.engine.RingEffect.PostMissedNotification
import com.lumian.morningup.core.model.engine.RingEffect.RecheckScheduled
import com.lumian.morningup.core.model.engine.RingEffect.RescheduleAlarm
import com.lumian.morningup.core.model.engine.RingEffect.RestStarted
import com.lumian.morningup.core.model.engine.RingEffect.ScheduleTimer
import com.lumian.morningup.core.model.engine.RingEffect.SessionEnded
import com.lumian.morningup.core.model.engine.RingEffect.ShowEscape
import com.lumian.morningup.core.model.engine.RingEffect.ShowMission
import com.lumian.morningup.core.model.engine.RingEffect.ShowRecheck
import com.lumian.morningup.core.model.engine.RingEffect.ShowRingScreen
import com.lumian.morningup.core.model.engine.RingEffect.ShowWakeCheck
import com.lumian.morningup.core.model.engine.RingEffect.ShowWakeUp
import com.lumian.morningup.core.model.engine.RingEffect.StopSound
import com.lumian.morningup.core.model.engine.RingEffect.WakeResultRecorded

/**
 * 울림 상태표(ADR 0002). 같은 입력이면 늘 같은 결과를 내는 순수 함수예요.
 * 시각은 epoch 밀리초예요.
 */
object RingStateMachine {
    /** 아무도 안 끌 때 한 번에 울리는 길이. 11-16 F4는 1분, 11-19 사용자 결정으로 2분이에요. */
    const val RING_MILLIS = 120_000L
    const val MISSION_MILLIS = 59_000L

    /**
     * M5 미션 포기에 있는 동안 걸어 두는 미션 시간(ISS-051). M5는 마지막으로 쓴 뒤 1분이 지나야 미션으로 돌아가니,
     * 그 1분에 돌아간 뒤의 미션 시간을 더해요. 그래야 M5에 있는 동안 다시 울리지 않아요.
     */
    const val ESCAPE_HOLD_MILLIS = MissionEscape.IDLE_MILLIS + MISSION_MILLIS

    /** W2, W3에 답하지 못해 처음부터 다시 울리는 것은 알람 하나에 2번까지예요(결정 145 6번). */
    const val MAX_ROUNDS = 2
    private const val FULL_VOLUME = 1f
    private const val MINUTE = 60_000L
    private const val SECOND = 1_000L

    /** AlarmManager가 몇 밀리초 일찍 깨워도 받아들이는 여유. */
    private const val TIMER_SLACK_MILLIS = 2_000L

    private val NoChange: (RingSession?) -> Transition = { Transition(it, emptyList()) }

    /**
     * [inCall]은 지금 통화 중인지예요. 통화 중이면 W2, W3를 띄우지 않고 멈춰 둬요.
     */
    fun reduce(session: RingSession?, event: RingEvent, now: Long, inCall: Boolean = false): Transition {
        if (event is RingEvent.AlarmFired) return withEscapeCleared(session, alarmFired(session, event.plan, now))
        if (session == null) return NoChange(null)
        return withEscapeCleared(session, route(session, event, now, inCall))
    }

    /**
     * 미션 비상구 알림이 떠 있던 세션이 알림 없는 상태로 바뀌면(미션 끝, 써서 끔, 울림 끝) 알림을 지워요.
     * 써서 끈 경우의 '알람을 껐어요'는 다른 알림이라 함께 지워지지 않아요.
     */
    private fun withEscapeCleared(before: RingSession?, t: Transition): Transition {
        val cleared = before?.escapeShown == true && t.session?.escapeShown != true
        if (!cleared) return t
        // 세션이 끝나는 경우 '끝남'이 늘 마지막 효과가 되게 그 앞에 넣어요.
        val end = t.effects.indexOfFirst { it is SessionEnded }.takeIf { it >= 0 } ?: t.effects.size
        return t.copy(effects = t.effects.take(end) + ClearEscape + t.effects.drop(end))
    }

    private fun route(session: RingSession, event: RingEvent, now: Long, inCall: Boolean): Transition {
        return when (event) {
            is RingEvent.TimerFired -> timerFired(session, event.kind, now, inCall)
            RingEvent.MissionStarted -> missionStarted(session, now)
            RingEvent.MissionInteraction -> missionInteraction(session, now)
            RingEvent.MissionCompleted -> missionCompleted(session, now)
            RingEvent.DismissWithoutMission -> dismissWithoutMission(session, now)
            RingEvent.MissionEscaped -> missionEscaped(session, now)
            RingEvent.EscapeOpened, RingEvent.EscapeTyped -> escapeOpened(session, now)
            RingEvent.WakeCheckConfirmed ->
                if (session.phase == RingPhase.WakeCheckAsking) {
                    wakeCheckPassed(session, now)
                } else {
                    NoChange(session)
                }
            RingEvent.RecheckConfirmed -> recheckConfirmed(session, now)
            is RingEvent.CallChanged -> callChanged(session, event.inCall, now)
            is RingEvent.CancelChecks -> cancelChecks(session, event.alarmId)
            RingEvent.RecheckTurnedOff -> recheckTurnedOff(session)
            is RingEvent.AlarmFired -> error("reduce에서 처리했어요")
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
        // 미션 비상구 알림은 앱이 죽으면 남아 있어도 바로 답장 받는 곳이 없어질 수 있어서 같은 예문으로 다시 올려요.
        val escape = if (session.escapeShown) listOf(ShowEscape(fresh = false)) else emptyList()
        return escape + when (session.phase) {
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
     * 알람 시각. 다른 알람이 울리거나 미션 중이면 사람이 깨어 있어서 뒤 알람은 '지나감'이에요. 다만 앞 알람이
     * W2, W3를 기다리거나 묻는 중이면 앞 알람의 남은 확인을 멈추고 새 알람이 울려요(결정 145 7번).
     * 앞 알람이 쉬는 중(다시 울림 사이)이어도 새 알람이 울리고 앞 알람의 남은 울림은 끝내요(결정 166).
     */
    private fun alarmFired(session: RingSession?, plan: RingPlan, now: Long): Transition {
        if (session != null && !session.phase.isCheck && session.phase != RingPhase.Resting) {
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
            // 처음부터 다시 울린 바퀴에서는 첫 미션 때 비상구 알림을 다시 띄워요(결정 149 2절).
            escapeShown = if (reset) false else session.escapeShown,
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
            // 미션 중에 멈춰 다시 울리면, 다시 '미션 시작'을 눌렀을 때 첫 미션부터 해요(결정 189). 다시 울림 횟수는 그대로예요.
            TimerKind.MissionTimeout -> ring(session.copy(missionsDone = 0), now)
            TimerKind.WakeCheckAsk ->
                ask(session, RingPhase.WakeCheckAsking, session.plan.wakeCheckAnswerSeconds * SECOND, now, inCall)
            TimerKind.RecheckAsk -> ask(
                session.copy(earlyUsed = false),
                RingPhase.RecheckAsking,
                session.plan.recheckAnswerMinutes * session.plan.minuteMillis,
                now,
                inCall,
            )
            // 시간 안에 못 하면 처음부터 다시 울리고, 미션과 다시 울림 횟수도 처음 값이에요(결정 136). 바퀴를 하나 세요.
            // W3를 여러 번 물어도 마친 W3 수는 0부터 다시 세요(결정 176).
            TimerKind.WakeCheckTimeout, TimerKind.RecheckTimeout ->
                ring(session.copy(earlyUsed = false, checksDone = 0), now, reset = true)
        }
    }

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
        // 이 바퀴에서 처음 미션을 시작할 때 미션 비상구 알림을 한 번 띄워요(결정 149 2절). 그 뒤 59초가 지나 다시
        // 울리거나 미션을 다시 시작해도 같은 알림이 그대로 남아요.
        val showEscape = !session.escapeShown && MissionEscape.shouldShow(session)
        val mission = session.copy(
            phase = RingPhase.InMission,
            phaseEndsAt = now + MISSION_MILLIS,
            escapeShown = session.escapeShown || showEscape,
        )
        return Transition(
            mission,
            listOfNotNull(
                // 미션 중에는 사용자가 손을 쓰는 중이라 소리를 끄고 진동도 멈춰요(11-17 사용자 지시).
                StopSound,
                ShowMission(mission.missionsDone),
                ScheduleTimer(mission.phaseEndsAt, TimerKind.MissionTimeout),
                if (showEscape) ShowEscape() else null,
            ),
        )
    }

    /**
     * 미션 비상구로 맞게 따라 썼어요(결정 149). 미션을 다 끝낸 것과 같아서 남은 미션을 건너뛰고 W1, W2로 가요.
     * 알림이 떠 있는 동안(울림, 쉬는 중, 미션 중)만 받아요. 여러 번 묻기 횟수는 줄지 않아요(결정 151).
     */
    private fun missionEscaped(session: RingSession, now: Long): Transition {
        val open = session.phase.isAlarmPhase || session.phase == RingPhase.InMission
        if (!session.escapeShown || !open) return NoChange(session)
        val done = session.copy(missionsDone = session.plan.missions.size, escapeShown = false)
        val end = finish(done, now)
        return end.copy(effects = listOf(EscapeUsed(session.plan.alarmId)) + end.effects)
    }

    /**
     * M5 '미션 포기' 화면을 열었거나 M5에서 글을 썼어요(결정 171). 화면에 있는 동안 소리를 꺼요. 울림, 쉬는 중이면
     * 미션 중으로 넘겨 소리를 꺼요. 미션 시간은 [ESCAPE_HOLD_MILLIS](M5 1분 + 미션 시간)로 걸어서, M5에서 1분 동안
     * 안 써 미션으로 돌아가기 전에는 다시 울리지 않아요(ISS-051).
     * 미션 화면은 띄우지 않아요(지금 화면은 M5예요). 알림이 닫혀 있으면 받지 않아요.
     */
    private fun escapeOpened(session: RingSession, now: Long): Transition {
        if (!session.escapeShown) return NoChange(session)
        return when (session.phase) {
            RingPhase.InMission, RingPhase.Ringing, RingPhase.Resting -> {
                val mission = session.copy(phase = RingPhase.InMission, phaseEndsAt = now + ESCAPE_HOLD_MILLIS)
                val stop = if (session.phase == RingPhase.InMission) emptyList() else listOf(StopSound)
                Transition(
                    mission,
                    stop + ScheduleTimer(mission.phaseEndsAt, TimerKind.MissionTimeout),
                )
            }
            else -> NoChange(session)
        }
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
            escapeShown = false,
        )
        val ask = ScheduleTimer(waiting.phaseEndsAt, TimerKind.WakeCheckAsk)
        return Transition(waiting, listOf(StopSound, ShowWakeUp, ask))
    }

    /** W2 '깼어요'. 한 번 더 확인이 켜져 있으면 W3 기다리는 시간 뒤 W3를 물어요(결정 145, 176). */
    private fun wakeCheckPassed(session: RingSession, now: Long): Transition {
        if (session.pausedLeftMillis != null) return NoChange(session)
        if (!session.plan.recheckEnabled) return dismissed(session, WakeResult.WakeCheckPassed)
        return awaitRecheck(session.copy(checksDone = 0), now, emptyList())
    }

    /** [session.checksDone]번째까지 마친 뒤 다음 W3를 W3 기다리는 시간 뒤에 물어요. */
    private fun awaitRecheck(session: RingSession, now: Long, before: List<RingEffect>): Transition {
        val askAt = now + session.plan.recheckWaitMinutes * session.plan.minuteMillis
        val waiting = session.copy(phase = RingPhase.AwaitingRecheck, phaseEndsAt = askAt)
        val total = session.plan.recheckCount
        val scheduled = if (total > 1) {
            RecheckScheduled(askAt, number = session.checksDone + 1, total = total)
        } else {
            RecheckScheduled(askAt)
        }
        return Transition(waiting, before + listOf(scheduled, ScheduleTimer(askAt, TimerKind.RecheckAsk)))
    }

    /**
     * W3 10초 길게 누르기를 마쳤어요. 기다리는 중에 알림이나 홈 띠로 연 경우는 미리 끝낸 거예요.
     * 묻는 횟수를 다 채우면 끝나고, 아니면 W3 기다리는 시간 뒤 다음 W3를 물어요(결정 176).
     * 미리 끝내기는 기다림 한 번에 하나만이에요.
     */
    private fun recheckConfirmed(session: RingSession, now: Long): Transition {
        val early = session.phase == RingPhase.AwaitingRecheck && !session.earlyUsed
        val asking = session.phase == RingPhase.RecheckAsking && session.pausedLeftMillis == null
        if (!early && !asking) return NoChange(session)
        val result = if (early) WakeResult.RecheckPassedEarly else WakeResult.RecheckPassed
        val done = session.copy(checksDone = session.checksDone + 1, earlyUsed = early)
        if (done.checksDone >= done.plan.recheckCount) return dismissed(done, result)
        return awaitRecheck(done, now, listOf(StopSound))
    }

    private fun cancelChecks(session: RingSession, alarmId: Long): Transition =
        if (session.plan.alarmId == alarmId && session.phase.isCheck) cancelled(session) else NoChange(session)

    private fun recheckTurnedOff(session: RingSession): Transition = when {
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
