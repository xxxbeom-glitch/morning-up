package com.lumian.morningup.core.alarm

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.lumian.morningup.core.alarm.receiver.AlarmFireReceiver
import com.lumian.morningup.core.alarm.receiver.RingTimerReceiver
import com.lumian.morningup.core.alarm.ring.RingService
import com.lumian.morningup.core.model.engine.TimerKind

/** 알람 모듈이 주고받는 Intent 모양을 한 곳에 모아요. */
object AlarmIntents {
    const val ACTION_FIRE = "com.lumian.morningup.alarm.FIRE"
    const val ACTION_TIMER = "com.lumian.morningup.alarm.TIMER"
    const val ACTION_RESUME = "com.lumian.morningup.alarm.RESUME"
    const val EXTRA_ALARM_ID = "alarmId"
    const val EXTRA_TIMER_KIND = "timerKind"

    /** 울림 타이머는 한 칸뿐이라 요청 코드 하나를 계속 써요. 알람 ID(1부터)와 겹치지 않게 음수로 둬요. */
    const val TIMER_REQUEST_CODE = -1

    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    /** 알람 ID마다 다른 요청 코드라 같은 ID로 다시 예약하면 앞의 것을 바꿔요. */
    fun firePendingIntent(context: Context, alarmId: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        alarmId.toInt(),
        Intent(context, AlarmFireReceiver::class.java).setAction(ACTION_FIRE).putExtra(EXTRA_ALARM_ID, alarmId),
        FLAGS,
    )

    /** 종류를 Intent에 담아도 요청 코드가 같아서 늘 한 칸을 덮어써요. */
    fun timerPendingIntent(context: Context, kind: TimerKind?): PendingIntent = PendingIntent.getBroadcast(
        context,
        TIMER_REQUEST_CODE,
        Intent(context, RingTimerReceiver::class.java).setAction(ACTION_TIMER).apply {
            if (kind != null) putExtra(EXTRA_TIMER_KIND, kind.name)
        },
        FLAGS,
    )

    fun serviceIntent(context: Context, action: String): Intent =
        Intent(context, RingService::class.java).setAction(action)

    fun timerKindOf(intent: Intent): TimerKind? =
        intent.getStringExtra(EXTRA_TIMER_KIND)?.let { name -> TimerKind.entries.firstOrNull { it.name == name } }
}
