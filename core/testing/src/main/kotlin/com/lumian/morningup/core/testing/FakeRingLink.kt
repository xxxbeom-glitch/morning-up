package com.lumian.morningup.core.testing

import com.lumian.morningup.core.alarm.ring.RingLink
import com.lumian.morningup.core.alarm.ring.RingScreen
import com.lumian.morningup.core.model.engine.RingEvent
import com.lumian.morningup.core.model.engine.RingSession
import kotlinx.coroutines.flow.MutableStateFlow

/** 가짜 울림 창구. 화면 테스트에서 상태를 직접 바꾸고, 보낸 사건을 [sent]로 확인해요. */
class FakeRingLink(
    session: RingSession? = null,
    screen: RingScreen = RingScreen.None,
) : RingLink {
    override val session = MutableStateFlow(session)
    override val screen = MutableStateFlow(screen)

    val sent = mutableListOf<RingEvent>()
    var wakeUpClosed = 0
        private set

    override fun send(event: RingEvent) {
        sent += event
    }

    /** [release]가 돌려줄 값. 불린 횟수는 [released]예요. */
    var releaseResult = true
    var released = 0
        private set

    override suspend fun release(): Boolean {
        released++
        return releaseResult
    }

    override fun closeRelease() {
        if (screen.value == RingScreen.Release) screen.value = RingScreen.None
    }

    override fun closeWakeUp() {
        wakeUpClosed++
        if (screen.value == RingScreen.WakeUp) screen.value = RingScreen.None
    }
}
