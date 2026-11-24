package com.lumian.morningup.core.alarm.ring

import com.lumian.morningup.core.alarm.AlarmDiagnostics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * '확실히 잠깨기' 실험실 기록(결정 146 4절). 사건은 모두 디버그 빌드 흐름 로그(`MorningFlow`)에 남기고,
 * [kept]인 사건(4절 * 표시)만 출시 빌드에도 폰 안 진단 기록에 남겨요. 밖으로는 보내지 않아요.
 * 값에는 강도, 횟수, 초, 이유 같은 숫자와 이름만 넣어요.
 */
@Singleton
class LabLog @Inject constructor(private val diagnostics: AlarmDiagnostics) {
    fun record(event: String, alarmId: Long? = null, detail: String = "", kept: Boolean = false) {
        FlowLog.event(event, alarmId, detail)
        if (kept) diagnostics.record(event, alarmId, detail)
    }

    companion object {
        const val TRICK_ON = "lab_trick_on"
        const val TRICK_OFF = "lab_trick_off"
        const val RECHECK_ON = "lab_recheck_on"
        const val RECHECK_OFF = "lab_recheck_off"
        const val TRICK_SHOWN = "trick_shown"
        const val TRICK_START = "trick_start"
        const val TRICK_AUTO_OFF = "trick_auto_off"
        const val TRICK_FAKE_TAP = "trick_fake_tap"
        const val TRICK_END = "trick_end"
        const val RECHECK_ASK = "recheck_ask"

        /** 미션 비상구로 껐어요(결정 149 7절). 출시 빌드에도 남아요. */
        const val ESCAPE_USED = "escape_used"

        /** 설정 T3에서 미션 포기 알림 숨기기를 켜거나 껐어요(결정 151). */
        const val ESCAPE_HIDE_ON = "lab_escape_hide_on"
        const val ESCAPE_HIDE_OFF = "lab_escape_hide_off"
    }
}
