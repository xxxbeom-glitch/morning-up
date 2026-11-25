package com.lumian.morningup.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumian.morningup.core.model.AlarmBook
import com.lumian.morningup.core.model.WakeCheckGate
import com.lumian.morningup.core.model.WakeCheckSettings
import com.lumian.morningup.core.model.WakeCheckSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * T1 '다시 잠들기 방지' 카드('묻는 때', '한 번 더 확인', 결정 145, 164). 값은 앱 전체에 하나예요. 다음 알람부터 새 값으로 울려요.
 * '한 번 더 확인'을 끄면 남은 W3도 멈춰요.
 */
@HiltViewModel
class WakeCheckSettingsViewModel @Inject constructor(
    private val store: WakeCheckSettingsStore,
    private val checks: WakeCheckGate,
    book: AlarmBook,
) : ViewModel() {
    val settings: StateFlow<WakeCheckSettings> = store.settings

    /** 켜진 알람 가운데 다시 잠들기 방지를 켠 알람이 하나도 없으면 참(결정 164). */
    val noGuardedAlarm: StateFlow<Boolean> = book.noGuardedAlarm(viewModelScope)

    init {
        // 예전 알람마다 값을 옮기기 전이면 옮긴 값으로 보여 줘요.
        viewModelScope.launch { store.current() }
    }

    fun setAskAfter(minutes: Int) {
        if (minutes in WakeCheckSettings.ASK_MINUTES) store.update { it.copy(askAfterMinutes = minutes) }
    }

    fun setAnswer(seconds: Int) {
        if (seconds in WakeCheckSettings.ANSWER_SECONDS) store.update { it.copy(answerSeconds = seconds) }
    }

    fun setRecheckMinutes(minutes: Int) {
        if (minutes in WakeCheckSettings.RECHECK_MINUTES) store.update { it.copy(recheckMinutes = minutes) }
    }

    fun setRecheck(on: Boolean) {
        store.update { it.copy(recheckEnabled = on) }
        if (!on) viewModelScope.launch { checks.recheckTurnedOff() }
    }
}
