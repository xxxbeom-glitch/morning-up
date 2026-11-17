package com.lumian.morningup.core.alarm

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.lumian.morningup.core.alarm.guard.PowerGuardService
import com.lumian.morningup.core.alarm.ring.RingNotifications
import com.lumian.morningup.core.model.AlarmPermission
import com.lumian.morningup.core.model.PermissionChecker
import com.lumian.morningup.core.model.PermissionReport
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 기기에서 알람 권한 상태를 읽어요. 앱이 앞으로 나올 때마다(기기 설정에서 돌아올 때 포함) [refresh]를 불러
 * 배너, 경고 아이콘, 권한 줄이 바로 맞게 바뀌어요(dev-readiness 3번 흐름 표 14).
 * 꺼진 필수 권한이 생기면 진단 기록에 남겨 아침 확인 때 볼 수 있게 해요.
 */
@Singleton
class AndroidPermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exact: ExactAlarms,
    private val notifications: RingNotifications,
    private val diagnostics: AlarmDiagnostics,
) : PermissionChecker {
    private val state = MutableStateFlow(PermissionReport.Unknown)
    override val report: StateFlow<PermissionReport> = state.asStateFlow()

    override fun refresh() {
        val next = PermissionReport(AlarmPermission.entries.associateWith(::read))
        val before = state.value
        state.value = next
        val newlyMissing = next.missingRequired - before.missingRequired.toSet()
        if (newlyMissing.isNotEmpty()) {
            diagnostics.record("permission_missing", null, newlyMissing.joinToString { it.name })
        }
    }

    private fun read(permission: AlarmPermission): Boolean = when (permission) {
        AlarmPermission.ExactAlarm -> exact.canScheduleExact()
        AlarmPermission.Battery ->
            context.getSystemService(PowerManager::class.java)
                ?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        AlarmPermission.DoNotDisturb ->
            context.getSystemService(NotificationManager::class.java)?.isNotificationPolicyAccessGranted ?: false
        AlarmPermission.AlarmVolume ->
            (context.getSystemService(AudioManager::class.java)?.getStreamVolume(AudioManager.STREAM_ALARM) ?: 0) > 0
        AlarmPermission.Notifications -> notificationsOn()
        AlarmPermission.FullScreen -> notifications.canShowFullScreen()
        AlarmPermission.Overlay -> Settings.canDrawOverlays(context)
        AlarmPermission.PowerGuard -> powerGuardOn()
    }

    /** 접근성 설정에서 전원 종료 방지 서비스가 켜져 있는지. */
    private fun powerGuardOn(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        val me = ComponentName(context, PowerGuardService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    /** 앱 알림 전체와 울림 알림 채널이 모두 켜져 있어야 울림 화면을 띄울 수 있어요. */
    private fun notificationsOn(): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        val channel = manager.getNotificationChannel(RingNotifications.CHANNEL_RINGING) ?: return true
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }
}
