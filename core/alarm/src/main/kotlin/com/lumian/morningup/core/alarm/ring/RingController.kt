package com.lumian.morningup.core.alarm.ring

import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.data.session.RingSessionStore
import com.lumian.morningup.core.model.engine.AlarmStore
import com.lumian.morningup.core.model.engine.RingEffect
import com.lumian.morningup.core.model.engine.RingEvent
import com.lumian.morningup.core.model.engine.RingPlan
import com.lumian.morningup.core.model.engine.RingSession
import com.lumian.morningup.core.model.engine.RingStateMachine
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
        mutex.withLock { apply(RingEvent.AlarmFired(RingPlan.from(alarm))) }
        return true
    }

    suspend fun send(event: RingEvent) = mutex.withLock { apply(event) }

    /** 앱이나 서비스가 다시 살아났을 때. 이어갈 울림이 있으면 소리, 화면, 타이머를 되살려요. */
    suspend fun resume(): RingSession? = mutex.withLock {
        val saved = sessions.load()
        _session.value = saved
        if (saved != null) {
            val effects = RingStateMachine.resume(saved)
            updateScreen(effects)
            runner.run(saved, effects)
        }
        saved
    }

    private suspend fun apply(event: RingEvent) {
        val current = sessions.load()
        val transition = RingStateMachine.reduce(current, event, now.millis())
        if (transition.session != current) sessions.save(transition.session)
        _session.value = transition.session
        updateScreen(transition.effects)
        runner.run(transition.session ?: current, transition.effects)
    }

    private fun updateScreen(effects: List<RingEffect>) {
        effects.forEach { effect ->
            when (effect) {
                RingEffect.ShowRingScreen -> _screen.value = RingScreen.Ringing
                is RingEffect.ShowMission -> _screen.value = RingScreen.Mission(effect.index)
                RingEffect.ShowWakeUp -> _screen.value = RingScreen.WakeUp
                RingEffect.ShowWakeCheck -> _screen.value = RingScreen.WakeCheck
                // W1(해제 완료)은 끝난 뒤에도 사용자가 볼 수 있게 남겨요. 놓친 경우만 화면을 닫아요.
                is RingEffect.SessionEnded -> if (_screen.value != RingScreen.WakeUp) _screen.value = RingScreen.None
                else -> Unit
            }
        }
    }

    /** W1 화면을 닫았어요. */
    fun clearScreen() {
        if (_session.value == null) _screen.value = RingScreen.None
    }
}
