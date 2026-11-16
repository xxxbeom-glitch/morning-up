package com.lumian.morningup.core.model.engine

import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.MissionType
import kotlinx.serialization.Serializable

/** 울리기 시작할 때 알람에서 떼어 둔 값. 울리는 중에 알람을 고쳐도 이번 울림은 이 값대로 가요. */
@Serializable
data class RingPlan(
    val alarmId: Long,
    val missions: List<MissionType>,
    val intervalMinutes: Int,
    /** 처음 울림 뒤 더 울릴 횟수. null이면 끌 때까지. */
    val ringAgainCount: Int?,
    val wakeCheckEnabled: Boolean,
    val wakeCheckAskAfterMinutes: Int,
    val wakeCheckAnswerSeconds: Int,
) {
    companion object {
        fun from(alarm: Alarm) = RingPlan(
            alarmId = alarm.id,
            missions = alarm.missions,
            intervalMinutes = alarm.ringAgain.intervalMinutes,
            ringAgainCount = alarm.ringAgain.count,
            wakeCheckEnabled = alarm.wakeCheck.enabled,
            wakeCheckAskAfterMinutes = alarm.wakeCheck.askAfterMinutes,
            wakeCheckAnswerSeconds = alarm.wakeCheck.answerSeconds,
        )
    }
}

/** 울림 상태(ADR 0002 표). 꺼짐과 놓침은 세션이 끝난 것이라 상태로 두지 않고 [RingEffect.SessionEnded]로 알려요. */
@Serializable
enum class RingPhase { Ringing, Resting, InMission, AwaitingWakeCheck, WakeCheckAsking }

/** 지금 울리고 있는 알람 하나의 진행. 기기 보호 저장소에 저장해서 앱이 죽어도 이어가요. */
@Serializable
data class RingSession(
    val plan: RingPlan,
    val phase: RingPhase,
    /** 남은 '다시 울림' 횟수. null이면 끌 때까지. */
    val ringsLeft: Int?,
    val missionsDone: Int,
    /** 지금 단계가 끝나는 시각(epoch 밀리초). 이 시각의 타이머만 받아들여요. */
    val phaseEndsAt: Long,
)

@Serializable
enum class TimerKind(val phase: RingPhase) {
    RingTimeout(RingPhase.Ringing),
    RestEnded(RingPhase.Resting),
    MissionTimeout(RingPhase.InMission),
    WakeCheckAsk(RingPhase.AwaitingWakeCheck),
    WakeCheckTimeout(RingPhase.WakeCheckAsking),
}

enum class RingOutcome { Dismissed, Missed }

sealed interface RingEvent {
    /** 어떤 알람의 시각이 됐어요. 이미 다른 알람이 울리는 중이면 '지나감'으로 처리해요. */
    data class AlarmFired(val plan: RingPlan) : RingEvent

    data class TimerFired(val kind: TimerKind) : RingEvent

    /** D0 '미션 시작'. 미션 없는 알람이면 '알람 끄기'와 같아요. */
    data object MissionStarted : RingEvent

    /** 미션 화면에서 무엇이든 조작했어요. 59초가 다시 차요. */
    data object MissionInteraction : RingEvent

    data object MissionCompleted : RingEvent

    /** 미션 없는 알람의 '알람 끄기'. */
    data object DismissWithoutMission : RingEvent

    /** W2 '깼어요'. */
    data object WakeCheckConfirmed : RingEvent

    companion object {
        val userEvents: List<RingEvent> =
            listOf(MissionStarted, MissionInteraction, MissionCompleted, DismissWithoutMission, WakeCheckConfirmed)
    }
}

/** 상태표가 바깥에 시키는 일. 울림 서비스가 차례대로 실행해요. */
sealed interface RingEffect {
    /** 앱 볼륨 기준 비율(1 = 그 알람 볼륨 그대로)로 울려요. 이미 울리고 있으면 볼륨만 바꿔요. */
    data class PlaySound(val volumeFraction: Float) : RingEffect

    data object StopSound : RingEffect

    data object ShowRingScreen : RingEffect

    data class ShowMission(val index: Int) : RingEffect

    /** W1 해제 완료 화면. */
    data object ShowWakeUp : RingEffect

    /** W2 깨어 있나요 화면. */
    data object ShowWakeCheck : RingEffect

    /** 타이머는 한 칸뿐이에요. 새로 예약하면 앞의 것을 바꿔요. */
    data class ScheduleTimer(val at: Long, val kind: TimerKind) : RingEffect

    data object CancelTimer : RingEffect

    data class PostMissedNotification(val alarmId: Long) : RingEffect

    /** 이 알람의 다음 시각을 다시 예약해요(한 번 알람이면 꺼요). */
    data class RescheduleAlarm(val alarmId: Long) : RingEffect

    /** 다른 알람이 울리는 중이라 이 알람은 '지나감'으로 기록해요. */
    data class MarkPassed(val alarmId: Long) : RingEffect

    data class SessionEnded(val alarmId: Long, val outcome: RingOutcome) : RingEffect
}

data class Transition(val session: RingSession?, val effects: List<RingEffect>)
