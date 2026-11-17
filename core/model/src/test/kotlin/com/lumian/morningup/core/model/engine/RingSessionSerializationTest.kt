package com.lumian.morningup.core.model.engine

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import java.time.LocalTime
import kotlinx.serialization.json.Json
import org.junit.Test

/** 울리는 중 상태는 JSON으로 저장했다가 앱이 다시 살아나면 그대로 읽어요. */
class RingSessionSerializationTest {
    @Test
    fun roundTrip_keepsEveryField() {
        val session = RingSession(
            plan = RingPlan(7, listOf(MissionType.QuickMath, MissionType.ShakeShake), 10, null, true, 5, 90),
            phase = RingPhase.InMission,
            ringsLeft = null,
            missionsDone = 1,
            phaseEndsAt = 1_800_000_059_000,
        )
        val json = Json.encodeToString(RingSession.serializer(), session)
        assertThat(Json.decodeFromString(RingSession.serializer(), json)).isEqualTo(session)
    }

    @Test
    fun roundTrip_keepsMissionSettings() {
        val alarm = Alarm(
            id = 4,
            time = LocalTime.of(6, 30),
            missions = listOf(MissionSetting(MissionType.QuickMath, level = 4, count = 5)),
        )
        val session = RingSession(RingPlan.from(alarm), RingPhase.Ringing, 3, 0, 1_800_000_060_000)
        val json = Json.encodeToString(RingSession.serializer(), session)
        val back = Json.decodeFromString(RingSession.serializer(), json)
        assertThat(back).isEqualTo(session)
        assertThat(back.plan.missionSetting(0)).isEqualTo(MissionSetting(MissionType.QuickMath, 4, 5))
    }

    /** 3단계까지 저장된 세션 JSON에는 missionSettings가 없어요. 처음 값으로 채워 읽어요. */
    @Test
    fun oldSessionJson_withoutSettings_readsWithDefaults() {
        val old = """{"plan":{"alarmId":7,"missions":["QuickMath"],"intervalMinutes":5,"ringAgainCount":3,""" +
            """"wakeCheckEnabled":false,"wakeCheckAskAfterMinutes":1,"wakeCheckAnswerSeconds":60},""" +
            """"phase":"InMission","ringsLeft":3,"missionsDone":0,"phaseEndsAt":1800000059000}"""
        val session = Json { ignoreUnknownKeys = true }.decodeFromString(RingSession.serializer(), old)
        assertThat(session.plan.missionSettings).isEmpty()
        assertThat(session.plan.missionSetting(0)).isEqualTo(MissionSetting.default(MissionType.QuickMath))
        assertThat(session.plan.missionSetting(1)).isNull()
    }

    @Test
    fun mismatchedSetting_fallsBackToDefault() {
        val plan =
            RingPlan(1, listOf(MissionType.QuickMath), 5, 3, false, 1, 60, listOf(MissionSetting(MissionType.PopQuiz)))
        assertThat(plan.missionSetting(0)).isEqualTo(MissionSetting.default(MissionType.QuickMath))
    }
}
