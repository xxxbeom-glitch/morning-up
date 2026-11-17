package com.lumian.morningup.feature.settings

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.alarm.guard.PowerGuardConsent
import com.lumian.morningup.core.data.AlarmRepository
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.PermissionChecker
import com.lumian.morningup.core.model.PermissionReport
import com.lumian.morningup.core.model.engine.AlarmRescheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** T1 설정과 T2-A, T2-B 권한 화면이 같이 써요. 권한 상태는 앱이 앞으로 나올 때마다 새로 읽혀요. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val permissions: PermissionChecker,
    private val repository: AlarmRepository,
    private val rescheduler: AlarmRescheduler,
    private val now: NowProvider,
    private val powerConsent: PowerGuardConsent,
    @ApplicationContext context: Context,
) : ViewModel() {
    /** P6 접근성 권한 동의를 받았는지. */
    val powerGuardAgreed: StateFlow<Boolean> = powerConsent.agreed

    fun agreePowerGuard() = powerConsent.agree()

    val report: StateFlow<PermissionReport> = permissions.report

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _firstSaveAsked = MutableStateFlow(prefs.getBoolean(KEY_FIRST_SAVE_ASKED, false))

    /** 첫 알람 저장 때 권한 묻기(F21)를 이미 했는지. 한 번 돌면 다시 묻지 않아요. */
    val firstSaveAsked: StateFlow<Boolean> = _firstSaveAsked.asStateFlow()

    fun markFirstSaveAsked() {
        prefs.edit { putBoolean(KEY_FIRST_SAVE_ASKED, true) }
        _firstSaveAsked.value = true
    }

    private val _tryAlarmAt = MutableStateFlow<LocalTime?>(null)

    /** '알람 미리 울려 보기'로 예약한 시각. 잠깐 토스트로 보여 주고 비워요. */
    val tryAlarmAt: StateFlow<LocalTime?> = _tryAlarmAt.asStateFlow()

    fun refresh() = permissions.refresh()

    /** 다음 분 정각에 미션 없는 한 번 알람을 울려요. 지금 설정대로 정말 울리는지 확인하는 용도예요. */
    fun tryAlarm() {
        viewModelScope.launch {
            val current = now.now()
            val time = current.toLocalTime().plusMinutes(1).truncatedTo(ChronoUnit.MINUTES)
            val alarm = repository.insert(Alarm(time = time))
            rescheduler.reschedule(alarm, current)
            _tryAlarmAt.value = time
            delay(TOAST_MILLIS)
            _tryAlarmAt.value = null
        }
    }

    private companion object {
        const val TOAST_MILLIS = 2_500L
        const val PREFS = "settings"
        const val KEY_FIRST_SAVE_ASKED = "first_save_asked"
    }
}
