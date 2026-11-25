package com.lumian.morningup.core.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * 다시 잠들기 방지 전체 설정(결정 145). 알람마다는 켜고 끄기([WakeCheck])만 두고, 값은 앱 전체에 한 번만 둬요.
 * - [askAfterMinutes]: '다시 물어보기'. 알람을 끈 뒤 W2까지, W2 '깼어요' 뒤 W3까지 같은 간격이에요.
 * - [answerSeconds]: W2 '대답할 시간'.
 * - [recheckEnabled]: '한 번 더 확인'(W3 '정말 깼어요?'). 처음 켜짐.
 * - [recheckMinutes]: W3 '확인할 시간'. 처음 10분.
 * - [lab]: '확실히 잠깨기' 실험실(결정 146). 여러 번 묻기를 켜면 [recheckEnabled]와 관계없이 W3를 물어요.
 */
data class WakeCheckSettings(
    val askAfterMinutes: Int = DEFAULT_ASK_MINUTES,
    val answerSeconds: Int = DEFAULT_ANSWER_SECONDS,
    val recheckEnabled: Boolean = true,
    val recheckMinutes: Int = DEFAULT_RECHECK_MINUTES,
    val lab: LabSettings = LabSettings(),
) {
    /** 실제로 W3를 묻는지. 실험실 여러 번 묻기를 켜면 '한 번 더 확인'은 켜진 채 잠겨요. */
    val recheckOn: Boolean get() = recheckEnabled || lab.multiAskEnabled

    companion object {
        const val DEFAULT_ASK_MINUTES = 3
        const val DEFAULT_ANSWER_SECONDS = 60
        const val DEFAULT_RECHECK_MINUTES = 10

        /** T1-1 '다시 물어보기' 3분 후, 5분 후, 10분 후. */
        val ASK_MINUTES = listOf(3, 5, 10)

        /** T1-1 '대답할 시간' 30초, 1분, 3분. */
        val ANSWER_SECONDS = listOf(30, 60, 180)

        /** T1-2 '확인할 시간' 3분, 5분, 10분. */
        val RECHECK_MINUTES = listOf(3, 5, 10)
    }
}

/** 전체 설정 저장소. 구현은 `:core:data`의 기기 보호 저장소예요(재부팅 뒤 잠금을 풀기 전에도 읽혀야 해서). */
interface WakeCheckSettingsStore {
    val settings: StateFlow<WakeCheckSettings>

    /** 예전 알람마다 값을 옮기는 일이 끝난 뒤의 값. 울리기 시작할 때 이걸 읽어요. */
    suspend fun current(): WakeCheckSettings

    fun update(transform: (WakeCheckSettings) -> WakeCheckSettings)
}

/**
 * 울린 뒤 남은 확인(W2, W3)을 화면(feature)에서 다루는 창구. 구현은 `:core:alarm`이에요.
 * 결정 145 '남은 확인 취소' 표대로 알람을 끄거나 지우거나 저장하면 남은 확인을 멈춰요.
 */
interface WakeCheckGate {
    /** 한 번 더 확인(W3)을 기다리는 중이고 지금 미리 끝낼 수 있는지. 홈 위쪽 띠가 봐요. */
    val recheckPending: Flow<Boolean>

    /** 실험실 여러 번 묻기의 남은 확인 수. 여러 번 묻기가 아니면 null이에요(띠 글이 '한 번 더 확인이 2번 남았어요'). */
    val recheckLeft: Flow<Int?> get() = flowOf(null)

    /**
     * [alarmId]가 지금 울리는 중이거나 울린 뒤 남은 확인(W2, W3)을 기다리는 중인지.
     * 이때 끈 알람의 '이번만 쉬기'는 오늘 것을 쉰 걸로 쳐요(결정 154).
     */
    fun isActive(alarmId: Long): Boolean = false

    /**
     * [alarmId]가 지금 울리는 중, 쉬는 중(다시 울림 사이), 미션 중인지. 이때는 홈과 편집에서 그 알람을 끄거나 쉬거나
     * 지우거나 저장하지 못하게 막아요(결정 165). W2, W3 대기는 들지 않아요(결정 153대로 끄면 확인을 멈춰요).
     */
    fun isRinging(alarmId: Long): Boolean = false

    /** 쉬는 중이면 다시 울릴 시각(epoch 밀리초), 아니면 null. 홈 띠 '오전 6:40에 다시 울려요'가 봐요(결정 165). */
    val restingUntil: Flow<Long?> get() = flowOf(null)

    /** 미션 끝 뒤 W2를 기다리는 중인지. 홈 띠 '곧 깼는지 물어볼게요'가 봐요(결정 168). */
    val wakeCheckWaiting: Flow<Boolean> get() = flowOf(false)

    /** 쉬는 중 홈 띠를 눌렀어요. 쉬는 중 알림을 누른 것과 같이 울림 화면을 열어요(결정 70, 165). */
    fun openRest() = Unit

    /** [alarmId]의 남은 확인을 모두 멈춰요. 멈춘 것이 있으면 true예요. */
    suspend fun cancelFor(alarmId: Long): Boolean

    /** 전체 설정에서 '한 번 더 확인'을 껐어요. 남은 W3만 멈춰요. */
    suspend fun recheckTurnedOff()

    /** W3 '정말 깼어요?'를 지금 열어요(홈 띠, 미리 끝내기). */
    fun openRecheck()
}

/** 메모리에만 두는 설정 저장소. 시험과 기본값에 써요. */
class InMemoryWakeCheckSettingsStore(initial: WakeCheckSettings = WakeCheckSettings()) : WakeCheckSettingsStore {
    private val state = MutableStateFlow(initial)
    override val settings: StateFlow<WakeCheckSettings> = state

    override suspend fun current(): WakeCheckSettings = state.value

    override fun update(transform: (WakeCheckSettings) -> WakeCheckSettings) {
        state.value = transform(state.value)
    }
}
