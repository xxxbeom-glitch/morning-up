package com.lumian.morningup.core.alarm.ring

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lumian.morningup.core.alarm.AlarmDiagnostics
import com.lumian.morningup.core.alarm.R
import com.lumian.morningup.core.model.engine.MissionEscape
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.temporal.TemporalAccessor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 미션 비상구 알림(결정 149~151, TSK-182). 첫 미션을 시작할 때 예문과 바로 답장 칸(`RemoteInput`)을 열어요.
 *
 * 처음에는 따로 알림(ID 5)이었는데, 안드로이드(삼성 One UI 포함)가 같은 앱 알림을 그룹 키와 상관없이
 * 'Aggregate_' 묶음으로 모아 접어서, 예문과 버튼을 보려면 두 번 펼쳐야 했어요(ISS-042). 그래서 지금 떠 있는
 * 울림 알림 하나(울림, 미션 중은 ID 1, 쉬는 중은 ID 2)에 예문과 버튼을 붙여요. 앱 알림이 하나뿐이면 묶이지 않아요.
 * 맞게 쓰면 '알람을 껐어요'를 다른 알림(ID 6)으로 3초 보여 줘요.
 */
@Singleton
class MissionEscapeNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notifications: RingNotifications,
    private val store: MissionEscapeStore,
    private val diagnostics: AlarmDiagnostics,
) {
    private val manager = NotificationManagerCompat.from(context)

    /** 지금 따라 쓸 예문. 비상구가 닫혀 있으면 null이에요. */
    val currentSentence: String? get() = store.current()?.sentence

    /**
     * 비상구를 열어요. [fresh]면 바로 전 예문을 빼고 새로 골라요(4절). 아니면 지금 예문과 틀림 표시 그대로 다시 올려요
     * (앱이 다시 살아났을 때, 사용자가 알림을 밀어서 지웠을 때).
     */
    fun show(alarmId: Long?, fresh: Boolean) {
        val index = store.current()?.index?.takeUnless { fresh } ?: store.openFresh()
        val posted = notifications.refreshEscape()
        FlowLog.event(EVENT_SHOWN, alarmId, "sentence=${index + 1} fresh=$fresh posted=$posted")
    }

    /** 틀리게 보냈어요. 첫 줄을 '조금 달라요'로 바꾸고 같은 예문을 그대로 둬요(3절). */
    fun showWrong() {
        if (store.current() == null) return
        store.markWrong()
        notifications.refreshEscape()
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

    /** 비상구를 닫아요. 미션을 다 끝냈거나, 써서 껐거나, 울림이 끝났어요. 다음에는 새 예문을 골라요. */
    fun clear() {
        store.close()
        // 예전 판(따로 알림 ID 5)이 남아 있을 수 있어 같이 지워요.
        manager.cancel(RingNotifications.ESCAPE_NOTIFICATION_ID)
        notifications.refreshEscape()
    }

    /** 써서 끈 횟수를 이번 달 몫으로 하나 세고 '이번 달 n번째'를 돌려줘요(7절). 진단 기록에만 남겨요. */
    fun countUse(now: TemporalAccessor = diagnostics.now()): Pair<String, Int> = store.countUse(now)

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

        private const val DONE_VIBRATE_MILLIS = 120L
    }
}
