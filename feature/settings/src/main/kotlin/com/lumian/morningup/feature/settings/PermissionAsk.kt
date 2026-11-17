package com.lumian.morningup.feature.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.lumian.morningup.core.alarm.PermissionSettings
import com.lumian.morningup.core.designsystem.component.MorningDialog
import com.lumian.morningup.core.model.AlarmPermission
import com.lumian.morningup.core.model.PermissionReport

/** 권한 요청 창의 제목, 본문, 확인 버튼 글. 제목은 T2 줄 이름과 같은 말을 써요(design-system 공용 Dialog). */
private data class AskTexts(val title: Int, val message: Int, val confirm: Int = R.string.ask_allow)

private fun askTexts(permission: AlarmPermission): AskTexts = when (permission) {
    AlarmPermission.Notifications -> AskTexts(R.string.ask_notifications, R.string.ask_notifications_msg)
    AlarmPermission.ExactAlarm -> AskTexts(R.string.ask_exact, R.string.ask_exact_msg)
    AlarmPermission.Battery -> AskTexts(R.string.ask_battery, R.string.ask_battery_msg)
    AlarmPermission.FullScreen -> AskTexts(R.string.ask_full_screen, R.string.ask_full_screen_msg)
    AlarmPermission.Overlay -> AskTexts(R.string.ask_overlay, R.string.ask_overlay_msg)
    AlarmPermission.DoNotDisturb -> AskTexts(R.string.ask_dnd, R.string.ask_dnd_msg)
    AlarmPermission.AlarmVolume -> AskTexts(R.string.ask_volume, R.string.ask_volume_msg, R.string.ask_volume_confirm)
    AlarmPermission.PowerGuard ->
        AskTexts(R.string.power_consent_title, R.string.power_consent_msg, R.string.power_consent_agree)
}

/** 안드로이드 13 이상에서 알림 권한 자체(채널이 아니라)가 아직 없는지. 이때는 시스템 요청 창으로 먼저 물어요. */
private fun needsRuntimeNotificationAsk(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

/**
 * P1~P4(그리고 같은 모양의 다른 앱 위에 표시, 방해 금지, 알람 음량) 권한 요청 창.
 * '허용하기'를 누르면 알림은 시스템 요청 창으로 먼저 묻고, 거절되면(두 번 거절해 시스템이 더 묻지 않는 경우 포함)
 * 앱 알림 설정 화면으로 보내요. 나머지는 바로 그 권한의 기기 설정 화면으로 보내요.
 * 돌아오면 앱이 다시 확인해서 토글, 배너, 말풍선이 바뀌어요.
 */
@Composable
fun PermissionAskDialog(permission: AlarmPermission, onDismiss: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val texts = askTexts(permission)
    val openSettings = { PermissionSettings.open(context, PermissionSettings.intentFor(context, permission)) }
    val notificationAsk = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) openSettings()
        onDone()
    }
    MorningDialog(
        title = stringResource(texts.title),
        message = stringResource(texts.message),
        dismissLabel = stringResource(R.string.ask_later),
        confirmLabel = stringResource(texts.confirm),
        onDismiss = onDismiss,
        onConfirm = {
            if (permission == AlarmPermission.Notifications && needsRuntimeNotificationAsk(context)) {
                notificationAsk.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openSettings()
                onDone()
            }
        },
    )
}

/** 첫 알람을 저장할 때 묻는 순서(F21): 알림, 전체 화면 알림, 배터리. 정확한 알람은 알람 앱이라 보통 켜져 있어요. */
val FirstSaveAskOrder = listOf(AlarmPermission.Notifications, AlarmPermission.FullScreen, AlarmPermission.Battery)

/**
 * 첫 알람 저장 뒤 한 번만 도는 권한 묻기. 순서대로 꺼진 것만 하나씩 창을 띄우고, '나중에'를 눌러도 다음으로 넘어가요.
 * 기기 설정에 다녀오면 다음 창이 이어서 떠요. 다 돌면 [onFinished]를 불러 다시 묻지 않게 해요.
 */
@Composable
fun FirstSavePermissionAsk(report: PermissionReport, onFinished: () -> Unit) {
    var next by rememberSaveable { mutableIntStateOf(0) }
    val index = (next until FirstSaveAskOrder.size).firstOrNull { !report.isGranted(FirstSaveAskOrder[it]) }
    if (index == null) {
        LaunchedEffect(Unit) { onFinished() }
        return
    }
    PermissionAskDialog(
        permission = FirstSaveAskOrder[index],
        onDismiss = { next = index + 1 },
        onDone = { next = index + 1 },
    )
}
