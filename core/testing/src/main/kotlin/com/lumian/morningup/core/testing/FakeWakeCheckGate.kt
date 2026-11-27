package com.lumian.morningup.core.testing

import com.lumian.morningup.core.model.WakeCheckGate
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 가짜 남은 확인 창구. [pendingFor]에 넣은 알람은 멈출 확인이 있는 것으로, [ringingFor]는 울리는 중으로 봐요.
 * 불린 것을 [calls]에 남겨요.
 */
class FakeWakeCheckGate(
    var pendingFor: Set<Long> = emptySet(),
    var ringingFor: Set<Long> = emptySet(),
) : WakeCheckGate {
    override val recheckPending = MutableStateFlow(false)
    override val recheckLeft = MutableStateFlow<Int?>(null)

    val calls = mutableListOf<String>()

    override val restingUntil = MutableStateFlow<Long?>(null)
    override val restingSnoozed = MutableStateFlow(false)
    override val wakeCheckWaiting = MutableStateFlow(false)
    override val wakeCheckAt = MutableStateFlow<Long?>(null)

    override fun isActive(alarmId: Long): Boolean = alarmId in pendingFor || alarmId in ringingFor

    override fun isRinging(alarmId: Long): Boolean = alarmId in ringingFor

    override fun openRest() {
        calls += "open_rest"
    }

    override suspend fun cancelFor(alarmId: Long): Boolean {
        calls += "cancel:$alarmId"
        val had = alarmId in pendingFor
        pendingFor = pendingFor - alarmId
        return had
    }

    override suspend fun recheckTurnedOff() {
        calls += "recheck_off"
        recheckPending.value = false
    }

    override fun openRecheck() {
        calls += "open"
    }

    override val releaseNotice = MutableStateFlow(false)

    override fun openRelease() {
        calls += "open_release"
    }

    override fun consumeReleaseNotice() {
        calls += "release_seen"
        releaseNotice.value = false
    }
}
