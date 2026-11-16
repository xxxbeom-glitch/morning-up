package com.lumian.morningup.core.model.engine

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.model.MissionType
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
}
