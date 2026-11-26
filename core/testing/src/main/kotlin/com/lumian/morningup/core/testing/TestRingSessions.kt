package com.lumian.morningup.core.testing

import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.engine.RingPhase
import com.lumian.morningup.core.model.engine.RingPlan
import com.lumian.morningup.core.model.engine.RingSession

/** 화면 테스트용 울림 세션. */
fun testRingSession(
    phase: RingPhase = RingPhase.Ringing,
    missions: List<MissionSetting> = emptyList(),
    missionsDone: Int = 0,
    phaseEndsAt: Long = 0L,
    alarmId: Long = 1L,
    wakeCheckAnswerSeconds: Int = 60,
    wakeCheckAskAfterMinutes: Int = 1,
    recheckEnabled: Boolean = false,
    pausedLeftMillis: Long? = null,
    recheckWaitMinutes: Int = 3,
    recheckCount: Int = 1,
    checksDone: Int = 0,
) = RingSession(
    plan = RingPlan(
        alarmId = alarmId,
        missions = missions.map { it.type },
        intervalMinutes = 5,
        ringAgainCount = null,
        wakeCheckEnabled = true,
        wakeCheckAskAfterMinutes = wakeCheckAskAfterMinutes,
        wakeCheckAnswerSeconds = wakeCheckAnswerSeconds,
        missionSettings = missions,
        recheckEnabled = recheckEnabled,
        recheckWaitMinutes = recheckWaitMinutes,
        recheckCount = recheckCount,
    ),
    phase = phase,
    ringsLeft = null,
    missionsDone = missionsDone,
    phaseEndsAt = phaseEndsAt,
    pausedLeftMillis = pausedLeftMillis,
    checksDone = checksDone,
)
