package com.lumian.morningup.core.alarm

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import com.lumian.morningup.core.alarm.receiver.AlarmFireReceiver
import com.lumian.morningup.core.alarm.ring.RingNotifications
import com.lumian.morningup.core.model.engine.AlarmStore
import com.lumian.morningup.core.model.engine.ForceStopPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 강제 종료 흔적(TSK-177 답 6, B-03, 시안 N2). 앱을 열 때 예약을 다시 맞추기 전에 [begin]으로 알아채고,
 * 다시 맞춘 뒤 [finish]로 '앱이 강제로 닫혀 알람이 꺼져 있었어요 / 지금 다시 맞췄어요'를 한 번 띄워요.
 * 그사이 놓친 알람 문구는 '앱이 닫혀 있는 동안'으로 나눠요([AndroidCatchUpHandler.appClosed]).
 */
@Singleton
class ForceStopTrace @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: AlarmStore,
    private val catchUp: AndroidCatchUpHandler,
    private val notifications: RingNotifications,
    private val diagnostics: AlarmDiagnostics,
) {
    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    /** 다시 예약하기 전에 불러요. 강제 종료로 보이면 true이고, 놓친 알람 문구를 바꿔 둬요. */
    suspend fun begin(): Boolean {
        val enabled = store.all().filter { it.enabled && it.nextTriggerAt != null }
        val userStop = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) userStopSinceLastOpen() else null
        val wiped = enabled.isNotEmpty() && enabled.none { scheduled(it.id) }
        val detected = ForceStopPolicy.detected(enabled.isNotEmpty(), userStop, wiped)
        if (detected) {
            diagnostics.record(EVENT, detail = "exit_info=$userStop wiped=$wiped")
            catchUp.appClosed = true
        }
        return detected
    }

    /** 다시 예약한 뒤 불러요. [detected]면 알림을 한 번 띄우고 놓친 알람 문구를 되돌려요. */
    fun finish(detected: Boolean) {
        catchUp.appClosed = false
        if (detected) notifications.forceStopped()
    }

    /** 이 알람의 울림 예약(PendingIntent)이 아직 있는지. 강제 종료하면 함께 지워져요. */
    private fun scheduled(alarmId: Long): Boolean = PendingIntent.getBroadcast(
        context,
        alarmId.toInt(),
        Intent(context, AlarmFireReceiver::class.java).setAction(AlarmIntents.ACTION_FIRE),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
    ) != null

    /**
     * 지난번에 본 뒤 '사용자 요청으로 닫힘'(설정의 강제 종료 등) 기록이 있는지. 처음 보는 때는 기준만 잡고 false예요.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun userStopSinceLastOpen(): Boolean {
        val manager = context.getSystemService(ActivityManager::class.java) ?: return false
        val exits = runCatching { manager.getHistoricalProcessExitReasons(context.packageName, 0, MAX_EXITS) }
            .getOrDefault(emptyList())
        val last = prefs.getLong(KEY_LAST_SEEN, NEVER)
        val newest = exits.maxOfOrNull { it.timestamp } ?: last
        prefs.edit { putLong(KEY_LAST_SEEN, maxOf(newest, last, 0L)) }
        if (last == NEVER) return false
        return exits.any { it.timestamp > last && it.reason == ApplicationExitInfo.REASON_USER_REQUESTED }
    }

    private companion object {
        const val PREFS = "force_stop_trace"
        const val KEY_LAST_SEEN = "last_exit_seen"
        const val NEVER = -1L
        const val MAX_EXITS = 10
        const val EVENT = "force_stop_detected"
    }
}
