package com.lumian.morningup.feature.mission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.alarm.ring.MissionEscapeAnswers
import com.lumian.morningup.core.alarm.ring.MissionEscapeNotifier
import com.lumian.morningup.core.alarm.ring.RingLink
import com.lumian.morningup.core.model.engine.MissionEscape
import com.lumian.morningup.core.model.engine.RingEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EscapeResult { Correct }

/**
 * M5 미션 포기가 그릴 것. [lastTypedAt]부터 1분이 지나면 미션으로 돌아가요.
 * [rejected]는 틀린 글자 뒤로 이어 쓰려다 막힌 횟수예요. 바뀔 때마다 칸이 살짝 흔들려요.
 */
data class EscapeUiState(
    val sentence: String,
    val text: String = "",
    val result: EscapeResult? = null,
    val lastTypedAt: Long = 0L,
    val rejected: Int = 0,
)

/**
 * M5 미션 포기(결정 171, TSK-231 따라 쓰기). 쓰는 동안 미션 시간이 끝나 다시 울리지 않게 '미션 조작'을 보내요.
 * 틀린 글자가 있으면 지울 때까지 이어 쓰지 못해요([TraceTyping.accepts]). 끝까지 맞게 쓰면 확인 버튼 없이
 * 초록 칸을 잠깐 보여 준 뒤 [MissionEscapeAnswers]로 보내요(미션을 다 끝낸 것과 같아요, 결정 150).
 * 쓴 글은 어디에도 남기지 않아요.
 */
@HiltViewModel
class EscapeViewModel @Inject constructor(
    private val link: RingLink,
    notifier: MissionEscapeNotifier,
    private val answers: MissionEscapeAnswers,
    private val now: NowProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(
        EscapeUiState(
            sentence = notifier.currentSentence ?: MissionEscape.TEST_SENTENCE,
            lastTypedAt = now.millis(),
        ),
    )
    val state: StateFlow<EscapeUiState> = _state.asStateFlow()

    private var lastInteractionSent = Long.MIN_VALUE

    fun nowMillis(): Long = now.millis()

    /** 글자를 쳤어요. 1분이 처음부터 다시 시작해요. 틀린 글자 뒤로 이어 쓰면 막고, 끝까지 맞으면 바로 끝나요. */
    fun onType(text: String) {
        val current = _state.value
        if (current.result == EscapeResult.Correct) return
        val at = now.millis()
        if (!TraceTyping.accepts(current.text, text, current.sentence)) {
            _state.update { it.copy(rejected = it.rejected + 1, lastTypedAt = at) }
            keepMissionAlive(at)
            return
        }
        if (MissionEscape.matches(text, current.sentence)) {
            _state.update { it.copy(text = text, result = EscapeResult.Correct, lastTypedAt = at) }
            viewModelScope.launch {
                delay(CORRECT_HOLD_MILLIS)
                answers.answer(text)
            }
            return
        }
        _state.update { it.copy(text = text, result = null, lastTypedAt = at) }
        keepMissionAlive(at)
    }

    /** '미션으로 돌아가기'나 뒤로 가기. */
    fun onBack() = link.backToMission()

    /** 1분 동안 아무것도 안 썼어요. 하던 미션으로 돌아가요. */
    fun onIdle() = link.backToMission()

    private fun keepMissionAlive(at: Long) {
        if (at - lastInteractionSent < INTERACTION_GAP_MILLIS) return
        lastInteractionSent = at
        // '미션 조작'(59초)이 아니라 M5 열기와 같게 1분 + 미션 시간을 다시 걸어요(ISS-051).
        link.send(RingEvent.EscapeTyped)
    }

    companion object {
        /** 다 맞은 초록 칸을 보여 주는 시간. */
        const val CORRECT_HOLD_MILLIS = 1_200L

        /** 글자마다 서비스를 부르지 않게 '미션 조작'은 10초에 한 번만 보내요(미션 시간 2분보다 충분히 짧아요). */
        const val INTERACTION_GAP_MILLIS = 10_000L
    }
}
