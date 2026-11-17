package com.lumian.morningup.feature.settings

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.AlarmClock
import com.composables.icons.lucide.BatteryFull
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.BellOff
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Layers
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Moon
import com.composables.icons.lucide.Shield
import com.composables.icons.lucide.SquareDot
import com.composables.icons.lucide.Volume2
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
import com.lumian.morningup.core.model.PermissionScreen

private val IconCircle = 40.dp
private val IconSize = 20.dp
private val IconTextGap = 14.dp
private val ChevronSize = 16.dp

/**
 * 권한 줄 받침 색. 피그마 T2-A(`388:7368`), T2-B(`388:7393`)에 변수 없이 칠해진 값이라 여기 모아 둬요.
 * 받침 위 아이콘은 흰색이에요.
 */
private object PermissionTint {
    val Battery = Color(0xFF34C17A)
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
private data class RowSpec(val title: Int, val description: Int, val icon: ImageVector, val tint: Color)

private fun spec(permission: AlarmPermission): RowSpec = when (permission) {
    AlarmPermission.ExactAlarm ->
        RowSpec(R.string.permission_exact, R.string.permission_exact_desc, Lucide.AlarmClock, PermissionTint.Exact)
    AlarmPermission.Battery ->
        RowSpec(
            R.string.permission_battery,
            R.string.permission_battery_desc,
            Lucide.BatteryFull,
            PermissionTint.Battery,
        )
    AlarmPermission.DoNotDisturb ->
        RowSpec(R.string.permission_dnd, R.string.permission_dnd_desc, Lucide.BellOff, PermissionTint.DoNotDisturb)
    AlarmPermission.AlarmVolume ->
        RowSpec(R.string.permission_volume, R.string.permission_volume_desc, Lucide.Volume2, PermissionTint.Volume)
    AlarmPermission.Notifications ->
        RowSpec(
            R.string.permission_notifications,
            R.string.permission_notifications_desc,
            Lucide.Bell,
            PermissionTint.Notifications,
        )
    AlarmPermission.FullScreen ->
        RowSpec(
            R.string.permission_full_screen,
            R.string.permission_full_screen_desc,
            Lucide.SquareDot,
            PermissionTint.FullScreen,
        )
    AlarmPermission.Overlay ->
        RowSpec(R.string.permission_overlay, R.string.permission_overlay_desc, Lucide.Layers, PermissionTint.Overlay)
    AlarmPermission.PowerGuard ->
        RowSpec(R.string.permission_power, R.string.permission_power_desc, Lucide.Shield, PermissionTint.PowerGuard)
}

/**
 * T2-A 알람 울림, T2-B 알람 화면 표시. 토글은 지금 기기 상태를 그대로 보여 주고, 줄이나 토글을 누르면
 * 꺼진 권한은 P1~P4 요청 창을 거쳐 기기 설정 화면으로 보내요. 돌아오면 앱이 다시 확인해서 토글이 바로 바뀌어요.
 * 전원 종료 방지(선택 권한)는 접근성 동의 화면과 함께 다음 단위에서 더해요.
 */
@Composable
fun PermissionRoute(
    screen: PermissionScreen,
    onBack: () -> Unit,
    onOpenPowerGuide: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val report by viewModel.report.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val powerAgreed by viewModel.powerGuardAgreed.collectAsStateWithLifecycle()
    var asking by rememberSaveable { mutableStateOf<AlarmPermission?>(null) }
    PermissionPage(
        screen = screen,
        report = report,
        showSamsungSleep = PermissionSettings.isSamsung,
        onBack = onBack,
        // 꺼진 권한은 P1~P4 요청 창을 먼저 띄우고, 켜진 권한은 끄는 곳(기기 설정)으로 바로 보내요.
        onOpen = {
            when {
                report.isGranted(it) -> PermissionSettings.open(context, PermissionSettings.intentFor(context, it))
                // 전원 종료 방지는 P6 동의를 한 번 받고, 그 뒤로는 P7 안내 화면으로 바로 가요.
                it == AlarmPermission.PowerGuard && powerAgreed -> onOpenPowerGuide()
                else -> asking = it
            }
        },
        onOpenSamsungSleep = { PermissionSettings.open(context, PermissionSettings.samsungSleepIntent()) },
        modifier = modifier,
    )
    when (val permission = asking) {
        null -> Unit
        AlarmPermission.PowerGuard -> PowerGuardConsentDialog(
            onDismiss = { asking = null },
            onAgree = {
                viewModel.agreePowerGuard()
                asking = null
                onOpenPowerGuide()
            },
        )
        else -> PermissionAskDialog(permission = permission, onDismiss = { asking = null }, onDone = { asking = null })
    }
}

@Composable
fun PermissionPage(
    screen: PermissionScreen,
    report: PermissionReport,
    showSamsungSleep: Boolean,
    onBack: () -> Unit,
    onOpen: (AlarmPermission) -> Unit,
    onOpenSamsungSleep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = if (screen == PermissionScreen.Ring) R.string.settings_alarm_ring else R.string.settings_alarm_screen
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MorningTheme.colors.backgroundBase)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        MorningTopBar(
            title = stringResource(title),
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
            MorningSettingsCard(title = stringResource(R.string.permission_required)) {
                Column(verticalArrangement = Arrangement.spacedBy(MorningDimens.space24)) {
                    when (screen) {
                        PermissionScreen.Ring -> {
                            // 정확한 알람은 꺼졌을 때만 맨 위에 보여요(알람 시계 앱은 보통 처음부터 켜져 있어요).
                            if (!report.isGranted(AlarmPermission.ExactAlarm)) {
                                PermissionRow(AlarmPermission.ExactAlarm, report, onOpen)
                            }
                            PermissionRow(AlarmPermission.Battery, report, onOpen)
                            if (showSamsungSleep) SamsungSleepRow(onOpenSamsungSleep)
                            PermissionRow(AlarmPermission.DoNotDisturb, report, onOpen)
                            PermissionRow(AlarmPermission.AlarmVolume, report, onOpen)
                        }
                        PermissionScreen.Screen -> {
                            PermissionRow(AlarmPermission.Notifications, report, onOpen)
                            PermissionRow(AlarmPermission.FullScreen, report, onOpen)
                        }
                    }
                }
            }
            MorningSettingsCard(title = stringResource(R.string.permission_optional)) {
                val optional = if (screen ==
                    PermissionScreen.Ring
                ) {
                    AlarmPermission.PowerGuard
                } else {
                    AlarmPermission.Overlay
                }
                PermissionRow(optional, report, onOpen)
            }
        }
    }
}

@Composable
private fun PermissionRow(permission: AlarmPermission, report: PermissionReport, onOpen: (AlarmPermission) -> Unit) {
    val spec = spec(permission)
    val granted = report.isGranted(permission)
    val state = stringResource(if (granted) R.string.permission_granted else R.string.permission_not_granted)
    RowFrame(
        spec = spec,
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

/** 삼성 '절전 예외 앱'은 앱에서 켜졌는지 알 수 없어서 토글 대신 화살표로 기기 화면만 열어요. */
@Composable
private fun SamsungSleepRow(onClick: () -> Unit) {
    val spec = RowSpec(
        R.string.permission_samsung_sleep,
        R.string.permission_samsung_sleep_desc,
        Lucide.Moon,
        PermissionTint.SamsungSleep,
    )
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
            Icon(spec.icon, null, Modifier.size(IconSize), tint = Color.White)
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
