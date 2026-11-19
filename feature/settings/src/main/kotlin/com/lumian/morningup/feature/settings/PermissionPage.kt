package com.lumian.morningup.feature.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.AlarmClock
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Shield
import com.lumian.morningup.core.alarm.PermissionSettings
import com.lumian.morningup.core.designsystem.component.MorningSettingsCard
import com.lumian.morningup.core.designsystem.component.MorningToggle
import com.lumian.morningup.core.designsystem.component.MorningToggleSize
import com.lumian.morningup.core.designsystem.component.MorningTopBar
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.AlarmPermission
import com.lumian.morningup.core.model.PermissionReport

private val IconCircle = 40.dp
private val IconSize = 20.dp
private val IconTextGap = 14.dp
private val ChevronSize = 16.dp

/**
 * 권한 줄 받침 색. 피그마 '권한' 화면(구역 `781:18261`)에 변수 없이 칠해진 값이라 여기 모아 둬요.
 * 받침 위 아이콘은 흰색이에요.
 */
private object PermissionTint {
    val Battery = Color(0xFF34C17A)
    val Location = Color(0xFFF2884B)
    val SamsungSleep = Color(0xFFF5A524)
    val DoNotDisturb = Color(0xFF9B7BEA)
    val Volume = Color(0xFFEE6FA6)
    val Exact = Color(0xFF0B6CF0)
    val Notifications = Color(0xFFF2675A)
    val FullScreen = Color(0xFF6D7CF2)
    val Overlay = Color(0xFF4C8DF0)
    val PowerGuard = Color(0xFF2BB3C0)
}

/** 권한 줄 하나의 모양. */
private data class RowSpec(val title: Int, val description: Int, val icon: RowIcon, val tint: Color)

/**
 * 줄 아이콘. 화면에 보이는 줄은 피그마 T2-A(388:7368)의 채움 모양을 옮긴 그림([Drawn], 16, 방해 금지만 18.3)이고,
 * '권한' 화면에 줄이 없는 정확한 알람, 전원 종료 방지만 Lucide 선 아이콘([Line], 20)이에요.
 */
private sealed interface RowIcon {
    data class Drawn(@DrawableRes val res: Int) : RowIcon

    data class Line(val vector: ImageVector) : RowIcon
}

private fun spec(permission: AlarmPermission): RowSpec = when (permission) {
    AlarmPermission.Notifications ->
        RowSpec(
            R.string.permission_notifications,
            R.string.permission_notifications_desc,
            RowIcon.Drawn(R.drawable.ic_permission_notifications),
            PermissionTint.Notifications,
        )
    AlarmPermission.FullScreen ->
        RowSpec(
            R.string.permission_full_screen,
            R.string.permission_full_screen_desc,
            RowIcon.Drawn(R.drawable.ic_permission_full_screen),
            PermissionTint.FullScreen,
        )
    AlarmPermission.Overlay ->
        RowSpec(
            R.string.permission_overlay,
            R.string.permission_overlay_desc,
            RowIcon.Drawn(R.drawable.ic_permission_overlay),
            PermissionTint.Overlay,
        )
    AlarmPermission.Battery ->
        RowSpec(
            R.string.permission_battery,
            R.string.permission_battery_desc,
            RowIcon.Drawn(R.drawable.ic_permission_battery),
            PermissionTint.Battery,
        )
    AlarmPermission.Location ->
        RowSpec(
            R.string.permission_location,
            R.string.permission_location_desc,
            RowIcon.Drawn(R.drawable.ic_permission_location),
            PermissionTint.Location,
        )
    AlarmPermission.DoNotDisturb ->
        RowSpec(
            R.string.permission_dnd,
            R.string.permission_dnd_desc,
            RowIcon.Drawn(R.drawable.ic_permission_dnd),
            PermissionTint.DoNotDisturb,
        )
    AlarmPermission.AlarmVolume ->
        RowSpec(
            R.string.permission_volume,
            R.string.permission_volume_desc,
            RowIcon.Drawn(R.drawable.ic_permission_volume),
            PermissionTint.Volume,
        )
    AlarmPermission.ExactAlarm ->
        RowSpec(
            R.string.permission_exact,
            R.string.permission_exact_desc,
            RowIcon.Line(Lucide.AlarmClock),
            PermissionTint.Exact,
        )
    AlarmPermission.PowerGuard ->
        RowSpec(
            R.string.permission_power,
            R.string.permission_power_desc,
            RowIcon.Line(Lucide.Shield),
            PermissionTint.PowerGuard,
        )
}

/**
 * 설정 '권한' 한 화면(결정 126, 130 보충). 줄을 누르면 안내 창 없이 바로 폰 설정이나 시스템 창으로 가요.
 * 알림, 위치는 시스템 요청 창을 먼저 띄우고, 두 번 거절해 시스템이 더 묻지 않으면 알림은 앱 알림 설정, 위치는 앱 정보로 보내요.
 * 배터리는 꺼져 있으면 시스템 확인 창, 켜져 있으면 배터리 최적화 목록이에요. 목록 화면으로 열리는 줄은 떠날 때 안내를 띄워요.
 * 돌아오면 앱이 다시 확인해서(MainActivity.onResume) 토글이 실제 상태로 바뀌어요.
 */
@Composable
fun PermissionRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val report by viewModel.report.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationAsk = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refresh()
    }
    val locationAsk = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refresh()
    }
    PermissionPage(
        report = report,
        showSamsungSleep = PermissionSettings.isSamsung,
        onBack = onBack,
        onOpen = { permission ->
            val granted = report.isGranted(permission)
            when {
                permission == AlarmPermission.Notifications && !granted && needsRuntimeNotificationAsk(context) ->
                    askOrOpen(context, viewModel, Manifest.permission.POST_NOTIFICATIONS, permission) {
                        notificationAsk.launch(it)
                    }
                permission == AlarmPermission.Location && !granted ->
                    askOrOpen(context, viewModel, Manifest.permission.ACCESS_COARSE_LOCATION, permission) {
                        locationAsk.launch(it)
                    }
                permission == AlarmPermission.Battery && !granted ->
                    PermissionSettings.open(context, PermissionSettings.batteryRequestIntent(context))
                else -> openSettings(context, permission)
            }
        },
        onOpenSamsungSleep = { PermissionSettings.open(context, PermissionSettings.samsungSleepIntent()) },
        modifier = modifier,
    )
}

/** 그 권한의 폰 설정을 열어요. 앱 목록으로 열리는 화면이면 떠날 때 '목록에서 morning-up을 찾아 켜 주세요'를 띄워요. */
internal fun openSettings(context: Context, permission: AlarmPermission) {
    if (PermissionSettings.opensList(permission)) {
        // 폰 설정 화면 위에서도 보이도록 시스템 토스트로 띄워요(앱 안 스낵바는 앱을 떠나면 안 보여요).
        Toast.makeText(context, R.string.permission_find_in_list, Toast.LENGTH_LONG).show()
    }
    PermissionSettings.open(context, PermissionSettings.intentFor(context, permission))
}

/** 시스템 요청 창으로 물어요. 이미 물었고 시스템이 더 묻지 않는 상태(두 번 거절)면 창 대신 폰 설정으로 보내요. */
internal fun askOrOpen(
    context: Context,
    viewModel: SettingsViewModel,
    manifestPermission: String,
    permission: AlarmPermission,
    launch: (String) -> Unit,
) {
    val activity = context.findActivity()
    val systemWillAsk = activity == null ||
        !viewModel.wasAsked(permission) ||
        ActivityCompat.shouldShowRequestPermissionRationale(activity, manifestPermission)
    if (systemWillAsk) {
        viewModel.markAsked(permission)
        launch(manifestPermission)
    } else {
        openSettings(context, permission)
    }
}

/** 안드로이드 13 이상에서 알림 권한 자체(채널이 아니라)가 아직 없는지. 이때는 시스템 요청 창으로 먼저 물어요. */
internal fun needsRuntimeNotificationAsk(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * '권한' 화면 카드 셋: '필수 권한'(알림, 전체 화면 알림, 다른 앱 위에 표시), '선택 권한'(배터리, 위치),
 * '기타'(방해 금지 모드 예외, 알람 음량 화살표, 절전 예외 앱(삼성만) 화살표). 정확한 알람 줄은 없어요(결정 133).
 */
@Composable
fun PermissionPage(
    report: PermissionReport,
    showSamsungSleep: Boolean,
    onBack: () -> Unit,
    onOpen: (AlarmPermission) -> Unit,
    onOpenSamsungSleep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MorningTheme.colors.backgroundBase)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        MorningTopBar(
            title = stringResource(R.string.permission_title),
            onBack = onBack,
            backContentDescription = stringResource(R.string.settings_back),
        )
        Column(
            modifier = Modifier.padding(
                start = MorningDimens.layoutScreenMargin,
                end = MorningDimens.layoutScreenMargin,
                top = MorningDimens.space16,
                bottom = MorningDimens.space32,
            ),
            verticalArrangement = Arrangement.spacedBy(MorningDimens.layoutCardGap),
        ) {
            PermissionCard(R.string.permission_required) {
                ToggleRow(AlarmPermission.Notifications, report, onOpen)
                ToggleRow(AlarmPermission.FullScreen, report, onOpen)
                ToggleRow(AlarmPermission.Overlay, report, onOpen)
            }
            PermissionCard(R.string.permission_optional) {
                ToggleRow(AlarmPermission.Battery, report, onOpen)
                ToggleRow(AlarmPermission.Location, report, onOpen)
            }
            PermissionCard(R.string.permission_other) {
                ToggleRow(AlarmPermission.DoNotDisturb, report, onOpen)
                // 알람 음량은 켜짐, 꺼짐이 없는 기기 볼륨이라 화살표로 두고 줄 전체를 누르면 소리 설정으로 가요(결정 126).
                ArrowRow(spec(AlarmPermission.AlarmVolume)) { onOpen(AlarmPermission.AlarmVolume) }
                if (showSamsungSleep) {
                    ArrowRow(
                        RowSpec(
                            R.string.permission_samsung_sleep,
                            R.string.permission_samsung_sleep_desc,
                            RowIcon.Drawn(R.drawable.ic_permission_samsung_sleep),
                            PermissionTint.SamsungSleep,
                        ),
                        onOpenSamsungSleep,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(title: Int, content: @Composable () -> Unit) {
    MorningSettingsCard(title = stringResource(title)) {
        Column(verticalArrangement = Arrangement.spacedBy(MorningDimens.space24)) { content() }
    }
}

@Composable
private fun ToggleRow(permission: AlarmPermission, report: PermissionReport, onOpen: (AlarmPermission) -> Unit) {
    val granted = report.isGranted(permission)
    val state = stringResource(if (granted) R.string.permission_granted else R.string.permission_not_granted)
    RowFrame(
        spec = spec(permission),
        onClick = { onOpen(permission) },
        modifier = Modifier.semantics { stateDescription = state },
    ) {
        MorningToggle(
            checked = granted,
            onCheckedChange = { onOpen(permission) },
            size = MorningToggleSize.Small,
        )
    }
}

/** 상태가 없는 줄(알람 음량, 삼성 '절전 예외 앱'). 토글 대신 화살표로 기기 화면만 열어요. */
@Composable
private fun ArrowRow(spec: RowSpec, onClick: () -> Unit) {
    RowFrame(spec = spec, onClick = onClick) {
        Icon(Lucide.ChevronRight, null, Modifier.size(ChevronSize), tint = MorningTheme.colors.textTertiary)
    }
}

@Composable
private fun RowFrame(
    spec: RowSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    val colors = MorningTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .morningClickable(haptic = null, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(IconTextGap),
    ) {
        Box(
            modifier = Modifier
                .size(IconCircle)
                .background(spec.tint, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            when (val icon = spec.icon) {
                is RowIcon.Drawn -> Image(painterResource(icon.res), contentDescription = null)
                is RowIcon.Line -> Icon(icon.vector, null, Modifier.size(IconSize), tint = Color.White)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(spec.title),
                style = MorningTheme.typography.bodyMediumStrong,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(spec.description),
                style = MorningTheme.typography.captionMedium,
                color = colors.textSecondary,
            )
        }
        trailing()
    }
}
