package com.lumian.morningup.feature.mission.play

import androidx.lifecycle.ViewModel
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.engine.RingStateMachine
import com.lumian.morningup.core.model.mission.MathKey
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 미션 화면 상태(울림과 미리보기 공통). */
data class MissionUiState(
    /** null이면 그릴 미션이 없어요. */
    val play: MissionPlay? = null,
    /** 지금 미션 순서(0부터)와 전체 개수. */
    val index: Int = 0,
    val total: Int = 0,
    /** 59초가 끝나는 시각(epoch 밀리초). 조작하면 바로 다시 차요. */
    val timerEndsAt: Long? = null,
    /** 미션 사이 넘어가기 중이면 다음 미션 번호(1부터). '다음 미션 n / total'을 보여 줘요. */
    val transitionTo: Int? = null,
    /** 미리보기(닫기 버튼, '잘했어요')인지. */
    val preview: Boolean = false,
    /** 미리보기를 다 끝냈어요('잘했어요'). */
    val completed: Boolean = false,
    /** 미리보기를 닫을 때예요. */
    val closed: Boolean = false,
)

/**
 * 미션 화면 공통 뼈대. 조작을 [MissionRunner]에 넘기고, 59초 타이머와 미션 사이 넘어가기(0.8초)를 관리해요.
 * 시간 흐름은 화면이 매 장면마다 [onFrame]을 불러 알려 줘요(뷰모델이 따로 타이머를 돌리지 않아요).
 */
abstract class MissionHostViewModel(
    protected val now: NowProvider,
    private val random: Random,
    private val contentSource: MissionContentSource,
) : ViewModel() {
    protected val state = MutableStateFlow(MissionUiState())
    val uiState: StateFlow<MissionUiState> = state.asStateFlow()

    private var runner: MissionRunner? = null
    private var pending: Pair<MissionSetting, Int>? = null
    private var transitionEndsAt = 0L

    /** [index]번째 미션을 바로 열어요. */
    protected fun open(setting: MissionSetting, index: Int, total: Int, timerEndsAt: Long) {
        val next = MissionRunner(setting, contentSource.load(), random, now.millis())
        runner = next
        pending = null
        state.update {
            it.copy(play = next.play, index = index, total = total, timerEndsAt = timerEndsAt, transitionTo = null)
        }
    }

    /** 끝낸 미션 화면을 물리고 '다음 미션 n / total'을 0.8초 보여 준 뒤 [index]번째 미션을 열어요. */
    protected fun beginTransition(setting: MissionSetting, index: Int, total: Int) {
        pending = setting to index
        transitionEndsAt = now.millis() + TRANSITION_MILLIS
        state.update { it.copy(transitionTo = index + 1, total = total) }
    }

    /** 미션을 버려요. 다시 열면 처음부터예요. */
    protected fun clear() {
        runner = null
        pending = null
        state.update { MissionUiState(preview = it.preview, closed = it.closed) }
    }

    fun onMathKey(key: MathKey) = act { it.mathKey(key, now.millis()) }

    fun onQuizAnswer(index: Int) = act { it.quizAnswer(index, now.millis()) }

    fun onDropKey(syllable: String) = act { it.dropKey(syllable, now.millis()) }

    fun onDropDelete() = act { it.dropDelete() }

    fun onDropSubmit() = act { it.dropSubmit(now.millis()) }

    fun onShake() = act { it.shake() }

    private fun act(block: (MissionRunner) -> Outcome) {
        val current = runner ?: return
        if (state.value.transitionTo != null || state.value.completed) return
        val outcome = block(current)
        state.update { it.copy(play = current.play) }
        when (outcome) {
            Outcome.Interacted -> touch()
            Outcome.Finished -> onFinished(state.value.index)
            Outcome.Ignored -> Unit
        }
    }

    /** 화면이 매 장면 불러요. 넘어가기를 끝내고, 떨어지는 단어를 움직여요. */
    fun onFrame() {
        val t = now.millis()
        val waiting = pending
        if (waiting != null && t >= transitionEndsAt) {
            open(waiting.first, waiting.second, state.value.total, t + RingStateMachine.MISSION_MILLIS)
            // 59초는 새로 시작해요.
            onInteracted()
        }
        val current = runner
        if (current != null && state.value.transitionTo == null) {
            current.frame(t)
            if (current.play != state.value.play) state.update { it.copy(play = current.play) }
        }
        onTick(t)
    }

    fun nowMillis(): Long = now.millis()

    /** 조작했어요. 화면 타이머는 바로 다시 차요. */
    private fun touch() {
        state.update { it.copy(timerEndsAt = now.millis() + RingStateMachine.MISSION_MILLIS) }
        onInteracted()
    }

    /** 조작이 있었어요(울림은 엔진에 알리고, 미리보기는 할 일 없음). */
    protected abstract fun onInteracted()

    /** [index]번째 미션을 끝냈어요. */
    protected abstract fun onFinished(index: Int)

    /** 매 장면 시각. */
    protected open fun onTick(nowMillis: Long) = Unit

    companion object {
        /** '다음 미션 n / total'을 보여 주는 시간. */
        const val TRANSITION_MILLIS = 800L
    }
}
