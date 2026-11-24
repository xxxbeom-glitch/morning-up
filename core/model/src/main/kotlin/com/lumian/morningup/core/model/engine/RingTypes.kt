package com.lumian.morningup.core.model.engine

import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.WakeCheckSettings
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
    /**
     * 미션마다 단계와 개수(4단계에서 더함). 3단계 이전에 저장된 세션에는 없어서 비어 있을 수 있어요.
     * 그럴 때는 [missionSetting]이 처음 값으로 채워요.
     */
    val missionSettings: List<MissionSetting> = emptyList(),
    /** 미션 중 전원 메뉴 닫기(전원 종료 방지). 미션 없는 알람에서는 쓰지 않아요. */
    val powerOffGuard: Boolean = false,
    /** S8 알람 화면 배경 id. null이면 기본 '시간대 하늘'이에요. */
    val background: String? = null,
    /** W2 '깼어요' 뒤 한 번 더 확인(W3)을 할지(결정 145). 울리기 시작할 때의 전체 설정 값이에요. */
    val recheckEnabled: Boolean = false,
    /** W3 '확인할 시간'(분). */
    val recheckAnswerMinutes: Int = WakeCheckSettings.DEFAULT_RECHECK_MINUTES,
) {
    /** [index]번째 미션 설정. 저장된 값이 없거나 종류가 다르면 그 미션의 처음 값이에요. */
    fun missionSetting(index: Int): MissionSetting? {
        val type = missions.getOrNull(index) ?: return null
        return missionSettings.getOrNull(index)?.takeIf { it.type == type }?.normalized()
            ?: MissionSetting.default(type)
    }

    companion object {
        /**
         * 알람과 그때의 전체 설정([settings])으로 이번 울림 값을 정해요. 전체 설정을 바꿔도 이미 시작한 울림은
         * 이 값대로 가고, 다음 알람부터 새 값을 써요(결정 145 7번).
         */
        fun from(alarm: Alarm, settings: WakeCheckSettings = WakeCheckSettings()) = RingPlan(
            alarmId = alarm.id,
            missions = alarm.missionTypes,
            missionSettings = alarm.missions,
            intervalMinutes = alarm.ringAgain.intervalMinutes,
            ringAgainCount = alarm.ringAgain.count,
            wakeCheckEnabled = alarm.wakeCheck.enabled,
            wakeCheckAskAfterMinutes = settings.askAfterMinutes,
            wakeCheckAnswerSeconds = settings.answerSeconds,
            powerOffGuard = alarm.powerOffGuard && alarm.missions.isNotEmpty(),
            background = alarm.background,
            recheckEnabled = settings.recheckEnabled,
            recheckAnswerMinutes = settings.recheckMinutes,
        )
    }
}

/**
 * 울림 상태(ADR 0002 표). 꺼짐과 놓침은 세션이 끝난 것이라 상태로 두지 않고 [RingEffect.SessionEnded]로 알려요.
 * [AwaitingRecheck], [RecheckAsking]은 W3 '정말 깼어요?' 기다림과 묻는 중이에요(결정 145).
 */
@Serializable
enum class RingPhase {
    Ringing,
    Resting,
    InMission,
    AwaitingWakeCheck,
    WakeCheckAsking,
    AwaitingRecheck,
    RecheckAsking,
    ;

    /** 울린 뒤 확인(W2, W3)을 기다리거나 묻는 중. 이때 다른 알람이 울리면 새 알람이 이겨요(결정 145 7번). */
    val isCheck: Boolean get() = isAsking || this == AwaitingWakeCheck || this == AwaitingRecheck

    /** W2나 W3 화면을 보여 주며 묻는 중. */
    val isAsking: Boolean get() = this == WakeCheckAsking || this == RecheckAsking
}

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
    /** W2, W3에 답하지 못해 처음부터 다시 울린 횟수(바퀴). [RingStateMachine.MAX_ROUNDS]까지예요. */
    val rounds: Int = 0,
    /** 통화 중이라 W2, W3를 멈춰 둔 동안 남은 시간(밀리초). null이면 멈추지 않았어요. */
    val pausedLeftMillis: Long? = null,
)

@Serializable
enum class TimerKind(val phase: RingPhase) {
    RingTimeout(RingPhase.Ringing),
    RestEnded(RingPhase.Resting),
    MissionTimeout(RingPhase.InMission),
    WakeCheckAsk(RingPhase.AwaitingWakeCheck),
    WakeCheckTimeout(RingPhase.WakeCheckAsking),
    RecheckAsk(RingPhase.AwaitingRecheck),
    RecheckTimeout(RingPhase.RecheckAsking),
}

/** 세션이 끝난 까닭. [Cancelled]는 남은 확인을 멈춘 경우예요(알람 끔, 지움, 저장, 새 알람, 한 번 더 확인 끔). */
enum class RingOutcome { Dismissed, Missed, Cancelled }

/** 다시 잠들기 방지 결과(결정 145 '연속 기록 세는 법'). 진단 기록에만 남겨요. */
enum class WakeResult {
    /** W2 '깼어요'로 끝남(한 번 더 확인 꺼짐). */
    WakeCheckPassed,

    /** W3를 정한 때에 끝냄. */
    RecheckPassed,

    /** W3를 알림이나 홈 띠로 미리 끝냄. */
    RecheckPassedEarly,

    /** 다시 울린 바퀴가 상한이라 W1에서 끝남. */
    EndedByCap,

    /** 다시 울렸는데 아무도 안 꺼서 놓침. */
    Missed,

    /** 남은 확인을 멈춤. */
    Cancelled,
}

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

    /** W2 '깼어요'(10초 길게 누르기, 결정 145 9-1). */
    data object WakeCheckConfirmed : RingEvent

    /** W3 '정말 깼어요?'에서 10초 길게 누르기를 마쳤어요. W3를 기다리는 중(미리 끝내기)에도 받아요. */
    data object RecheckConfirmed : RingEvent

    /** 통화가 시작되거나 끝났어요. 통화 중에는 W2, W3를 멈추고 시간도 멈춰요. */
    data class CallChanged(val inCall: Boolean) : RingEvent

    /** 이 알람의 남은 확인(W2, W3)을 멈춰요. 알람을 끄거나 지우거나 편집에서 저장했어요. */
    data class CancelChecks(val alarmId: Long) : RingEvent

    /** 전체 설정에서 '한 번 더 확인'을 껐어요. 남은 W3를 멈추고, W2가 남았으면 W2에서 끝나요. */
    data object RecheckTurnedOff : RingEvent

    companion object {
        /**
         * 화면 버튼 사건을 Intent로 주고받을 때 쓰는 고정 이름. 클래스 이름을 쓰면 출시 빌드의 코드 줄이기(R8)가
         * 이름을 바꾸거나 합칠 수 있어서 직접 정해 둬요.
         */
        val userEventKeys: Map<String, RingEvent> = mapOf(
            "mission_started" to MissionStarted,
            "mission_interaction" to MissionInteraction,
            "mission_completed" to MissionCompleted,
            "dismiss_without_mission" to DismissWithoutMission,
            "wake_check_confirmed" to WakeCheckConfirmed,
            "recheck_confirmed" to RecheckConfirmed,
        )

        val userEvents: List<RingEvent> = userEventKeys.values.toList()

        fun keyOf(event: RingEvent): String? = userEventKeys.entries.firstOrNull { it.value == event }?.key
    }
}

/** 상태표가 바깥에 시키는 일. 울림 서비스가 차례대로 실행해요. */
sealed interface RingEffect {
    /** 앱 볼륨 기준 비율(1 = 그 알람 볼륨 그대로)로 울려요. 이미 울리고 있으면 볼륨만 바꿔요. */
    data class PlaySound(val volumeFraction: Float) : RingEffect

    data object StopSound : RingEffect

    data object ShowRingScreen : RingEffect

    /**
     * 한 번 울림(2분)이 끝나 쉬기 시작했어요. 울림 화면을 닫고, [nextRingAt]에 다시 울린다고 짧게 알려요(11-17 사용자 지시).
     * [nextRingNumber]는 다음이 몇 번째 울림인지예요. 끌 때까지 울리는 알람은 셀 수 없어 null이에요.
     */
    data class RestStarted(val nextRingAt: Long, val intervalMinutes: Int, val nextRingNumber: Int?) : RingEffect

    data class ShowMission(val index: Int) : RingEffect

    /** W1 해제 완료 화면. */
    data object ShowWakeUp : RingEffect

    /** W2 깨어 있나요 화면. */
    data object ShowWakeCheck : RingEffect

    /** W3 정말 깼어요 화면. */
    data object ShowRecheck : RingEffect

    /** W2, W3가 뜰 때 짧은 알림음(알람 볼륨의 30%)과 진동 2번(결정 145 3번). */
    data object PlayChime : RingEffect

    /** 통화 중이라 W2, W3를 멈췄어요. 화면을 닫고 통화가 끝나면 이어서 물어요. */
    data object PausedForCall : RingEffect

    /** W2를 통과해 [askAt]에 W3를 물어요. 화면을 닫고 안내 알림('오전 6:43에 한 번 더 물어볼게요')을 띄워요. */
    data class RecheckScheduled(val askAt: Long) : RingEffect

    /** 다시 잠들기 방지 결과를 진단 기록에 남겨요. [rounds]는 다시 울린 바퀴 수예요. */
    data class WakeResultRecorded(val alarmId: Long, val result: WakeResult, val rounds: Int) : RingEffect

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
