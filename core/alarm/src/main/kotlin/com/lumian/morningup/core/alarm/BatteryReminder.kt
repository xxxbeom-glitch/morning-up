package com.lumian.morningup.core.alarm

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.lumian.morningup.core.model.engine.AlarmStore
import com.lumian.morningup.core.model.engine.BatteryReminderPolicy
import com.lumian.morningup.core.model.engine.HolidayCalendar
import com.lumian.morningup.core.model.engine.NextAlarm
import com.lumian.morningup.core.model.engine.TomorrowIssue
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 밤 9시 '내일 알람 점검'(결정 113, 119, TSK-177 답 5, 시안 N1). 매일 밤 9시에 한 번 깨어나 배터리 '제한', 방해 금지,
 * 배터리 부족, 배터리 예외를 보고 [BatteryReminderPolicy]로 보낼지 정한 뒤, 다음 날 밤 9시를 다시 예약해요.
 * 첫 문제가 배터리 예외면 알림을 눌렀을 때 PL2를 2단계(필수가 꺼져 있으면 1단계)로, 아니면 홈(경고 배너)을 열어요.
 * 정확한 시각이 중요하지 않아 정확한 알람 대신 절전 중에도 깨는 보통 예약을 써요(몇 분 늦을 수 있어요).
 */
@Singleton
class BatteryReminder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
    private val store: AlarmStore,
    private val holidays: HolidayCalendar,
    private val now: NowProvider,
    private val silence: AlarmSilenceReader,
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
        val issues = readIssues()
        val last = prefs.getString(KEY_LAST, null)?.let(LocalDate::parse)
        if (BatteryReminderPolicy.shouldNotify(time, next, manager.areNotificationsEnabled(), issues, last)) {
            post(issues)
            prefs.edit { putString(KEY_LAST, time.toLocalDate().toString()) }
        }
    }

    private fun readIssues(): List<TomorrowIssue> {
        val power = context.getSystemService(PowerManager::class.java)
        val battery = context.getSystemService(BatteryManager::class.java)
        val percent = battery?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..FULL }
        return BatteryReminderPolicy.issues(
            backgroundRestricted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                context.getSystemService(ActivityManager::class.java)?.isBackgroundRestricted == true,
            alarmsSilenced = silence.silenced(),
            batteryPercent = percent,
            charging = battery?.isCharging == true,
            batteryExempt = power?.isIgnoringBatteryOptimizations(context.packageName) == true,
        )
    }

    /** 디버그 '테스트' 페이지용: 밤 9시와 조건을 건너뛰고 지금 바로 보내요. 알림 권한이 없으면 false. */
    fun postNowForTest(): Boolean {
        if (!manager.areNotificationsEnabled()) return false
        post(readIssues().ifEmpty { listOf(TomorrowIssue.BatteryExempt) })
        return true
    }

    @SuppressLint("MissingPermission")
    private fun post(issues: List<TomorrowIssue>) {
        val first = issues.first()
        val titleId = when (first) {
            TomorrowIssue.AlarmsSilenced -> R.string.tomorrow_title_silent
            else -> R.string.battery_reminder_title
        }
        val title = context.getString(titleId)
        val reason = reasonOf(first)
        val text = if (issues.size > 1) context.getString(R.string.tomorrow_more, reason, issues.size - 1) else reason
        val channel = NotificationChannel(
            CHANNEL,
            context.getString(R.string.channel_reminder),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.channel_reminder_description) }
        manager.createNotificationChannel(channel)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_alarm_clock)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openIntent(batterySheet = first == TomorrowIssue.BatteryExempt))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        if (manager.areNotificationsEnabled()) manager.notify(NOTIFICATION_ID, notification)
    }

    private fun reasonOf(issue: TomorrowIssue): String = when (issue) {
        TomorrowIssue.BatteryRestricted -> context.getString(R.string.tomorrow_text_restricted)
        TomorrowIssue.AlarmsSilenced -> context.getString(R.string.tomorrow_text_silenced)
        TomorrowIssue.LowBattery -> context.getString(
            R.string.tomorrow_text_low_battery,
            context.getSystemService(BatteryManager::class.java)
                ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0,
        )
        TomorrowIssue.BatteryExempt -> context.getString(R.string.battery_reminder_text)
    }

    private fun checkIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, BatteryReminderReceiver::class.java).setAction(ACTION_CHECK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun openIntent(batterySheet: Boolean): PendingIntent? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launch ->
            launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (batterySheet) launch.putExtra(AlarmIntents.EXTRA_OPEN_BATTERY_SHEET, true)
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
        /** 미션 포기 알림(ESCAPE 5)과 겹치지 않게 8번을 써요(TSK-222). 울림 알림이 끝날 때 5번을 지워서 이 알림도 사라졌어요. */
        private const val NOTIFICATION_ID = 8
        private const val FULL = 100

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
