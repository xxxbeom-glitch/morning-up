package com.lumian.morningup.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import com.lumian.morningup.core.model.AlarmPermission
import timber.log.Timber

/**
 * 권한 줄을 누르면 여는 기기 설정 화면. 기기마다 없는 화면이 있어서, 열리지 않으면 앱 정보 화면으로 대신 보내요.
 * 배터리는 직접 예외 요청 창을 쓰지 않고 기기 설정으로 보내요(Play 정책, dev-plan 6단계).
 */
internal object PermissionSettings {
    fun intentFor(context: Context, permission: AlarmPermission): Intent {
        val pkg = "package:${context.packageName}".toUri()
        return when (permission) {
            AlarmPermission.ExactAlarm ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg)
                } else {
                    appDetails(context)
                }
            AlarmPermission.Battery -> Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            AlarmPermission.DoNotDisturb -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            AlarmPermission.AlarmVolume -> Intent(Settings.Panel.ACTION_VOLUME)
            AlarmPermission.Notifications ->
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            AlarmPermission.FullScreen ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg)
                } else {
                    appDetails(context)
                }
            AlarmPermission.Overlay -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkg)
        }
    }

    /** 삼성 '절전 예외 앱' 목록이 있는 기기 배터리 화면. 없으면 앱 정보 화면이에요. */
    fun samsungSleepIntent(): Intent = Intent().setClassName(
        "com.samsung.android.lool",
        "com.samsung.android.sm.battery.ui.BatteryActivity",
    )

    val isSamsung: Boolean get() = Build.MANUFACTURER.equals("samsung", ignoreCase = true)

    fun open(context: Context, intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Timber.w(e, "settings screen missing: %s", intent.action)
            context.startActivity(appDetails(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: SecurityException) {
            Timber.w(e, "settings screen blocked: %s", intent.action)
            context.startActivity(appDetails(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun appDetails(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
}
