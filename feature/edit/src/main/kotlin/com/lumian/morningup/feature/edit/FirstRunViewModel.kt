package com.lumian.morningup.feature.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumian.morningup.core.alarm.ShakeSupport
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.AlarmBook
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 첫 알람 만들기 4단계(결정 191, O4-1~O4-4). 진행 막대는 이 4칸이에요. */
enum class FirstRunStep {
    Time,
    Background,
    Sound,
    Mission,
    ;

    /** 다음 단계. 마지막(미션)이면 null이고, 그때 '완료'가 알람을 저장해요. */
    val next: FirstRunStep? get() = entries.getOrNull(ordinal + 1)

    /** 이전 단계. 첫 단계(알람 시간)면 null이고, 그때 뒤로 가기는 앱을 나가요. */
    val previous: FirstRunStep? get() = entries.getOrNull(ordinal - 1)
}

/** 첫 실행이 어떻게 끝났는지. [Saved]면 홈에서 '알람을 저장했어요' 토스트를 띄워요. */
sealed interface FirstRunExit {
    data class Saved(val alarmId: Long) : FirstRunExit

    /** 건너뛰기. 알람을 만들지 않고 홈은 빈 상태예요. */
    data object Skipped : FirstRunExit
}

data class FirstRunUiState(
    val step: FirstRunStep = FirstRunStep.Time,
    val draft: Alarm = FirstRunViewModel.firstAlarm(),
    val shakeAvailable: Boolean = true,
    val saving: Boolean = false,
    val exit: FirstRunExit? = null,
) {
    /** 고른 미션. null이면 미션 없이(처음 값, 11-16 결정). */
    val mission: MissionType? get() = draft.missionTypes.firstOrNull()
}

/**
 * 첫 알람 만들기(O4-1~O4-4) 상태. 단계마다 고른 값을 한 알람 초안에 모았다가 O4-4 '완료'에서 한 번 저장해요.
 * 중간에 앱을 닫으면 저장한 것이 없어서 다음 실행에 O4-1부터 다시 해요(기획 1절).
 */
@HiltViewModel
class FirstRunViewModel @Inject constructor(
    private val book: AlarmBook,
    shakeSupport: ShakeSupport,
) : ViewModel() {
    private val state = MutableStateFlow(FirstRunUiState(shakeAvailable = shakeSupport.available()))
    val uiState: StateFlow<FirstRunUiState> = state.asStateFlow()

    private fun edit(change: (Alarm) -> Alarm) = state.update { it.copy(draft = change(it.draft)) }

    fun setTime(hour: Int, minute: Int) = edit { it.copy(time = LocalTime.of(hour, minute)) }

    /** null은 시간대 하늘(새 알람 처음 값). */
    fun setBackground(id: String?) = edit { it.copy(background = id) }

    fun setSound(id: String) = edit { it.copy(soundId = id) }

    /** 미션은 하나만 골라요. null은 미션 없이. 값은 모두 처음 값(결정 187)이고 설정 시트는 열지 않아요. */
    fun setMission(type: MissionType?) {
        if (type == MissionType.ShakeShake && !state.value.shakeAvailable) return
        edit { it.copy(missions = listOfNotNull(type?.let(MissionSetting::default))) }
    }

    /** 다음 단계로. 마지막 단계면 알람을 저장해요. [save]가 false(확인용 앱)면 저장하지 않고 끝내요. */
    fun next(save: Boolean = true) {
        val current = state.value
        if (current.saving || current.exit != null) return
        val following = current.step.next
        when {
            following != null -> state.update { it.copy(step = following) }
            save -> save()
            else -> state.update { it.copy(exit = FirstRunExit.Skipped) }
        }
    }

    /** 이전 단계로. 첫 단계면 false(부르는 쪽이 앱을 나가게 둬요). */
    fun back(): Boolean {
        val previous = state.value.step.previous ?: return false
        state.update { it.copy(step = previous) }
        return true
    }

    fun skip() = state.update { if (it.exit == null && !it.saving) it.copy(exit = FirstRunExit.Skipped) else it }

    /** 확인용 앱에서 한 바퀴를 마치고 처음부터 다시 볼 때. 저장한 알람은 건드리지 않아요. */
    fun restart() = state.update { FirstRunUiState(shakeAvailable = it.shakeAvailable) }

    private fun save() {
        state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val saved = book.save(state.value.draft.copy(enabled = true))
            state.update { it.copy(saving = false, exit = FirstRunExit.Saved(saved.id)) }
        }
    }

    companion object {
        /** 첫 알람 처음 값: 새 알람과 같고 반복만 매일(알라미와 같음, 기획 O4-1). 요일은 나중에 알람 편집에서 바꿔요. */
        fun firstAlarm(): Alarm = EditViewModel.newAlarm().copy(repeatDays = DayOfWeek.entries.toSet())
    }
}
