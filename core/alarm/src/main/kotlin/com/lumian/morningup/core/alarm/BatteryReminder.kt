package com.lumian.morningup.core.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.lumian.morningup.core.model.engine.AlarmStore
import com.lumian.morningup.core.model.engine.BatteryReminderPolicy
import com.lumian.morningup.core.model.engine.HolidayCalendar
import com.lumian.morningup.core.model.engine.NextAlarm
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 배터리 전날 밤 알림(결정 113, 119). 매일 밤 9시(추천안)에 한 번 깨어나 [BatteryReminderPolicy]로 보낼지 정하고,
 * 다음 날 밤 9시를 다시 예약해요. 알림을 누르면 앱이 나중용 권한 시트 PL2를 2단계(필수가 꺼져 있으면 1단계)로 열어요.
 * 정확한 시각이 중요하지 않아 정확한 알람 대신 절전 중에도 깨는 보통 예약을 써요(몇 분 늦을 수 있어요).
 */
@Singleton
class BatteryReminder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
    private val store: AlarmStore,
    private val holidays: HolidayCalendar,
    private val now: NowProvider,
) {
    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    private val manager = NotificationManagerCompat.from(context)

    /** 다음 확인 시각을 예약해요. 앱을 열 때와 부팅, 시간 변경 때 불러요. 같은 칸을 덮어써서 여러 번 불러도 하나예요. */
    fun schedule() {
        val at = BatteryReminderPolicy.nextCheckAt(now.now()).toInstant().toEpochMilli()
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, checkIntent())
    }

    /** 밤 9시 확인. 조건이 맞으면 알림을 하루 한 번 보내요. */
    suspend fun check() {
        val time = now.now()
        val next = NextAlarm.find(store.all(), time, holidays)?.at
        val battery = context.getSystemService(PowerManager::class.java)
        val exempt = battery?.isIgnoringBatteryOptimizations(context.packageName) == true
        val last = prefs.getString(KEY_LAST, null)?.let(LocalDate::parse)
        if (BatteryReminderPolicy.shouldNotify(time, next, manager.areNotificationsEnabled(), exempt, last)) {
            post()
            prefs.edit { putString(KEY_LAST, time.toLocalDate().toString()) }
        }
    }

    @SuppressLint("MissingPermission")
    private fun post() {
        val channel = NotificationChannel(
            CHANNEL,
            context.getString(R.string.channel_reminder),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.channel_reminder_description) }
        manager.createNotificationChannel(channel)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_alarm_clock)
            .setContentTitle(context.getString(R.string.battery_reminder_title))
            .setContentText(context.getString(R.string.battery_reminder_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.battery_reminder_text)))
            .setContentIntent(openIntent())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        if (manager.areNotificationsEnabled()) manager.notify(NOTIFICATION_ID, notification)
    }

    private fun checkIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, BatteryReminderReceiver::class.java).setAction(ACTION_CHECK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun openIntent(): PendingIntent? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launch ->
            launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            launch.putExtra(AlarmIntents.EXTRA_OPEN_BATTERY_SHEET, true)
            PendingIntent.getActivity(
                context,
                REQUEST_CODE,
                launch,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

    internal companion object {
        const val ACTION_CHECK = "com.lumian.morningup.alarm.BATTERY_REMINDER"
        private const val PREFS = "battery_reminder"
        private const val KEY_LAST = "last_notified"
        private const val CHANNEL = "reminder"
        private const val NOTIFICATION_ID = 5

        /** 알람 ID(1부터), 울림 타이머(-1)와 겹치지 않는 요청 코드. */
        private const val REQUEST_CODE = -2
    }
}

/** 밤 9시 배터리 확인. 확인한 뒤 다음 날 밤을 다시 예약해요. */
@AndroidEntryPoint
class BatteryReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var reminder: BatteryReminder

    @Inject
    @AlarmScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BatteryReminder.ACTION_CHECK) return
        val pending = goAsync()
        scope.launch {
            try {
                reminder.check()
            } finally {
                reminder.schedule()
                pending.finish()
            }
        }
    }
}
