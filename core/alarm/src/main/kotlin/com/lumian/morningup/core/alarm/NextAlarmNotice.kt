package com.lumian.morningup.core.alarm

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.lumian.morningup.core.data.AlarmRepository
import com.lumian.morningup.core.model.engine.HolidayCalendar
import com.lumian.morningup.core.model.engine.NextAlarmNoticeContent
import com.lumian.morningup.core.model.engine.NextAlarmNoticePolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * '다음 알람 늘 보이기'(결정 179, 시안 TSK-224 N3). 설정 탭에서 켜면([enabled], 처음 꺼짐) 알림 창에 다음 알람을
 * 지울 수 없는 조용한 알림으로 늘 띄워요. 알람 목록이 바뀌면(저장, 켜고 끄기, 이번만 쉬기, 울린 뒤 다음 예약) 바로 고치고,
 * 그 알람 시각이 지나면 다음 알람으로 다시 써요. 켜진 알람이 없거나 끄면 알림을 지워요. 누르면 앱 홈이 열려요.
 *
 * 알람 저장소는 잠금을 푼 뒤에만 열릴 수 있어서 앱 시작점이 아니라 [start]를 앱을 열 때, 부팅과 시간 변경, 울림 서비스에서 불러요.
 */
@Singleton
class NextAlarmNotice @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AlarmRepository,
    private val holidays: HolidayCalendar,
    private val now: NowProvider,
    @AlarmScope private val scope: CoroutineScope,
) {
    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    private val manager = NotificationManagerCompat.from(context)
    private val enabledState by lazy { MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false)) }
    private val started = AtomicBoolean(false)

    /** 설정 탭 '다음 알람 늘 보이기' 토글. */
    val enabled: StateFlow<Boolean> get() = enabledState.asStateFlow()

    fun setEnabled(on: Boolean) {
        prefs.edit { putBoolean(KEY_ENABLED, on) }
        enabledState.value = on
        start()
    }

    /** 알람 목록과 토글을 지켜보기 시작해요. 여러 번 불러도 한 번만 돌아요. */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            combine(enabledState, repository.alarms) { on, alarms -> on to alarms }
                .collectLatest { (on, alarms) ->
                    if (!on) {
                        manager.cancel(NOTIFICATION_ID)
                        return@collectLatest
                    }
                    // 알람 시각이 지나면 다음 알람으로 다시 써요. 알람 목록이 바뀌면 collectLatest가 이 기다림을 끊어요.
                    while (true) {
                        val content = NextAlarmNoticePolicy.content(alarms, now.now(), holidays)
                        if (content == null) {
                            manager.cancel(NOTIFICATION_ID)
                            return@collectLatest
                        }
                        post(content)
                        val wait = content.at.toInstant().toEpochMilli() - now.millis() + PASS_MILLIS
                        delay(wait.coerceAtLeast(PASS_MILLIS))
                    }
                }
        }
    }

    @SuppressLint("MissingPermission")
    private fun post(content: NextAlarmNoticeContent) {
        if (!manager.areNotificationsEnabled()) return
        val channel = NotificationChannel(
            CHANNEL,
            context.getString(R.string.channel_next_alarm),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_next_alarm_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
        val today = now.now().toLocalDate()
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_alarm_clock)
            .setContentTitle(context.getString(R.string.next_alarm_title, whenText(content.at, today)))
            .setContentText(bodyText(content, today))
            .setContentIntent(openIntent())
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    /** '목요일 오전 6:30'. 일주일 넘게 남았으면 '12월 3일 오전 6:30'이에요. */
    private fun whenText(at: ZonedDateTime, today: LocalDate): String =
        context.getString(R.string.next_alarm_when, dayText(at.toLocalDate(), today), clock12(at))

    private fun bodyText(content: NextAlarmNoticeContent, today: LocalDate): String {
        val rested = content.restedDate
        return when {
            rested != null -> context.getString(
                R.string.next_alarm_rested,
                dayText(rested, today),
                whenText(content.at, today),
            )
            content.repeatDays.isEmpty() -> context.getString(R.string.next_alarm_once)
            else -> context.getString(
                R.string.next_alarm_repeat,
                content.repeatDays.sorted().joinToString(" ") { it.getDisplayName(TextStyle.SHORT, Locale.KOREAN) },
            )
        }
    }

    private fun dayText(date: LocalDate, today: LocalDate): String =
        if (ChronoUnit.DAYS.between(today, date) < DAYS_IN_WEEK) {
            date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)
        } else {
            context.getString(R.string.next_alarm_date, date.monthValue, date.dayOfMonth)
        }

    private fun clock12(at: ZonedDateTime): String {
        val template = if (at.hour < NOON) R.string.clock_am else R.string.clock_pm
        return context.getString(template, at.format(DateTimeFormatter.ofPattern("h:mm")))
    }

    private fun openIntent(): PendingIntent? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launch ->
            launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            PendingIntent.getActivity(
                context,
                REQUEST_CODE,
                launch,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

    internal companion object {
        private const val PREFS = "next_alarm_notice"
        private const val KEY_ENABLED = "enabled"
        private const val CHANNEL = "next_alarm"

        /** 배터리 점검(8)까지와 겹치지 않게 9번을 써요. */
        const val NOTIFICATION_ID = 9

        /** 알람 ID(1부터), 울림 타이머(-1), 배터리 점검(-2), 10분 전 점검(-3)과 겹치지 않는 요청 코드. */
        private const val REQUEST_CODE = -4

        /** 알람 시각에서 이만큼 지나면 다음 알람으로 다시 써요(울림이 예약을 고칠 틈). */
        private const val PASS_MILLIS = 60_000L
        private const val NOON = 12
        private const val DAYS_IN_WEEK = 7L
    }
}
