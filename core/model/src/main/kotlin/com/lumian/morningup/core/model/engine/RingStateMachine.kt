package com.lumian.morningup.core.model.engine

import com.lumian.morningup.core.model.engine.RingEffect.CancelTimer
import com.lumian.morningup.core.model.engine.RingEffect.MarkPassed
import com.lumian.morningup.core.model.engine.RingEffect.PlaySound
import com.lumian.morningup.core.model.engine.RingEffect.PostMissedNotification
import com.lumian.morningup.core.model.engine.RingEffect.RescheduleAlarm
import com.lumian.morningup.core.model.engine.RingEffect.RestStarted
import com.lumian.morningup.core.model.engine.RingEffect.ScheduleTimer
import com.lumian.morningup.core.model.engine.RingEffect.SessionEnded
import com.lumian.morningup.core.model.engine.RingEffect.ShowMission
import com.lumian.morningup.core.model.engine.RingEffect.ShowRingScreen
import com.lumian.morningup.core.model.engine.RingEffect.ShowWakeCheck
import com.lumian.morningup.core.model.engine.RingEffect.ShowWakeUp
import com.lumian.morningup.core.model.engine.RingEffect.StopSound

/**
 * 울림 상태표(ADR 0002). 같은 입력이면 늘 같은 결과를 내는 순수 함수예요.
 * 시각은 epoch 밀리초예요.
 */
object RingStateMachine {
    /** 아무도 안 끌 때 한 번에 울리는 길이. 11-16 F4는 1분, 11-19 사용자 결정으로 2분이에요. */
    const val RING_MILLIS = 120_000L
    const val MISSION_MILLIS = 59_000L
    private const val FULL_VOLUME = 1f
    private const val MINUTE = 60_000L
    private const val SECOND = 1_000L

    /** AlarmManager가 몇 밀리초 일찍 깨워도 받아들이는 여유. */
    private const val TIMER_SLACK_MILLIS = 2_000L

    private val NoChange: (RingSession?) -> Transition = { Transition(it, emptyList()) }

    fun reduce(session: RingSession?, event: RingEvent, now: Long): Transition {
        if (event is RingEvent.AlarmFired) return alarmFired(session, event.plan, now)
        if (session == null) return NoChange(null)
        return when (event) {
            is RingEvent.TimerFired -> timerFired(session, event.kind, now)
            RingEvent.MissionStarted -> missionStarted(session, now)
            RingEvent.MissionInteraction -> missionInteraction(session, now)
            RingEvent.MissionCompleted -> missionCompleted(session, now)
            RingEvent.DismissWithoutMission -> dismissWithoutMission(session, now)
            RingEvent.WakeCheckConfirmed ->
                if (session.phase == RingPhase.WakeCheckAsking) dismissed(session) else NoChange(session)
            is RingEvent.AlarmFired -> error("위에서 처리했어요")
        }
    }

    /**
     * 앱이나 서비스가 죽었다 다시 살아났을 때 저장된 상태에서 다시 할 일. 소리, 화면, 타이머를 되살려요.
     * 이미 지난 타이머는 AlarmManager가 바로 깨워서 상태표가 이어서 처리해요.
     */
    fun resume(session: RingSession): List<RingEffect> {
        val timer = ScheduleTimer(session.phaseEndsAt, TimerKind.entries.first { it.phase == session.phase })
        return when (session.phase) {
            RingPhase.Ringing -> listOf(PlaySound(FULL_VOLUME), ShowRingScreen, timer)
            RingPhase.InMission -> listOf(StopSound, ShowMission(session.missionsDone), timer)
            RingPhase.WakeCheckAsking -> listOf(ShowWakeCheck, timer)
            RingPhase.Resting, RingPhase.AwaitingWakeCheck -> listOf(timer)
        }
    }

    private fun dismissWithoutMission(session: RingSession, now: Long): Transition =
        if (session.plan.missions.isEmpty() && session.phase.isAlarmPhase) finish(session, now) else NoChange(session)

    private val RingPhase.isAlarmPhase get() = this == RingPhase.Ringing || this == RingPhase.Resting

    private fun alarmFired(session: RingSession?, plan: RingPlan, now: Long): Transition {
        if (session != null) {
            return Transition(session, listOf(MarkPassed(plan.alarmId), RescheduleAlarm(plan.alarmId)))
        }
        val ringing = RingSession(
            plan = plan,
            phase = RingPhase.Ringing,
            ringsLeft = plan.ringAgainCount,
            missionsDone = 0,
            phaseEndsAt = now + RING_MILLIS,
        )
        return Transition(ringing, ringEffects(ringing) + RescheduleAlarm(plan.alarmId))
    }

    private fun ring(session: RingSession, now: Long, reset: Boolean = false): Transition {
        val next = session.copy(
            phase = RingPhase.Ringing,
            phaseEndsAt = now + RING_MILLIS,
            ringsLeft = if (reset) session.plan.ringAgainCount else session.ringsLeft,
            missionsDone = if (reset) 0 else session.missionsDone,
        )
        return Transition(next, ringEffects(next))
    }

    private fun ringEffects(s: RingSession) =
        listOf(PlaySound(FULL_VOLUME), ShowRingScreen, ScheduleTimer(s.phaseEndsAt, TimerKind.RingTimeout))

    private fun timerFired(session: RingSession, kind: TimerKind, now: Long): Transition {
        val valid = session.phase == kind.phase && now >= session.phaseEndsAt - TIMER_SLACK_MILLIS
        if (!valid) return NoChange(session)
        return when (kind) {
            TimerKind.RingTimeout -> ringTimeout(session, now)
            TimerKind.RestEnded -> ring(session, now)
            TimerKind.MissionTimeout -> ring(session, now)
            TimerKind.WakeCheckAsk -> {
                val asking = session.copy(
                    phase = RingPhase.WakeCheckAsking,
                    phaseEndsAt = now + session.plan.wakeCheckAnswerSeconds * SECOND,
                )
                Transition(asking, listOf(ShowWakeCheck, ScheduleTimer(asking.phaseEndsAt, TimerKind.WakeCheckTimeout)))
            }
            TimerKind.WakeCheckTimeout -> ring(session, now, reset = true)
        }
    }

    private fun ringTimeout(session: RingSession, now: Long): Transition {
        val left = session.ringsLeft
        if (left != null && left <= 0) {
            val id = session.plan.alarmId
            return Transition(
                null,
                listOf(StopSound, CancelTimer, PostMissedNotification(id), SessionEnded(id, RingOutcome.Missed)),
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

    /** 미션을 다 끝냈거나 미션 없는 알람을 껐어요. W2가 켜져 있으면 기다리고, 아니면 끝나요. */
    private fun finish(session: RingSession, now: Long): Transition {
        if (!session.plan.wakeCheckEnabled) {
            val ended = SessionEnded(session.plan.alarmId, RingOutcome.Dismissed)
            return Transition(null, listOf(StopSound, CancelTimer, ShowWakeUp, ended))
        }
        val waiting = session.copy(
            phase = RingPhase.AwaitingWakeCheck,
            phaseEndsAt = now + session.plan.wakeCheckAskAfterMinutes * MINUTE,
        )
        val ask = ScheduleTimer(waiting.phaseEndsAt, TimerKind.WakeCheckAsk)
        return Transition(waiting, listOf(StopSound, ShowWakeUp, ask))
    }

    private fun dismissed(session: RingSession) =
        Transition(null, listOf(StopSound, CancelTimer, SessionEnded(session.plan.alarmId, RingOutcome.Dismissed)))
}
