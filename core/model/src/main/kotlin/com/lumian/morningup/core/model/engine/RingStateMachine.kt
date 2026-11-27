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
import com.lumian.morningup.core.model.engine.RingEffect.SnoozeStarted
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
        // 멈춘 앞 알람 가운데 이 사건으로 없어질 것(남은 확인 멈춤)을 먼저 덜어 내요.
        val (active, early) = pruneSuspended(session, event)
        val t = withSuspendedResumed(active, withEscapeCleared(active, route(active, event, now, inCall)), now)
        return if (early.isEmpty()) t else t.copy(effects = early + t.effects)
    }

    /**
     * 끼어든 알람이 끝나면 멈춘 앞 알람([RingSession.suspended])을 이어가요(결정 200 3, 4번).
     * 끼어든 알람이 미션을 끝내 확인(W2, W3)을 기다리게 됐는데 앞 알람이 쉬는 중이면, 다시 울려야 하는 앞 알람을
     * 먼저 이어가고 끼어든 알람의 확인을 대신 멈춰 둬요. 타이머는 한 칸뿐이라 이어가는 알람 것으로 다시 걸어요.
     */
    private fun withSuspendedResumed(before: RingSession, t: Transition, now: Long): Transition {
        val waiting = before.suspended ?: return t
        val after = t.session
        return when {
            after == null -> {
                val back = resumed(waiting, now)
                Transition(back, t.effects + timerOf(back))
            }
            after.phase.isCheck && !before.phase.isCheck && waiting.phase == RingPhase.Resting -> {
                val later = suspendedForLater(after.copy(suspended = waiting.suspended))
                val back = resumed(waiting, now).copy(suspended = later)
                Transition(back, t.effects + timerOf(back))
            }
            else -> t
        }
    }

    /** 멈춰 둔 알람을 다시 이어가요. 기다리던 때가 이미 지났으면 바로 다음 단계로 가요. */
    private fun resumed(waiting: RingSession, now: Long) =
        waiting.copy(phaseEndsAt = maxOf(waiting.phaseEndsAt, now))

    private fun timerOf(s: RingSession) = ScheduleTimer(s.phaseEndsAt, TimerKind.entries.first { it.phase == s.phase })

    /**
     * 다른 알람이 끼어들어 멈춰 둘 모습. W2, W3를 묻던 중이면 기다리는 단계로 돌려 이어갈 때 바로 다시 물어요.
     * 미션 비상구 알림은 새 알람이 울리며 지워져서 다음 미션 때 다시 띄우게 해요.
     */
    private fun suspendedForLater(s: RingSession): RingSession = when (s.phase) {
        RingPhase.WakeCheckAsking -> s.copy(phase = RingPhase.AwaitingWakeCheck, phaseEndsAt = 0L)
        RingPhase.RecheckAsking -> s.copy(phase = RingPhase.AwaitingRecheck, phaseEndsAt = 0L)
        else -> s
    }.copy(pausedLeftMillis = null, escapeShown = false)

    /**
     * 멈춘 앞 알람에도 닿는 사건. 그 알람의 남은 확인을 멈추거나(알람 끔, 지움, 저장) 전체 설정에서 한 번 더 확인을
     * 껐으면 멈춘 알람에서도 덜어 내요. 덜어 낸 것은 진단 기록만 남겨요(지금 울림의 화면과 알림은 그대로예요).
     */
    private fun pruneSuspended(session: RingSession, event: RingEvent): Pair<RingSession, List<RingEffect>> {
        val stack = session.suspended ?: return session to emptyList()
        val (rest, dropped) = when (event) {
            is RingEvent.CancelChecks -> pruned(stack, { it.plan.alarmId == event.alarmId && it.phase.isCheck })
            RingEvent.RecheckTurnedOff -> pruned(
                stack,
                drop = { it.phase == RingPhase.AwaitingRecheck },
                keep = { it.copy(plan = it.plan.copy(recheckEnabled = false)) },
            )
            else -> return session to emptyList()
        }
        val effects = dropped.map { WakeResultRecorded(it.plan.alarmId, WakeResult.Cancelled, it.rounds) }
        return session.copy(suspended = rest) to effects
    }

    private fun pruned(
        s: RingSession?,
        drop: (RingSession) -> Boolean,
        keep: (RingSession) -> RingSession = { it },
    ): Pair<RingSession?, List<RingSession>> {
        if (s == null) return null to emptyList()
        val (rest, dropped) = pruned(s.suspended, drop, keep)
        return if (drop(s)) rest to dropped + s else keep(s).copy(suspended = rest) to dropped
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
            RingEvent.Snoozed -> snooze(session, now)
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
     * 알람 시각(결정 200, 더 엄격한 쪽을 남겨요).
     * - 같은 분에 먼저 울리기 시작한 알람보다 이 알람이 더 엄격하면(미션이 더 많거나, 같은 수면 먼저 만든 알람) 이 알람으로
     *   바꿔 울리고 앞 알람은 '지나감'이에요(1번, ISS-069). 아니면 이 알람이 '지나감'이에요.
     * - 다른 알람이 울리거나 미션 중이면 사람이 깨어 있어서 뒤 알람은 '지나감'이에요(2번).
     * - 앞 알람이 쉬는 중이거나 W2, W3를 기다리거나 묻는 중이면 새 알람이 울리고, 앞 알람은 멈춰 뒀다가
     *   새 알람이 끝나면 이어가요(3, 4번).
     * '지나감'이어도 다음 회차는 알람마다 따로 예약해요.
     */
    private fun alarmFired(session: RingSession?, plan: RingPlan, now: Long): Transition {
        if (session != null && replacesSameMinute(session, plan, now)) {
            val ringing = newRinging(plan, now, suspended = session.suspended)
            val effects = listOf(MarkPassed(session.plan.alarmId)) + ringEffects(ringing) +
                RescheduleAlarm(plan.alarmId)
            return Transition(ringing, effects)
        }
        if (session != null && !session.phase.isCheck && session.phase != RingPhase.Resting) {
            return Transition(session, listOf(MarkPassed(plan.alarmId), RescheduleAlarm(plan.alarmId)))
        }
        val ringing = newRinging(plan, now, suspended = session?.let(::suspendedForLater))
        return Transition(ringing, ringEffects(ringing) + RescheduleAlarm(plan.alarmId))
    }

    private fun newRinging(plan: RingPlan, now: Long, suspended: RingSession?) = RingSession(
        plan = plan,
        phase = RingPhase.Ringing,
        ringsLeft = plan.ringAgainCount,
        missionsDone = 0,
        phaseEndsAt = now + RING_MILLIS,
        snoozesLeft = plan.snoozeCount,
        firedAt = now,
        suspended = suspended,
    )

    /**
     * 앞 알람이 같은 분에 막 울리기 시작한 첫 울림이고(아직 아무도 손대지 않음), 새 알람이 더 엄격한지(결정 200 1번).
     * 미션 수가 많은 쪽, 같으면 먼저 만든 알람(id가 작은 쪽)이에요. 둘 다 미션이 없으면 바꾸지 않아요.
     */
    private fun replacesSameMinute(session: RingSession, plan: RingPlan, now: Long): Boolean {
        val untouched = session.phase == RingPhase.Ringing && session.rounds == 0 &&
            session.ringsLeft == session.plan.ringAgainCount && session.snoozesLeft == session.plan.snoozeCount
        val fired = session.firedAt
        val sameMinute = fired > 0 && now - fired < MINUTE && now / MINUTE == fired / MINUTE
        if (!untouched || !sameMinute) return false
        val mine = session.plan.missions.size
        val theirs = plan.missions.size
        return theirs > mine || (theirs == mine && theirs > 0 && plan.alarmId < session.plan.alarmId)
    }

    private fun ring(session: RingSession, now: Long, reset: Boolean = false): Transition {
        val next = session.copy(
            phase = RingPhase.Ringing,
            phaseEndsAt = now + RING_MILLIS,
            ringsLeft = if (reset) session.plan.ringAgainCount else session.ringsLeft,
            missionsDone = if (reset) 0 else session.missionsDone,
            rounds = if (reset) session.rounds + 1 else session.rounds,
            pausedLeftMillis = null,
            snoozed = false,
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
            snoozed = false,
            ringsLeft = left?.minus(1),
            phaseEndsAt = now + session.plan.intervalMinutes * MINUTE,
        )
        val total = session.plan.ringAgainCount
        val nextNumber = if (total != null && resting.ringsLeft != null) total - resting.ringsLeft + 1 else null
        val rest = RestStarted(resting.phaseEndsAt, session.plan.intervalMinutes, nextNumber)
        return Transition(resting, listOf(StopSound, rest, ScheduleTimer(resting.phaseEndsAt, TimerKind.RestEnded)))
    }

    /**
     * D0 'n분 뒤 다시'(결정 195). 소리를 멈추고 울림 화면을 닫은 뒤 미루기 간격 뒤에 다시 울려요. 쉬는 틀은 '다시 울림'
     * 쉬는 중([RingPhase.Resting])과 같고, 미루기 횟수만 하나 줄어요. 다시 울림 횟수([RingSession.ringsLeft])는 그대로예요.
     */
    private fun snooze(session: RingSession, now: Long): Transition {
        if (!session.canSnooze) return NoChange(session)
        val resting = session.copy(
            phase = RingPhase.Resting,
            snoozed = true,
            snoozesLeft = session.snoozesLeft - 1,
            phaseEndsAt = now + session.plan.snoozeMinutes * MINUTE,
        )
        val started = SnoozeStarted(resting.phaseEndsAt, session.plan.snoozeMinutes, resting.snoozesLeft)
        return Transition(resting, listOf(StopSound, started, ScheduleTimer(resting.phaseEndsAt, TimerKind.RestEnded)))
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
