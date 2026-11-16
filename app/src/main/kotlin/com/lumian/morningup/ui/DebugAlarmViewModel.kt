package com.lumian.morningup.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.data.AlarmRepository
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.RingAgain
import com.lumian.morningup.core.model.WakeCheck
import com.lumian.morningup.core.model.engine.AlarmRescheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 디버그 견본 화면의 '1분 뒤 울리기'. 폰에서 엔진 흐름을 확인하는 용도예요. */
@HiltViewModel
class DebugAlarmViewModel @Inject constructor(
    private val repository: AlarmRepository,
    private val rescheduler: AlarmRescheduler,
    private val now: NowProvider,
) : ViewModel() {
    private val _scheduledAt = MutableStateFlow<LocalTime?>(null)
    val scheduledAt: StateFlow<LocalTime?> = _scheduledAt.asStateFlow()

    /** 다음 분 정각에 울리는 한 번 알람을 만들어요. 다시 울림은 3분 간격 1번이라 금방 확인할 수 있어요. */
    fun scheduleTestAlarm(withMission: Boolean) {
        viewModelScope.launch {
            val current = now.now()
            val time = current.toLocalTime().plusMinutes(1).truncatedTo(ChronoUnit.MINUTES)
            val alarm = repository.insert(
                Alarm(
                    time = time,
                    missions = if (withMission) listOf(MissionType.QuickMath) else emptyList(),
                    ringAgain = RingAgain(intervalMinutes = 3, count = 1),
                    wakeCheck = WakeCheck(enabled = withMission, askAfterMinutes = 1, answerSeconds = 30),
                ),
            )
            rescheduler.reschedule(alarm, current)
            _scheduledAt.value = time
        }
    }
}
