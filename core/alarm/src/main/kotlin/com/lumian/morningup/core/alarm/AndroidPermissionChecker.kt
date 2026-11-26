package com.lumian.morningup.core.alarm

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lumian.morningup.core.alarm.guard.PowerGuardService
import com.lumian.morningup.core.alarm.ring.RingNotifications
import com.lumian.morningup.core.model.AlarmPermission
import com.lumian.morningup.core.model.AlarmSilence
import com.lumian.morningup.core.model.DndMode
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
 * 배터리 '제한'과 방해 금지의 알람 막힘(결정 143, TSK-177 C-13, R2)도 같이 읽어요.
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
        val next = PermissionReport(
            granted = AlarmPermission.entries.associateWith(::read),
            backgroundRestricted = backgroundRestricted(),
            alarmsSilenced = alarmsSilenced(),
        )
        val before = state.value
        state.value = next
        val newlyMissing = next.missingRequired - before.missingRequired.toSet()
        if (newlyMissing.isNotEmpty()) {
            diagnostics.record("permission_missing", null, newlyMissing.joinToString { it.name })
        }
        if (next.backgroundRestricted && !before.backgroundRestricted) diagnostics.record(EVENT_RESTRICTED)
        if (next.alarmsSilenced && !before.alarmsSilenced) diagnostics.record(EVENT_SILENCED)
    }

    /** 앱 배터리 사용이 '제한'(안드로이드 9부터 읽을 수 있어요). */
    private fun backgroundRestricted(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
        context.getSystemService(ActivityManager::class.java)?.isBackgroundRestricted == true

    /** 지금 방해 금지가 알람 소리를 막는지. 갤럭시 모드도 합쳐진 정책으로 봐요(TSK-214). */
    private fun alarmsSilenced(): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        val mode = when (manager.currentInterruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_ALL -> DndMode.Off
            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> DndMode.Priority
            NotificationManager.INTERRUPTION_FILTER_ALARMS -> DndMode.AlarmsOnly
            NotificationManager.INTERRUPTION_FILTER_NONE -> DndMode.Total
            else -> DndMode.Unknown
        }
        val policy = if (mode == DndMode.Priority) {
            // 합친 정책을 못 읽으면 기본 정책으로 봐요.
            val consolidated = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    manager.consolidatedNotificationPolicy
                } else {
                    null
                }
            }.getOrNull()
            consolidated ?: runCatching { manager.notificationPolicy }.getOrNull()
        } else {
            null
        }
        val alarmsAllowed = policy == null ||
            policy.priorityCategories and NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS != 0
        return AlarmSilence.blocks(mode, alarmsAllowed)
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
        AlarmPermission.Location ->
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
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

    private companion object {
        const val EVENT_RESTRICTED = "battery_restricted"
        const val EVENT_SILENCED = "dnd_blocks_alarms"
    }
}
