package com.lumian.morningup.core.alarm

import android.content.Context
import com.lumian.morningup.core.alarm.receiver.startRingService
import com.lumian.morningup.core.alarm.ring.RingNotifications
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.engine.CatchUpHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 폰이 꺼져 있던 동안 지난 알람: 10분 안이면 바로 울리고, 넘으면 '놓친 알람' 알림을 남겨요. */
@Singleton
class AndroidCatchUpHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notifications: RingNotifications,
    private val diagnostics: AlarmDiagnostics,
) : CatchUpHandler {
    override fun ringNow(alarm: Alarm) {
        diagnostics.record("catch_up_ring", alarm.id)
        val intent = AlarmIntents.serviceIntent(context, AlarmIntents.ACTION_FIRE)
            .putExtra(AlarmIntents.EXTRA_ALARM_ID, alarm.id)
        startRingService(context, intent, diagnostics)
    }

    override fun notifyMissed(alarm: Alarm) {
        diagnostics.record("catch_up_missed", alarm.id)
        notifications.missed(alarm.id, alarm.time, poweredOff = true)
    }
}
