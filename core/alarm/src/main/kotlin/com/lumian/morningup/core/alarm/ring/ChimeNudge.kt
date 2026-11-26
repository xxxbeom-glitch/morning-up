package com.lumian.morningup.core.alarm.ring

import com.lumian.morningup.core.model.engine.RingPhase
import com.lumian.morningup.core.model.engine.RingSession

/**
 * W2 '깨어 있나요?'와 W3 '정말 깼어요?' 알림음 규칙(결정 182, 185). 둘 다 증기 사이렌을 그 알람의 벨소리 볼륨으로,
 * 화면이 뜰 때 한 번, 대답할 1분 중 30초 지점에 한 번 더 울려요.
 */
internal object ChimeNudge {
    /** 끝나기 이만큼 전(1분 중 30초 지점)에 한 번 더 울려요. */
    const val BEFORE_END_MILLIS = 30_000L

    /** 진단 기록 이름(`chime w2`, `chime w3`). 묻는 중이 아니면 null. */
    fun label(phase: RingPhase?): String? = when (phase) {
        RingPhase.WakeCheckAsking -> "w2"
        RingPhase.RecheckAsking -> "w3"
        else -> null
    }

    /** 30초 지점까지 남은 시간. 묻는 중이 아니거나 멈춰 있거나 이미 지났으면 null. */
    fun delayMillis(session: RingSession?, now: Long): Long? {
        if (session == null || label(session.phase) == null || session.pausedLeftMillis != null) return null
        return (session.phaseEndsAt - BEFORE_END_MILLIS - now).takeIf { it > 0 }
    }
}
