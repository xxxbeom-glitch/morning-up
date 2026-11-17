package com.lumian.morningup.feature.mission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.alarm.ring.RingLink
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.engine.RingEvent
import com.lumian.morningup.core.model.engine.RingPhase
import com.lumian.morningup.core.model.engine.RingSession
import com.lumian.morningup.core.model.engine.RingStateMachine
import com.lumian.morningup.core.model.mission.MathKey
import com.lumian.morningup.core.model.mission.QuickMathGame
import com.lumian.morningup.core.model.mission.QuickMathState
import com.lumian.morningup.core.model.mission.SubmitResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/** 정답, 오답 한 번. [id]가 바뀔 때마다 화면이 움직임과 진동을 한 번 내요. */
data class MathFeedback(val id: Int, val result: SubmitResult)

data class QuickMathUiState(
    /** null이면 아직 미션 중이 아니에요(문제를 그리지 않아요). */
    val game: QuickMathState? = null,
    /** 59초가 끝나는 시각(epoch 밀리초). 조작하면 바로 다시 차요. */
    val timerEndsAt: Long? = null,
    val feedback: MathFeedback? = null,
)

/**
 * M3 Quick Math. 세션의 미션 설정(단계, 연속 정답 개수)으로 한 판을 열고, 조작마다 59초를 다시 채워요.
 * - 미션 중이 아니게 되면(59초가 지나 다시 울림) 판을 버려요. 다시 시작하면 처음부터예요.
 * - 아직 만들지 않은 미션 종류(5단계)는 Quick Math 처음 값으로 대신해요.
 * - 난수는 주입해요(테스트는 seed를 넣어 같은 문제를 받아요).
 */
@HiltViewModel
class QuickMathViewModel @Inject constructor(
    private val link: RingLink,
    private val now: NowProvider,
    @MissionRandom private val random: Random,
) : ViewModel() {
    private val _uiState = MutableStateFlow(QuickMathUiState())
    val uiState: StateFlow<QuickMathUiState> = _uiState.asStateFlow()

    private val signal = InteractionSignal(viewModelScope) { link.send(RingEvent.MissionInteraction) }
    private var game: QuickMathGame? = null

    /** 지금 판이 어느 미션(울림, 몇 번째)의 것인지. */
    private var gameKey: Pair<Long, Int>? = null
    private var feedbackId = 0

    init {
        viewModelScope.launch { link.session.collect(::onSession) }
    }

    private fun onSession(session: RingSession?) {
        if (session == null || session.phase != RingPhase.InMission) {
            if (gameKey != null) Timber.i("mission_reset")
            gameKey = null
            game = null
            signal.cancel()
            _uiState.value = QuickMathUiState()
            return
        }
        val key = session.plan.alarmId to session.missionsDone
        if (key == gameKey) {
            // 엔진이 59초를 다시 채웠어요. 화면이 먼저 채운 값보다 늦을 때만 따라가요.
            _uiState.update { it.copy(timerEndsAt = maxOf(it.timerEndsAt ?: 0L, session.phaseEndsAt)) }
            return
        }
        val setting = session.plan.missionSetting(session.missionsDone)
            ?.takeIf { it.type == MissionType.QuickMath }
            ?: MissionSetting.default(MissionType.QuickMath)
        val newGame = QuickMathGame(setting.level, setting.count, random)
        game = newGame
        gameKey = key
        signal.cancel()
        _uiState.value = QuickMathUiState(game = newGame.start(), timerEndsAt = session.phaseEndsAt)
    }

    fun onKey(key: MathKey) {
        val current = game ?: return
        val state = _uiState.value.game ?: return
        if (state.finished) return
        touch()
        when (key) {
            is MathKey.Digit -> _uiState.update { it.copy(game = current.type(state, key.value)) }
            MathKey.Delete -> _uiState.update { it.copy(game = current.delete(state)) }
            MathKey.Confirm -> submit(current, state)
        }
    }

    private fun submit(current: QuickMathGame, state: QuickMathState) {
        val (next, result) = current.submit(state)
        if (result == SubmitResult.Empty) return
        feedbackId++
        _uiState.update { it.copy(game = next, feedback = MathFeedback(feedbackId, result)) }
        if (result == SubmitResult.Finished) {
            Timber.i("mission_completed type=QuickMath level=%d", next.level)
            signal.cancel()
            link.send(RingEvent.MissionCompleted)
        }
    }

    /** 조작했어요. 화면 타이머는 바로 다시 차고, 엔진에는 묶어서 알려요. */
    private fun touch() {
        _uiState.update { it.copy(timerEndsAt = now.millis() + RingStateMachine.MISSION_MILLIS) }
        signal.poke()
    }

    fun nowMillis(): Long = now.millis()
}
