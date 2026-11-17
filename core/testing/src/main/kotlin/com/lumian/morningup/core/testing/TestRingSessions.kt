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
) = RingSession(
    plan = RingPlan(
        alarmId = alarmId,
        missions = missions.map { it.type },
        intervalMinutes = 5,
        ringAgainCount = null,
        wakeCheckEnabled = true,
        wakeCheckAskAfterMinutes = 1,
        wakeCheckAnswerSeconds = wakeCheckAnswerSeconds,
        missionSettings = missions,
    ),
    phase = phase,
    ringsLeft = null,
    missionsDone = missionsDone,
    phaseEndsAt = phaseEndsAt,
)
