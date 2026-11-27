package com.lumian.morningup.feature.settings

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumian.morningup.core.alarm.NextAlarmNotice
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.alarm.guard.PowerGuardConsent
import com.lumian.morningup.core.alarm.ring.AlarmDndOverride
import com.lumian.morningup.core.alarm.ring.AlarmOutputRoute
import com.lumian.morningup.core.data.AlarmRepository
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.AlarmPermission
import com.lumian.morningup.core.model.PermissionChecker
import com.lumian.morningup.core.model.PermissionReport
import com.lumian.morningup.core.model.engine.AlarmRescheduler
import com.lumian.morningup.core.ui.WeekStart
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

/** T1 설정과 '권한' 화면이 같이 써요. 권한 상태는 앱이 앞으로 나올 때마다 새로 읽혀요. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val permissions: PermissionChecker,
    private val repository: AlarmRepository,
    private val rescheduler: AlarmRescheduler,
    private val now: NowProvider,
    private val powerConsent: PowerGuardConsent,
    @ApplicationContext context: Context,
    private val dndOverride: AlarmDndOverride? = null,
    private val nextAlarmNotice: NextAlarmNotice? = null,
    private val outputRoute: AlarmOutputRoute? = null,
) : ViewModel() {
    private val appContext = context.applicationContext

    private val _weekMonday = MutableStateFlow(WeekStart.isMonday(appContext))

    /** 설정 > 알람 '한 주 시작 요일'. false면 일요일(처음 값), true면 월요일(TSK-287). */
    val weekMonday: StateFlow<Boolean> = _weekMonday.asStateFlow()

    fun setWeekMonday(monday: Boolean) {
        WeekStart.setMonday(appContext, monday)
        _weekMonday.value = monday
    }

    /** 설정 > 소리 '이어폰, 블루투스로 울리기'. 처음 꺼짐(TSK-287). */
    val externalOutput: StateFlow<Boolean> = outputRoute?.enabled ?: MutableStateFlow(false)

    fun setExternalOutput(on: Boolean) {
        outputRoute?.setEnabled(on)
    }

    /** 설정 탭 '다음 알람 늘 보이기' 토글(결정 179). 처음 꺼짐. */
    val nextAlarmShown: StateFlow<Boolean> = nextAlarmNotice?.enabled ?: MutableStateFlow(false)

    fun setNextAlarmShown(on: Boolean) {
        nextAlarmNotice?.setEnabled(on)
    }

    /** P6 접근성 권한 동의를 받았는지. */
    val powerGuardAgreed: StateFlow<Boolean> = powerConsent.agreed

    fun agreePowerGuard() = powerConsent.agree()

    /** 설정 탭 '전원 종료 방지' 토글(결정 101). */
    val powerGuardEnabled: StateFlow<Boolean> = powerConsent.enabled

    fun setPowerGuardEnabled(on: Boolean) = powerConsent.setEnabled(on)

    /** 설정 탭 '방해 금지 잠깐 풀기' 토글(TSK-177 답 3). 처음 꺼짐. */
    val dndOverrideEnabled: StateFlow<Boolean> = dndOverride?.enabled ?: MutableStateFlow(false)

    /** 안드로이드 15부터는 잠깐 풀기가 안 돼서 T2-1 안내로 보내요(ISS-057). */
    val dndOverrideSupported: Boolean get() = dndOverride?.supported ?: true

    fun setDndOverrideEnabled(on: Boolean) {
        dndOverride?.setEnabled(on)
    }

    val report: StateFlow<PermissionReport> = permissions.report

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * 그 권한을 시스템 요청 창으로 물어본 적이 있는지(알림, 위치). 물어본 적이 있는데 시스템이 더 묻지 않으면
     * (두 번 거절) 창 대신 폰 설정으로 보내요(결정 126 줄별 이동 표).
     */
    fun wasAsked(permission: AlarmPermission): Boolean = prefs.getBoolean(askedKey(permission), false)

    fun markAsked(permission: AlarmPermission) = prefs.edit { putBoolean(askedKey(permission), true) }

    private fun askedKey(permission: AlarmPermission) = "$KEY_ASKED_PREFIX${permission.name}"

    private val _tryAlarmAt = MutableStateFlow<LocalTime?>(null)

    /** '알람 미리 울려 보기'로 예약한 시각. 토스트로 3초 보여 주고 비워요(결정 128). */
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
        const val TOAST_MILLIS = 3_000L
        const val PREFS = "settings"
        const val KEY_ASKED_PREFIX = "asked_"
    }
}
