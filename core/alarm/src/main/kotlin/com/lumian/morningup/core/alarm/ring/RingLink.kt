package com.lumian.morningup.core.alarm.ring

import android.content.Context
import com.lumian.morningup.core.model.engine.RingEvent
import com.lumian.morningup.core.model.engine.RingSession
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

/**
 * 울림 화면(feature)이 보는 창구. 상태는 [RingController]의 [session], [screen]만 보고 그려요.
 * 버튼은 [send]로 울림 서비스에 보내요(서비스가 앞에 떠 있어야 소리와 타이머가 이어져서 직접 부르지 않아요).
 * 테스트는 core:testing의 `FakeRingLink`를 써요.
 */
interface RingLink {
    val session: StateFlow<RingSession?>
    val screen: StateFlow<RingScreen>

    fun send(event: RingEvent)

    /** W1 해제 완료를 닫았어요. */
    fun closeWakeUp()

    /** M5 미션 포기에서 하던 미션 화면으로 돌아가요(결정 171). */
    fun backToMission() = Unit

    /** W4에서 10초를 다 눌러 남은 재확인을 끝내요(TSK-266). 끝냈으면 true예요. */
    suspend fun release(): Boolean = false

    /** W4를 X나 뒤로로 닫아요. 기다림은 그대로예요. */
    fun closeRelease() = Unit
}

@Singleton
class ServiceRingLink @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: RingController,
) : RingLink {
    override val session: StateFlow<RingSession?> get() = controller.session
    override val screen: StateFlow<RingScreen> get() = controller.screen

    override fun send(event: RingEvent) = RingService.send(context, event)

    override fun closeWakeUp() = controller.clearScreen()

    override fun backToMission() = controller.backToMission()

    override suspend fun release(): Boolean = controller.release()

    override fun closeRelease() = controller.closeRelease()
}
