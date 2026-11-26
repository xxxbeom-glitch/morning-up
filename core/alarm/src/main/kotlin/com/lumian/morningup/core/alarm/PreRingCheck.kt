package com.lumian.morningup.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lumian.morningup.core.model.engine.AlarmStore
import com.lumian.morningup.core.model.engine.HolidayCalendar
import com.lumian.morningup.core.model.engine.NextAlarm
import com.lumian.morningup.core.model.engine.PreRingCheckPolicy
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 울리기 10분 전 방해 금지 점검(TSK-177 답 2, R2). 다음 알람 10분 전에 한 번 깨어, 방해 금지가 알람 소리를 막고 있으면
 * 진단 기록을 남겨요([SilencedRingLog.onPreRing]). 울릴 때 막혀 있으면 [SilencedRingLog]가 다음에 앱을 열 때 알려요.
 * 예약은 한 칸이라 여러 번 불러도 하나예요. 앱을 열 때, 알람을 저장하거나 켜고 끌 때, 부팅, 점검 뒤에 다시 잡아요.
 */
@Singleton
class PreRingCheck @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
    private val store: AlarmStore,
    private val holidays: HolidayCalendar,
    private val now: NowProvider,
    private val silencedRings: SilencedRingLog,
) {
    /** 다음 알람의 점검을 예약해요. 알람이 없으면 예약을 지워요. */
    suspend fun schedule() {
        val from = PreRingCheckPolicy.searchFrom(now.now())
        val at = PreRingCheckPolicy.checkAt(NextAlarm.find(store.all(), from, holidays)?.at)
        if (at == null) {
            alarmManager.cancel(checkIntent())
            return
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), checkIntent())
    }

    /** 10분 전 점검. 막혀 있으면 기록해요. */
    suspend fun check() {
        val next = NextAlarm.find(store.all(), now.now(), holidays)
        silencedRings.onPreRing(next?.alarm?.id)
    }

    private fun checkIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, PreRingCheckReceiver::class.java).setAction(ACTION_CHECK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    internal companion object {
        const val ACTION_CHECK = "com.lumian.morningup.alarm.PRE_RING_CHECK"

        /** 알람 ID(1부터), 울림 타이머(-1), 배터리 점검(-2)과 겹치지 않는 요청 코드. */
        private const val REQUEST_CODE = -3
    }
}

/** 울리기 10분 전 점검. 점검한 뒤 다음 알람을 다시 예약해요. */
@AndroidEntryPoint
class PreRingCheckReceiver : BroadcastReceiver() {
    @Inject lateinit var check: PreRingCheck

    @Inject
    @AlarmScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != PreRingCheck.ACTION_CHECK) return
        val pending = goAsync()
        scope.launch {
            try {
                check.check()
            } finally {
                check.schedule()
                pending.finish()
            }
        }
    }
}
