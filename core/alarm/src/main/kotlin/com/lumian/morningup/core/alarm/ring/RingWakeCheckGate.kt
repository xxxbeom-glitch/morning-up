package com.lumian.morningup.core.alarm.ring

import android.content.Context
import android.content.Intent
import com.lumian.morningup.core.model.WakeCheckGate
import com.lumian.morningup.core.model.engine.RingEvent
import com.lumian.morningup.core.model.engine.RingPhase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * 남은 확인(W2, W3)을 화면에서 다루는 창구(결정 145 '남은 확인 취소'). 상태표에 바로 보내요.
 * 끝난 세션이면 울림 서비스가 스스로 멈춰요.
 */
@Singleton
class RingWakeCheckGate @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: RingController,
    private val screenIntent: RingScreenIntentFactory,
) : WakeCheckGate {
    override val recheckPending: Flow<Boolean> = controller.session
        .map { it?.phase == RingPhase.AwaitingRecheck }
        .distinctUntilChanged()

    override suspend fun cancelFor(alarmId: Long): Boolean {
        val before = controller.session.value
        if (before?.plan?.alarmId != alarmId || !before.phase.isCheck) return false
        controller.send(RingEvent.CancelChecks(alarmId))
        return controller.session.value == null
    }

    override suspend fun recheckTurnedOff() = controller.send(RingEvent.RecheckTurnedOff)

    override fun openRecheck() {
        if (!controller.openRecheckEarly()) return
        val intent = screenIntent.create(context)
            .putExtra(RingNotifications.EXTRA_OPEN_RECHECK, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
