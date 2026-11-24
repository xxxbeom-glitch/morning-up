package com.lumian.morningup.core.alarm.ring

import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.alarm.ShakeSupport
import com.lumian.morningup.core.data.session.RingSessionStore
import com.lumian.morningup.core.model.InMemoryWakeCheckSettingsStore
import com.lumian.morningup.core.model.WakeCheckSettingsStore
import com.lumian.morningup.core.model.engine.AlarmStore
import com.lumian.morningup.core.model.engine.BootCatchUpPolicy
import com.lumian.morningup.core.model.engine.RingEffect
import com.lumian.morningup.core.model.engine.RingEvent
import com.lumian.morningup.core.model.engine.RingOutcome
import com.lumian.morningup.core.model.engine.RingPhase
import com.lumian.morningup.core.model.engine.RingPlan
import com.lumian.morningup.core.model.engine.RingSession
import com.lumian.morningup.core.model.engine.RingStateMachine
import com.lumian.morningup.core.model.engine.WakeResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 울림 화면이 지금 그릴 것. 상태표의 Show 효과로 바뀌어요. */
sealed interface RingScreen {
    data object None : RingScreen

    /** D0 울림 화면. */
    data object Ringing : RingScreen

    data class Mission(val index: Int) : RingScreen

    /** W1 해제 완료. */
    data object WakeUp : RingScreen

    /** W2 깨어 있나요. */
    data object WakeCheck : RingScreen

    /** W3 정말 깼어요. 기다리는 중에 알림이나 홈 띠로 미리 열 때도 이 화면이에요(결정 145). */
    data object Recheck : RingScreen
}

/** 상태표가 시킨 일을 실제로 하는 쪽. 안드로이드 구현은 [AndroidRingEffectRunner]예요. */
interface RingEffectRunner {
    /** [session]은 효과를 낸 세션이에요(끝난 경우 끝나기 직전 세션). */
    suspend fun run(session: RingSession?, effects: List<RingEffect>)
}

/**
 * 울림의 한 곳. 저장된 상태를 읽고 → 상태표에 넣고 → 저장하고 → 효과를 실행해요.
 * 여러 신호가 한꺼번에 와도 [mutex]로 하나씩 처리해요.
 */
@Singleton
class RingController @Inject constructor(
    private val sessions: RingSessionStore,
    private val alarms: AlarmStore,
    private val runner: RingEffectRunner,
    private val now: NowProvider,
    private val wakeSettings: WakeCheckSettingsStore = InMemoryWakeCheckSettingsStore(),
    private val calls: CallState = CallState.NotInCall,
    /** 흔들기 센서가 없는 폰은 미션 비상구 숨기기를 켜 두어도 알림을 띄워요(결정 151 5절). */
    private val shake: ShakeSupport = ShakeSupport { true },
) {
    private val mutex = Mutex()
    private val _session = MutableStateFlow<RingSession?>(null)
    private val _screen = MutableStateFlow<RingScreen>(RingScreen.None)

    val session: StateFlow<RingSession?> = _session.asStateFlow()
    val screen: StateFlow<RingScreen> = _screen.asStateFlow()

    /**
     * 알람 시각이 됐어요. 알람이 꺼져 있어도 울려요(예약이 남아 있었다면 사용자가 켜 둔 때 잡힌 것이에요).
     * 지워진 알람이면 아무것도 하지 않고 false를 돌려줘요.
     */
    suspend fun fire(alarmId: Long): Boolean {
        val alarm = alarms.get(alarmId) ?: return false
        mutex.withLock { apply(RingEvent.AlarmFired(RingPlan.from(alarm, wakeSettings.current(), shake.available()))) }
        return true
    }

    suspend fun send(event: RingEvent) = mutex.withLock { apply(event) }

    /**
     * 앱이나 서비스가 다시 살아났을 때. 이어갈 울림이 있으면 소리, 화면, 타이머를 되살려요.
     * 단계가 끝난 지 10분이 넘은 울림(폰이 오래 꺼져 있었던 경우)은 이어가지 않고 '놓친 알람'으로 끝내요.
     * 놓친 알람에 쓰는 10분 규칙([BootCatchUpPolicy])과 같아요.
     * 다만 울린 뒤 확인(W2, W3)을 기다리거나 묻던 중이면 알람은 이미 끈 것이라, 놓침 알림 없이 조용히 멈추고
     * 진단 기록만 남겨요(결정 145 '재부팅'). 10분 안이면 바로 물어요.
     */
    suspend fun resume(): RingSession? = mutex.withLock {
        var saved = sessions.load()
        if (saved != null && saved.pausedLeftMillis == null && isStale(saved)) {
            sessions.save(null)
            val ended = staleEffects(saved)
            FlowLog.event("missed_after_long_off", saved.plan.alarmId, FlowLog.effectsOf(ended))
            updateScreen(ended)
            runner.run(saved, ended)
            saved = null
        }
        _session.value = saved
        if (saved != null) {
            val effects = RingStateMachine.resume(saved)
            FlowLog.event("resumed", saved.plan.alarmId, "phase=${saved.phase} " + FlowLog.effectsOf(effects))
            updateScreen(effects)
            runner.run(saved, effects)
            // 통화 때문에 멈춰 둔 W2, W3는 지금 통화 상태로 이어가거나 계속 멈춰요.
            if (saved.pausedLeftMillis != null) apply(RingEvent.CallChanged(calls.inCall()))
        }
        _session.value
    }

    private fun isStale(saved: RingSession) =
        now.millis() - saved.phaseEndsAt > BootCatchUpPolicy.RING_NOW_WINDOW_MILLIS

    private fun staleEffects(saved: RingSession): List<RingEffect> {
        val id = saved.plan.alarmId
        if (saved.phase.isCheck) {
            return listOf(
                RingEffect.StopSound,
                RingEffect.CancelTimer,
                RingEffect.WakeResultRecorded(id, WakeResult.Cancelled, saved.rounds),
                RingEffect.SessionEnded(id, RingOutcome.Cancelled),
            )
        }
        return listOfNotNull(
            RingEffect.StopSound,
            RingEffect.CancelTimer,
            RingEffect.PostMissedNotification(id),
            if (saved.escapeShown) RingEffect.ClearEscape else null,
            RingEffect.SessionEnded(id, RingOutcome.Missed),
        )
    }

    private suspend fun apply(event: RingEvent) {
        val current = sessions.load()
        val transition = RingStateMachine.reduce(current, event, now.millis(), calls.inCall())
        // 미션 화면 조작(59초 다시 채우기)은 너무 잦아서 흐름 로그에 남기지 않아요.
        if (event != RingEvent.MissionInteraction) {
            val after = transition.session
            val firedId = (event as? RingEvent.AlarmFired)?.plan?.alarmId
            FlowLog.event(
                name = FlowLog.nameOf(event),
                alarmId = (after ?: current)?.plan?.alarmId ?: firedId,
                detail = "phase=${current?.phase ?: "-"}->${after?.phase ?: "-"} " +
                    FlowLog.effectsOf(transition.effects),
            )
        }
        if (transition.session != current) sessions.save(transition.session)
        _session.value = transition.session
        updateScreen(transition.effects)
        runner.run(transition.session ?: current, transition.effects)
    }

    private fun updateScreen(effects: List<RingEffect>) {
        effects.forEach { effect ->
            when (effect) {
                RingEffect.ShowRingScreen -> _screen.value = RingScreen.Ringing
                // 쉬기 시작하면 울림 화면을 닫아 폰이 평소처럼 잠들게 해요(결정 70).
                is RingEffect.RestStarted -> _screen.value = RingScreen.None
                is RingEffect.ShowMission -> _screen.value = RingScreen.Mission(effect.index)
                RingEffect.ShowWakeUp -> _screen.value = RingScreen.WakeUp
                RingEffect.ShowWakeCheck -> _screen.value = RingScreen.WakeCheck
                RingEffect.ShowRecheck -> _screen.value = RingScreen.Recheck
                // W2를 통과해 W3를 기다리거나 통화 때문에 멈추면 화면을 닫아요. 끝난 모습은 화면이 잠깐 보여 줘요.
                is RingEffect.RecheckScheduled, RingEffect.PausedForCall -> _screen.value = RingScreen.None
                // W1(해제 완료)은 끝난 뒤에도 사용자가 볼 수 있게 남겨요. 놓친 경우만 화면을 닫아요.
                is RingEffect.SessionEnded -> if (_screen.value != RingScreen.WakeUp) _screen.value = RingScreen.None
                else -> Unit
            }
        }
    }

    /**
     * W1 화면을 닫았어요. W2를 기다리는 중(세션이 남아 있음)이어도 W1은 닫아요.
     * W2 때가 되면 상태표가 ShowWakeCheck로 다시 화면을 정해요.
     */
    fun clearScreen() {
        if (_screen.value == RingScreen.WakeUp) {
            FlowLog.event("wake_up_closed", _session.value?.plan?.alarmId)
            _screen.value = RingScreen.None
        }
    }

    /** 쉬는 중 알림을 눌렀어요. 쉬는 동안에도 울림 화면을 보여 줘서 미션을 시작하거나 끌 수 있게 해요. */
    fun openDuringRest() {
        if (_session.value?.phase == RingPhase.Resting) _screen.value = RingScreen.Ringing
    }

    /**
     * W3를 기다리는 중 알림이나 홈 띠를 눌렀어요. 정한 때보다 먼저 W3를 열어 지금 끝낼 수 있게 해요.
     * 이미 묻는 중이면 그 화면을 그대로 보여 줘요. 열었으면 true예요.
     */
    fun openRecheckEarly(): Boolean {
        val session = _session.value
        val phase = session?.phase
        // 여러 번 묻기에서 이번 기다림에 이미 하나 미리 끝냈으면 열지 않아요(한 번에 하나만, 결정 146).
        val open = session?.canFinishEarly == true || phase == RingPhase.RecheckAsking
        if (open) {
            FlowLog.event("recheck_opened", _session.value?.plan?.alarmId, "phase=$phase")
            _screen.value = RingScreen.Recheck
        }
        return open
    }
}
