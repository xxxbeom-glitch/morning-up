package com.lumian.morningup.debug

import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.RingAgain
import com.lumian.morningup.core.model.WakeCheck
import java.time.LocalTime

/**
 * 시험 알람 A~G(11-17 사용자 지시: 설정 조합을 체계적으로 시험). 디버그 빌드에서 adb 명령으로만 걸어요(TSK-164,
 * docs/qa/README.md).
 * 기능마다 한 번씩 확인되도록 고른 7가지예요. 계획과 결과는 docs/test-alarm-cases.md에 적어요.
 * 모두 다음 분 정각에 울리는 한 번 알람이고, 소리는 'Spring Season'(디버그 빌드는 10% 크기)이에요.
 */
enum class AlarmTestCase {
    /** 전부 기본값: 미션 없음, 다시 울림 꺼짐. 2분 울리고 놓친 알람으로 끝나요. */
    A,

    /** 다시 울림 3분 간격 3번. 방치하면 13분 동안 4번 울리고 끝나요. */
    B,

    /** 미션(Drop Words 3개)과 전원 종료 방지. 미션 중 소리 끔, 59초 손 놓으면 다시 울림, 전원 메뉴 막힘. */
    C,

    /**
     * 미션 있음과 다시 잠들기 방지 켬. 간격과 대답할 시간은 전체 설정을 따라요(결정 145). 빨리 시험하려면
     * WAKE_CHECK 명령으로 1분, 30초처럼 줄여요. 답 안 하면 처음부터 다시 울려요.
     */
    D,

    /** 진동 끔과 부드럽게 깨우기 30초. 작은 소리에서 30초 동안 커져요. */
    E,

    /** 알람음 끔(진동만). */
    F,

    /** 다시 울림 3분 간격, 끌 때까지. 횟수를 다 써도 멈추지 않아요. */
    G,
}

object AlarmTestCases {
    private const val TEST_SOUND_ID = "spring_season"
    private const val INTERVAL_MINUTES = 3
    private const val RING_AGAIN_COUNT = 3
    private const val GENTLE_SECONDS = 30

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
                wakeCheck = WakeCheck(enabled = true),
            )
            AlarmTestCase.E -> base.copy(vibrate = false, gentleWake = true, gentleSeconds = GENTLE_SECONDS)
            AlarmTestCase.F -> base.copy(soundId = Alarm.SILENT_SOUND_ID)
            AlarmTestCase.G -> base.copy(ringAgain = RingAgain(INTERVAL_MINUTES, count = null))
        }
    }
}
