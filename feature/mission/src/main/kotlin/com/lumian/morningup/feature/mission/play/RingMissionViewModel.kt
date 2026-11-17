package com.lumian.morningup.feature.mission.play

import androidx.lifecycle.viewModelScope
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.alarm.ring.RingLink
import com.lumian.morningup.core.model.engine.RingEvent
import com.lumian.morningup.core.model.engine.RingPhase
import com.lumian.morningup.core.model.engine.RingSession
import com.lumian.morningup.feature.mission.InteractionSignal
import com.lumian.morningup.feature.mission.MissionRandom
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 진짜 울림의 미션 화면. 울림 세션의 '몇 번째 미션'을 따라가요.
 * - 미션을 끝내면 바로 엔진에 알려요. 엔진이 다음 미션으로 넘기면 0.8초 '다음 미션 n / total' 뒤 열어요.
 * - 미션 중이 아니게 되면(59초가 지나 다시 울림) 판을 버려요. 다시 시작하면 끝낸 미션은 건너뛰고 하던 미션은 처음부터예요.
 */
@HiltViewModel
class RingMissionViewModel @Inject constructor(
    private val link: RingLink,
    now: NowProvider,
    @MissionRandom random: Random,
    contentSource: MissionContentSource,
) : MissionHostViewModel(now, random, contentSource) {
    private val signal = InteractionSignal(viewModelScope) { link.send(RingEvent.MissionInteraction) }

    /** 지금 판이 어느 미션(울림, 몇 번째)의 것인지. */
    private var key: Pair<Long, Int>? = null

    init {
        viewModelScope.launch { link.session.collect(::onSession) }
    }

    private fun onSession(session: RingSession?) {
        val setting = session?.takeIf {
            it.phase == RingPhase.InMission
        }?.let { it.plan.missionSetting(it.missionsDone) }
        if (session == null || setting == null) {
            if (key != null) Timber.i("mission_reset")
            key = null
            signal.cancel()
            clear()
            return
        }
        val newKey = session.plan.alarmId to session.missionsDone
        val total = session.plan.missions.size
        val previous = key
        when {
            newKey == previous ->
                // 엔진이 59초를 다시 채웠어요. 화면이 먼저 채운 값보다 늦을 때만 따라가요.
                state.update { it.copy(timerEndsAt = maxOf(it.timerEndsAt ?: 0L, session.phaseEndsAt)) }
            previous != null && previous.first == newKey.first && previous.second + 1 == newKey.second -> {
                key = newKey
                signal.cancel()
                beginTransition(setting, session.missionsDone, total)
                state.update { it.copy(timerEndsAt = session.phaseEndsAt) }
            }
            else -> {
                key = newKey
                signal.cancel()
                open(setting, session.missionsDone, total, session.phaseEndsAt)
            }
        }
    }

    override fun onInteracted() = signal.poke()

    override fun onFinished(index: Int) {
        Timber.i("mission_completed type=%s index=%d", state.value.play?.type, index)
        signal.cancel()
        link.send(RingEvent.MissionCompleted)
    }
}
