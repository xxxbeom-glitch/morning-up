package com.lumian.morningup.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumian.morningup.R
import com.lumian.morningup.core.alarm.BatteryReminder
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.data.AlarmRepository
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.MissionSetting
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

/**
 * 설정 탭 '시험 알람' A~G(11-17 사용자 지시: 설정 조합을 체계적으로 시험).
 * 기능마다 한 번씩 확인되도록 고른 7가지예요. 계획과 결과는 docs/test-alarm-cases.md에 적어요.
 * 모두 다음 분 정각에 울리는 한 번 알람이고, 소리는 'Spring Season'(디버그 빌드는 10% 크기)이에요.
 */
enum class AlarmTestCase(@StringRes val label: Int) {
    /** 전부 기본값: 미션 없음, 다시 울림 꺼짐. 1분 울리고 놓친 알람으로 끝나요. */
    A(R.string.test_case_a),

    /** 다시 울림 3분 간격 3번. 방치하면 13분 동안 4번 울리고 끝나요. */
    B(R.string.test_case_b),

    /** 미션(Drop Words 3개)과 전원 종료 방지. 미션 중 소리 끔, 59초 손 놓으면 다시 울림, 전원 메뉴 막힘. */
    C(R.string.test_case_c),

    /** 미션 있음과 다시 잠들기 방지 1분 뒤, 답할 시간 30초. 답 안 하면 처음부터 다시 울려요. */
    D(R.string.test_case_d),

    /** 진동 끔과 부드럽게 깨우기 30초. 작은 소리에서 30초 동안 커져요. */
    E(R.string.test_case_e),

    /** 알람음 끔(진동만). */
    F(R.string.test_case_f),

    /** 다시 울림 3분 간격, 끌 때까지. 횟수를 다 써도 멈추지 않아요. */
    G(R.string.test_case_g),
}

@HiltViewModel
class DebugAlarmViewModel @Inject constructor(
    private val repository: AlarmRepository,
    private val rescheduler: AlarmRescheduler,
    private val now: NowProvider,
    private val batteryReminder: BatteryReminder,
) : ViewModel() {
    private val _scheduled = MutableStateFlow<Pair<AlarmTestCase, LocalTime>?>(null)
    val scheduled: StateFlow<Pair<AlarmTestCase, LocalTime>?> = _scheduled.asStateFlow()

    private val _batterySent = MutableStateFlow<Boolean?>(null)

    /** 배터리 알림 바로 보내기 결과. true 보냄, false 알림 권한이 없어 못 보냄, null 아직 안 누름. */
    val batterySent: StateFlow<Boolean?> = _batterySent.asStateFlow()

    /** 배터리 전날 밤 알림을 밤 9시와 조건 없이 지금 보내요(PL2 열기 확인용). */
    fun sendBatteryReminder() {
        _batterySent.value = batteryReminder.postNowForTest()
    }

    fun schedule(case: AlarmTestCase) {
        viewModelScope.launch {
            val current = now.now()
            val time = current.toLocalTime().plusMinutes(1).truncatedTo(ChronoUnit.MINUTES)
            val alarm = repository.insert(alarmFor(case, time))
            rescheduler.reschedule(alarm, current)
            _scheduled.value = case to time
        }
    }

    companion object {
        private const val TEST_SOUND_ID = "spring_season"
        private const val INTERVAL_MINUTES = 3
        private const val RING_AGAIN_COUNT = 3
        private const val GENTLE_SECONDS = 30
        private const val ANSWER_SECONDS = 30

        /** 미션 시험은 빨리 끝나도록 짧게: C는 Drop Words 3개(11-17 사용자 지시), D는 Quick Math 1문제. */
        private const val DROP_WORDS_TEST_COUNT = 3
        private val DROP_WORDS_MISSION =
            listOf(MissionSetting(MissionType.DropWords, count = DROP_WORDS_TEST_COUNT, forTest = true))
        private val ONE_QUICK_MISSION = listOf(MissionSetting(MissionType.QuickMath, level = 1, count = 1))

        fun alarmFor(case: AlarmTestCase, time: LocalTime): Alarm {
            val base = Alarm(time = time, soundId = TEST_SOUND_ID, ringAgain = RingAgain(count = 0))
            return when (case) {
                AlarmTestCase.A -> base
                AlarmTestCase.B -> base.copy(ringAgain = RingAgain(INTERVAL_MINUTES, RING_AGAIN_COUNT))
                AlarmTestCase.C -> base.copy(missions = DROP_WORDS_MISSION, powerOffGuard = true)
                AlarmTestCase.D -> base.copy(
                    missions = ONE_QUICK_MISSION,
                    wakeCheck = WakeCheck(enabled = true, askAfterMinutes = 1, answerSeconds = ANSWER_SECONDS),
                )
                AlarmTestCase.E -> base.copy(vibrate = false, gentleWake = true, gentleSeconds = GENTLE_SECONDS)
                AlarmTestCase.F -> base.copy(soundId = Alarm.SILENT_SOUND_ID)
                AlarmTestCase.G -> base.copy(ringAgain = RingAgain(INTERVAL_MINUTES, count = null))
            }
        }
    }
}
