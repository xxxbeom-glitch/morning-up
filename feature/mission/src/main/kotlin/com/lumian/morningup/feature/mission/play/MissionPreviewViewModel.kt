package com.lumian.morningup.feature.mission.play

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.alarm.ring.RingLink
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.engine.RingStateMachine
import com.lumian.morningup.feature.mission.MissionRandom
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import timber.log.Timber

/** 미리보기 길. 시트에서 고른 설정 그대로 담아 넘겨요. */
@Serializable
data class MissionPreviewRoute(val settingsJson: String) {
    companion object {
        private val serializer = ListSerializer(MissionSetting.serializer())

        fun of(settings: List<MissionSetting>) = MissionPreviewRoute(Json.encodeToString(serializer, settings))

        fun decode(json: String?): List<MissionSetting> =
            json?.let { runCatching { Json.decodeFromString(serializer, it) }.getOrNull() }.orEmpty()
    }
}

/**
 * '미리 해보기'와 시트 '미리보기'. 울림 엔진을 쓰지 않는 별도 길이에요(소리, 진동 알람 없음).
 * - 59초가 0이 되면 알람 대신 그냥 닫혀 시트로 돌아가요.
 * - 다 끝내면 '잘했어요'를 잠깐 보여 주고 닫혀요.
 * - 미리보기 중 진짜 알람이 울리면 바로 닫아요(흐름 표 11). 진짜 울림 화면은 울림 쪽이 띄워요.
 */
@HiltViewModel
class MissionPreviewViewModel @Inject constructor(
    savedState: SavedStateHandle,
    link: RingLink,
    now: NowProvider,
    @MissionRandom random: Random,
    contentSource: MissionContentSource,
) : MissionHostViewModel(now, random, contentSource) {
    private val settings = MissionPreviewRoute.decode(savedState[SETTINGS_KEY]).take(MissionSetting.MAX_MISSIONS)
    private var completeEndsAt = 0L

    init {
        state.update { it.copy(preview = true) }
        if (settings.isEmpty()) {
            close()
        } else {
            open(settings.first(), 0, settings.size, now.millis() + RingStateMachine.MISSION_MILLIS)
        }
        viewModelScope.launch {
            link.session.collect { session ->
                if (session != null && !state.value.closed) {
                    Timber.i("preview_closed_by_alarm")
                    close()
                }
            }
        }
    }

    override fun onInteracted() = Unit

    override fun onFinished(index: Int) {
        val next = index + 1
        if (next < settings.size) {
            beginTransition(settings[next], next, settings.size)
        } else {
            completeEndsAt = now.millis() + COMPLETE_MILLIS
            state.update { it.copy(completed = true) }
        }
    }

    override fun onTick(nowMillis: Long) {
        val s = state.value
        if (s.closed) return
        if (s.completed) {
            if (nowMillis >= completeEndsAt) close()
            return
        }
        val endsAt = s.timerEndsAt ?: return
        if (s.transitionTo == null && nowMillis >= endsAt) close()
    }

    /** 왼쪽 위 닫기, 뒤로 가기. */
    fun close() {
        state.update { it.copy(closed = true) }
    }

    companion object {
        const val SETTINGS_KEY = "settingsJson"

        /** '잘했어요'를 보여 주는 시간. */
        const val COMPLETE_MILLIS = 1_200L
    }
}
