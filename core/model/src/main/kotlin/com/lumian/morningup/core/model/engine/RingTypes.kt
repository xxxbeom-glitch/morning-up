package com.lumian.morningup.core.model.engine

import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.ButtonTrick
import com.lumian.morningup.core.model.LabSettings
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.Snooze
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
    /** W2 '깼어요' 뒤 한 번 더 확인(W3)을 할지(결정 145). 울리기 시작할 때의 알람 값이에요(결정 180). */
    val recheckEnabled: Boolean = false,
    /** W3 대답할 시간(분). 1분 고정이에요(결정 176). */
    val recheckAnswerMinutes: Int = WakeCheckSettings.RECHECK_ANSWER_MINUTES,
    /** W2 '깼어요' 뒤 W3까지, W3와 다음 W3 사이 기다리는 시간(분, 결정 176). */
    val recheckWaitMinutes: Int = WakeCheckSettings.DEFAULT_RECHECK_WAIT_MINUTES,
    /** W3를 몇 번 물을지(결정 176). 1이면 결정 145대로 한 번만 물어요. */
    val recheckCount: Int = WakeCheckSettings.DEFAULT_RECHECK_COUNT,
    /** W3 기다리는 시간과 대답할 시간에 쓰는 1분의 길이. 디버그 시험 배율이 아니면 60초예요. */
    val minuteMillis: Long = LabSettings.MINUTE_MILLIS,
    /** 실험실 버튼 속이기. null이면 꺼짐이에요. 울림 화면(D0)이 이 값을 봐요. */
    val trick: ButtonTrick? = null,
    /**
     * 실험실에서 '미션을 못 할 때 알림 숨기기'를 켰는지(결정 151). 움직임 센서가 없는 폰에서 흔들기 미션이 있으면
     * 켜 두었어도 false예요(5절 예외). 마지막 바퀴 예외는 상태표가 봐요([MissionEscape.shouldShow]).
     */
    val escapeHidden: Boolean = false,
    /** 미루기 간격(분, 결정 195). */
    val snoozeMinutes: Int = Snooze.DEFAULT_MINUTES,
    /** 한 번 울림 동안 미룰 수 있는 횟수. 미루기를 꺼 두었으면 0이에요. 옛 세션에도 0이라 버튼이 안 보여요. */
    val snoozeCount: Int = 0,
) {
    /** [index]번째 미션 설정. 저장된 값이 없거나 종류가 다르면 그 미션의 처음 값이에요. */
    fun missionSetting(index: Int): MissionSetting? {
        val type = missions.getOrNull(index) ?: return null
        return missionSettings.getOrNull(index)?.takeIf { it.type == type }?.normalized()
            ?: MissionSetting.default(type)
    }

    companion object {
        /**
         * 알람과 그때의 실험실 설정([settings])으로 이번 울림 값을 정해요. W2, W3 값은 알람 것이에요(결정 180).
         * 값을 바꿔도 이미 시작한 울림은 이 값대로 가고, 다음 알람부터 새 값을 써요(결정 145 7번).
         * [shakeSupported]는 흔들기를 알아챌 센서가 있는지예요.
         */
        fun from(
            alarm: Alarm,
            settings: WakeCheckSettings = WakeCheckSettings(),
            shakeSupported: Boolean = true,
        ) = RingPlan(
            alarmId = alarm.id,
            missions = alarm.missionTypes,
            missionSettings = alarm.missions,
            intervalMinutes = alarm.ringAgain.intervalMinutes,
            ringAgainCount = alarm.ringAgain.count,
            wakeCheckEnabled = alarm.wakeCheck.enabled,
            wakeCheckAskAfterMinutes = alarm.wakeCheck.normalized().askAfterMinutes,
            wakeCheckAnswerSeconds = alarm.wakeCheck.normalized().answerSeconds,
            powerOffGuard = alarm.powerOffGuard && alarm.missions.isNotEmpty(),
            background = alarm.background,
            // 결정 184: W3는 실험실 '한 번 더 확인'이 켜져 있을 때만 알람 값대로 물어요.
            recheckEnabled = alarm.wakeCheck.normalized().recheckEnabled && settings.lab.recheckEnabled,
            recheckWaitMinutes = alarm.wakeCheck.normalized().recheckWaitMinutes,
            recheckCount = alarm.wakeCheck.normalized().recheckCount,
            minuteMillis = settings.lab.debugMinuteMillis,
            trick = settings.lab.trick,
            escapeHidden = settings.lab.escapeHidden &&
                (shakeSupported || MissionType.ShakeShake !in alarm.missionTypes),
            snoozeMinutes = alarm.snooze.normalized().minutes,
            snoozeCount = if (alarm.snooze.enabled) alarm.snooze.normalized().count else 0,
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
    /** 이 바퀴에서 마친 W3 수. 못 해서 처음부터 다시 울리면 0부터 다시 세요(결정 136, 176). */
    val checksDone: Int = 0,
    /**
     * W3를 여러 번 물을 때, 지금 기다리는 W3를 알림이나 홈 띠로 이미 하나 미리 끝냈는지. 그러면 다음 W3가 실제로
     * 물을 때까지는 또 미리 끝낼 수 없어요(한 번에 하나만).
     */
    val earlyUsed: Boolean = false,
    /**
     * 이 바퀴에서 미션 비상구 알림(결정 149)을 띄웠는지. 첫 미션을 시작할 때 한 번 띄우고, 미션을 끝내거나 써서 끄거나
     * 울림이 끝나면 지워요. 다시 잠들기 방지로 처음부터 다시 울리면 그 바퀴의 첫 미션 때 다시 띄워요.
     */
    val escapeShown: Boolean = false,
    /** 남은 미루기 횟수(결정 195). 자동 '다시 울림' 횟수([ringsLeft])와 따로 세요. */
    val snoozesLeft: Int = 0,
    /** 지금 쉬는 중([RingPhase.Resting])이 미루기로 시작됐는지. 알림과 홈 띠 글이 달라요. */
    val snoozed: Boolean = false,
    /** 알람 시각에 울리기 시작한 때(epoch 밀리초). 같은 분에 온 알람끼리 고를 때 써요(결정 200 1번). 옛 세션은 0이에요. */
    val firedAt: Long = 0L,
    /**
     * 이 알람이 끼어들어 잠시 멈춘 앞 알람(결정 200 3, 4번). 쉬는 중이거나 확인(W2, W3)을 기다리던 알람이에요.
     * 이 알람이 끝나면 이어가요. 앞 알람도 멈춘 알람을 들고 있을 수 있어요.
     */
    val suspended: RingSession? = null,
) {
    /** 울림 화면(D0)에 'n분 뒤 다시'를 보여 줄지. 소리 내며 울리는 중이고 횟수가 남았을 때만이에요. */
    val canSnooze: Boolean get() = phase == RingPhase.Ringing && snoozesLeft > 0

    /** W3를 여러 번 물을 때 남은 수. 한 번만 물으면 null. */
    val checksLeft: Int?
        get() = plan.recheckCount.takeIf { it > 1 }?.let { (it - checksDone).coerceAtLeast(0) }

    /** W3를 지금 미리 열어 끝낼 수 있는지(기다리는 중이고, 이번 기다림에 아직 안 썼을 때). */
    val canFinishEarly: Boolean get() = phase == RingPhase.AwaitingRecheck && !earlyUsed
}

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

    /** D0 'n분 뒤 다시'(미루기, 결정 195). 울리는 중이고 횟수가 남았을 때만 받아요. */
    data object Snoozed : RingEvent

    /** 미션 없는 알람의 '알람 끄기'. */
    data object DismissWithoutMission : RingEvent

    /**
     * 미션 비상구 알림에 예문을 맞게 따라 썼어요(결정 149). 미션을 다 끝낸 것과 같게 처리해요. 알림이 떠 있는 동안
     * (울림, 쉬는 중, 미션 중)만 받아요. 틀리게 보낸 것은 [MissionInteraction]으로 보내 59초를 다시 채워요.
     */
    data object MissionEscaped : RingEvent

    /**
     * 미션 비상구 알림을 눌러 M5 '미션 포기' 화면을 열었어요(결정 171). 화면에 있는 동안 소리가 꺼져야 해서
     * 울림, 쉬는 중이면 미션 중으로 넘기고(소리 끔, 미션 시간), 미션 중이면 미션 시간을 다시 채워요.
     * 화면은 컨트롤러가 이미 M5로 정해서 미션 화면을 띄우는 효과는 내지 않아요.
     */
    data object EscapeOpened : RingEvent

    /** M5에서 글을 쓰거나 틀리게 보냈어요. [EscapeOpened]와 같게 M5 시간(1분 + 미션 시간)을 다시 걸어요(ISS-051). */
    data object EscapeTyped : RingEvent

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
            "mission_escaped" to MissionEscaped,
            "escape_opened" to EscapeOpened,
            "escape_typed" to EscapeTyped,
            "wake_check_confirmed" to WakeCheckConfirmed,
            "recheck_confirmed" to RecheckConfirmed,
            "snoozed" to Snoozed,
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

    /**
     * 미루기를 눌러 쉬기 시작했어요(결정 195). 울림 화면을 닫고 [nextRingAt]에 다시 울려요. [left]는 남은 미루기 횟수예요.
     * 알림은 쉬는 중 알림과 같은 자리에 '알람을 미뤘어요'로 떠요.
     */
    data class SnoozeStarted(val nextRingAt: Long, val minutes: Int, val left: Int) : RingEffect

    data class ShowMission(val index: Int) : RingEffect

    /**
     * 미션 비상구 알림을 띄워요(결정 149). [fresh]면 새 예문을 고르고(바로 전 것 빼고), 아니면 지금 예문 그대로
     * 다시 올려요(앱이 다시 살아났을 때).
     */
    data class ShowEscape(val fresh: Boolean = true) : RingEffect

    /** 미션 비상구 알림을 지워요. 미션을 다 끝냈거나 울림이 끝났어요. */
    data object ClearEscape : RingEffect

    /** 미션 비상구로 껐어요. 진단 기록에 '이번 달 n번째' 한 줄을 남겨요(7절). */
    data class EscapeUsed(val alarmId: Long) : RingEffect

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

    /**
     * W2를 통과해 [askAt]에 W3를 물어요. 화면을 닫고 안내 알림('오전 6:43에 한 번 더 물어볼게요')을 띄워요.
     * W3를 여러 번 물으면 [number]번째/[total]이에요(결정 176).
     */
    data class RecheckScheduled(
        val askAt: Long,
        val number: Int? = null,
        val total: Int? = null,
    ) : RingEffect

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
