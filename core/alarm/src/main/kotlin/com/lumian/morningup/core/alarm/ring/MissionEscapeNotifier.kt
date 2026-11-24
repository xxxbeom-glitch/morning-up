package com.lumian.morningup.core.alarm.ring

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.edit
import com.lumian.morningup.core.alarm.AlarmDiagnostics
import com.lumian.morningup.core.alarm.R
import com.lumian.morningup.core.alarm.receiver.MissionEscapeReceiver
import com.lumian.morningup.core.model.engine.MissionEscape
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.temporal.TemporalAccessor
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 미션 비상구 알림(결정 149~151, TSK-182). 첫 미션을 시작할 때 조용하고 밀어서 지울 수 없는 알림 하나를 띄우고,
 * 알림 안의 바로 답장 칸(`RemoteInput`)으로 예문을 받아요. 울리는 중 상태 알림과는 따로 하나예요(ID 5).
 * 맞게 쓰면 '알람을 껐어요'를 다른 알림(ID 6)으로 3초 보여 줘요.
 * 지금 예문과 '틀림' 표시는 재부팅 직후에도 읽도록 기기 보호 저장소에 둬요. 쓴 글 자체는 어디에도 남기지 않아요.
 */
@Singleton
class MissionEscapeNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notifications: RingNotifications,
    private val diagnostics: AlarmDiagnostics,
) {
    private val manager = NotificationManagerCompat.from(context)
    private val prefs: SharedPreferences by lazy {
        context.createDeviceProtectedStorageContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    private val sentences: Array<String> get() = context.resources.getStringArray(R.array.escape_sentences)

    /** 지금 따라 쓸 예문. 알림이 없으면 null이에요. */
    val currentSentence: String?
        get() = prefs.getInt(KEY_CURRENT, NONE).takeIf { it != NONE }?.let { sentences.getOrNull(it) }

    /**
     * 알림을 띄워요. [fresh]면 바로 전 예문을 빼고 새로 골라요(4절). 아니면 지금 예문과 틀림 표시 그대로 다시 올려요
     * (앱이 다시 살아났을 때, 사용자가 알림을 밀어서 지웠을 때).
     */
    fun show(alarmId: Long?, fresh: Boolean) {
        val all = sentences
        val current = prefs.getInt(KEY_CURRENT, NONE)
        val index = if (fresh || current !in all.indices) {
            val previous = prefs.getInt(KEY_LAST, NONE).takeIf { it in all.indices }
            MissionEscape.pick(all.size, previous).also { picked ->
                prefs.edit {
                    putInt(KEY_CURRENT, picked)
                    putInt(KEY_LAST, picked)
                    putBoolean(KEY_WRONG, false)
                }
            }
        } else {
            current
        }
        val posted = post(all[index], wrong = prefs.getBoolean(KEY_WRONG, false))
        FlowLog.event(EVENT_SHOWN, alarmId, "sentence=${index + 1} fresh=$fresh posted=$posted")
    }

    /** 틀리게 보냈어요. 첫 줄을 '조금 달라요'로 바꾸고 같은 예문을 그대로 둬요(3절). */
    fun showWrong() {
        val sentence = currentSentence ?: return
        prefs.edit { putBoolean(KEY_WRONG, true) }
        post(sentence, wrong = true)
    }

    /** 맞게 보냈어요. 짧은 진동과 함께 '알람을 껐어요'를 3초 보여 주고 사라져요(3절). */
    @SuppressLint("MissingPermission")
    fun showDone() {
        clear()
        vibrateShort()
        if (!manager.areNotificationsEnabled()) return
        val done = NotificationCompat.Builder(context, RingNotifications.CHANNEL_ESCAPE)
            .setSmallIcon(R.drawable.ic_alarm_clock)
            .setContentTitle(context.getString(R.string.escape_done))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setAutoCancel(true)
            .setTimeoutAfter(MissionEscape.DONE_MILLIS)
            .build()
        manager.notify(RingNotifications.ESCAPE_DONE_NOTIFICATION_ID, done)
    }

    /** 알림을 지워요. 미션을 다 끝냈거나, 써서 껐거나, 울림이 끝났어요. 다음에는 새 예문을 골라요. */
    fun clear() {
        manager.cancel(RingNotifications.ESCAPE_NOTIFICATION_ID)
        prefs.edit {
            putInt(KEY_CURRENT, NONE)
            putBoolean(KEY_WRONG, false)
        }
    }

    /**
     * 써서 끈 횟수를 이번 달 몫으로 하나 세고 '이번 달 n번째'를 돌려줘요(7절). 달이 바뀌면 1부터 다시 세요.
     * 진단 기록에만 남기고 밖으로 보내지 않아요.
     */
    fun countUse(now: TemporalAccessor = diagnostics.now()): Pair<String, Int> {
        val month = MONTH.format(now)
        val count = if (prefs.getString(KEY_MONTH, null) == month) prefs.getInt(KEY_MONTH_COUNT, 0) + 1 else 1
        prefs.edit {
            putString(KEY_MONTH, month)
            putInt(KEY_MONTH_COUNT, count)
        }
        return month to count
    }

    @SuppressLint("MissingPermission")
    private fun post(sentence: String, wrong: Boolean): Boolean {
        notifications.ensureChannels()
        // 알림 권한이 꺼져 있으면 이 길이 없어요. 앱 화면에 대신할 길은 만들지 않아요(3절).
        if (!manager.areNotificationsEnabled()) {
            diagnostics.record("escape_blocked", detail = "notifications_off")
            return false
        }
        val first = context.getString(if (wrong) R.string.escape_wrong else R.string.escape_text)
        val body = "$first\n$sentence"
        val input = RemoteInput.Builder(KEY_REPLY)
            .setLabel(context.getString(R.string.escape_hint))
            .build()
        val reply = PendingIntent.getBroadcast(
            context,
            REQUEST_REPLY,
            receiverIntent(MissionEscapeReceiver.ACTION_REPLY),
            // 바로 답장 글을 안드로이드가 채워 넣어야 해서 바꿀 수 있는(mutable) Intent예요. 받는 곳은 우리 앱으로 정해져 있어요.
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val label = context.getString(R.string.escape_action)
        val action = NotificationCompat.Action.Builder(R.drawable.ic_alarm_clock, label, reply)
            .addRemoteInput(input)
            .setAllowGeneratedReplies(false)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setShowsUserInterface(false)
            .build()
        // 안드로이드 14부터는 밀어서 지울 수 없는 알림도 지워질 수 있어서, 지워지면 받는 곳이 같은 예문으로 다시 올려요.
        val dismissed = PendingIntent.getBroadcast(
            context,
            REQUEST_DISMISSED,
            receiverIntent(MissionEscapeReceiver.ACTION_DISMISSED),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, RingNotifications.CHANNEL_ESCAPE)
            .setSmallIcon(R.drawable.ic_alarm_clock)
            .setContentTitle(context.getString(R.string.escape_title))
            .setContentText(first)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setDeleteIntent(dismissed)
            .addAction(action)
            .build()
        manager.notify(RingNotifications.ESCAPE_NOTIFICATION_ID, notification)
        return true
    }

    private fun receiverIntent(action: String) =
        Intent(context, MissionEscapeReceiver::class.java).setAction(action).setPackage(context.packageName)

    private fun vibrateShort() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        vibrator?.vibrate(VibrationEffect.createOneShot(DONE_VIBRATE_MILLIS, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    companion object {
        /** 바로 답장 글을 꺼낼 때 쓰는 이름. */
        const val KEY_REPLY = "escape_reply"

        const val EVENT_SHOWN = "escape_shown"
        const val EVENT_SENT = "escape_sent"

        private const val PREFS = "mission_escape"
        private const val KEY_CURRENT = "current_sentence"
        private const val KEY_LAST = "last_sentence"
        private const val KEY_WRONG = "wrong"
        private const val KEY_MONTH = "month"
        private const val KEY_MONTH_COUNT = "month_count"
        private const val NONE = -1
        private const val REQUEST_REPLY = 50
        private const val REQUEST_DISMISSED = 51
        private const val DONE_VIBRATE_MILLIS = 120L
        private val MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    }
}
