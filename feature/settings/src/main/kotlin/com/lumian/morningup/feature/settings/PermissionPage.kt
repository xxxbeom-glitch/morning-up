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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.heading
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
import com.lumian.morningup.core.designsystem.component.MorningToggle
import com.lumian.morningup.core.designsystem.component.MorningToggleSize
import com.lumian.morningup.core.designsystem.component.MorningTopBar
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningShadows
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.designsystem.theme.morningShadow
import com.lumian.morningup.core.model.AlarmPermission
import com.lumian.morningup.core.model.PermissionReport

private val IconCircle = 40.dp
private val IconSize = 20.dp
private val IconTextGap = 12.dp

/** 피그마 T2-A(388:7368) 줄 높이 64(줄 사이 0), 카드 제목 칸 32(제목 24 + 아래 8), 마지막 카드 아래 40. */
private val RowHeight = 64.dp
private val CardTitleBottom = 8.dp
private val PageBottom = 40.dp
private val ChevronSize = 16.dp

/**
 * 권한 줄 받침 색. 피그마 '권한' 화면(구역 `781:18261`)에 변수 없이 칠해진 값이라 여기 모아 둬요.
 * 받침 위 아이콘은 흰색이에요.
 */
internal object PermissionTint {
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
internal data class RowSpec(val title: Int, val description: Int, val icon: RowIcon, val tint: Color)

/**
 * 줄 아이콘. 화면에 보이는 줄은 피그마 T2-A(388:7368)의 채움 모양을 옮긴 그림([Drawn], 16, 방해 금지만 18.3)이고,
 * '권한' 화면에 줄이 없는 정확한 알람, 전원 종료 방지만 Lucide 선 아이콘([Line], 20)이에요.
 */
internal sealed interface RowIcon {
    data class Drawn(@DrawableRes val res: Int) : RowIcon

    data class Line(val vector: ImageVector) : RowIcon
}

internal fun spec(permission: AlarmPermission): RowSpec = when (permission) {
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
fun PermissionRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenDndGuide: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val report by viewModel.report.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val open = rememberPermissionOpener(viewModel)
    PermissionPage(
        report = report,
        showSamsungSleep = PermissionSettings.isSamsung,
        showExactAlarm = exactAlarmRowVisible(Build.VERSION.SDK_INT),
        onBack = onBack,
        // '방해 금지 모드 예외' 줄은 T2-1 안내 화면으로 가요(permission-matrix R2, 결정 표 4번).
        onOpen = { if (it == AlarmPermission.DoNotDisturb && onOpenDndGuide != null) onOpenDndGuide() else open(it) },
        onOpenSamsungSleep = { PermissionSettings.open(context, PermissionSettings.samsungSleepIntent()) },
        modifier = modifier,
    )
}

/**
 * 권한 줄을 눌렀을 때 그 권한의 폰 설정이나 시스템 창을 여는 함수(결정 126 줄별 이동 표). '권한' 화면과 PL2 시트가 같이 써요.
 * 알림(안드로이드 13 이상에서 권한 자체가 없을 때), 위치는 시스템 요청 창을 먼저 띄우고, 배터리는 꺼져 있으면 시스템 확인 창이에요.
 * 아무 화면도 열지 못하면 false를 돌려줘요.
 */
@Composable
internal fun rememberPermissionOpener(viewModel: SettingsViewModel): (AlarmPermission) -> Boolean {
    val context = LocalContext.current
    val notificationAsk = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refresh()
    }
    val locationAsk = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refresh()
    }
    return { permission ->
        val granted = viewModel.report.value.isGranted(permission)
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
    }
}

/**
 * 그 권한의 폰 설정을 열어요. 앱 목록으로 열리는 화면이면 떠날 때 '목록에서 일어나를 찾아 켜 주세요'를 띄워요.
 * 열지 못하면 false예요.
 */
internal fun openSettings(context: Context, permission: AlarmPermission): Boolean {
    val opened = PermissionSettings.open(context, PermissionSettings.intentFor(context, permission))
    if (opened && PermissionSettings.opensList(permission)) {
        // 폰 설정 화면 위에서도 보이도록 시스템 토스트로 띄워요(앱 안 스낵바는 앱을 떠나면 안 보여요).
        Toast.makeText(context, R.string.permission_find_in_list, Toast.LENGTH_LONG).show()
    }
    return opened
}

/**
 * 시스템 요청 창으로 물어요. 이미 물었고 시스템이 더 묻지 않는 상태(두 번 거절)면 창 대신 폰 설정으로 보내요.
 * 창을 띄웠거나 설정을 열었으면 true예요.
 */
internal fun askOrOpen(
    context: Context,
    viewModel: SettingsViewModel,
    manifestPermission: String,
    permission: AlarmPermission,
    launch: (String) -> Unit,
): Boolean {
    val activity = context.findActivity()
    val systemWillAsk = activity == null ||
        !viewModel.wasAsked(permission) ||
        ActivityCompat.shouldShowRequestPermissionRationale(activity, manifestPermission)
    return if (systemWillAsk) {
        viewModel.markAsked(permission)
        launch(manifestPermission)
        true
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
 * '기타'(방해 금지 모드 예외, 알람 음량 화살표, 절전 예외 앱(삼성만) 화살표).
 * [showExactAlarm]이면 필수 카드 끝에 '정확한 알람' 줄을 더해요(안드로이드 12만, TSK-177 답 8). 13 이상은 설치 때 받아 줄이 없어요(결정 133).
 */
@Composable
fun PermissionPage(
    report: PermissionReport,
    showSamsungSleep: Boolean,
    onBack: () -> Unit,
    onOpen: (AlarmPermission) -> Unit,
    onOpenSamsungSleep: () -> Unit,
    modifier: Modifier = Modifier,
    showExactAlarm: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MorningTheme.colors.backgroundBase)
            .statusBarsPadding(),
    ) {
        // 제목 줄은 위에 고정하고, 카드만 그 아래에서 스크롤돼요.
        MorningTopBar(
            title = stringResource(R.string.permission_title),
            onBack = onBack,
            backContentDescription = stringResource(R.string.settings_back),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                // 끝까지 내렸을 때 마지막 카드 아래 40이 시스템 내비게이션 바 위로 보이게 해요.
                .navigationBarsPadding()
                .padding(
                start = MorningDimens.layoutScreenMargin,
                end = MorningDimens.layoutScreenMargin,
                top = MorningDimens.space16,
                bottom = PageBottom,
            ),
            verticalArrangement = Arrangement.spacedBy(MorningDimens.layoutCardGap),
        ) {
            PermissionCard(R.string.permission_required) {
                ToggleRow(AlarmPermission.Notifications, report, onOpen)
                ToggleRow(AlarmPermission.FullScreen, report, onOpen)
                ToggleRow(AlarmPermission.Overlay, report, onOpen)
                if (showExactAlarm) ToggleRow(AlarmPermission.ExactAlarm, report, onOpen)
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

/** 피그마 card/필수 권한 등: 흰 카드, 위 24, 아래 16, 양옆 24, 제목 칸 32, 줄(64) 사이 0. */
@Composable
private fun PermissionCard(title: Int, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(MorningDimens.radiusCard)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .morningShadow(MorningShadows.card, shape)
            .background(MorningTheme.colors.surfaceCard, shape)
            .padding(
                start = MorningDimens.layoutCardPadding,
                end = MorningDimens.layoutCardPadding,
                top = MorningDimens.layoutCardPadding,
                bottom = MorningDimens.space16,
            ),
    ) {
        Text(
            text = stringResource(title),
            style = MorningTheme.typography.titleMedium,
            color = MorningTheme.colors.textPrimary,
            modifier = Modifier
                .padding(bottom = CardTitleBottom)
                .semantics { heading() },
        )
        content()
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
            .heightIn(min = RowHeight)
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

/** '정확한 알람' 줄을 보일지. 사용자가 끌 수 있는 안드로이드 12(API 31, 32)에서만 보여요(TSK-177 답 8). */
internal fun exactAlarmRowVisible(sdk: Int): Boolean =
    sdk == Build.VERSION_CODES.S || sdk == Build.VERSION_CODES.S_V2
