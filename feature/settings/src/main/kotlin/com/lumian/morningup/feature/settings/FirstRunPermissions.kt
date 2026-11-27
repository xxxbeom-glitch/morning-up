package com.lumian.morningup.feature.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.BellRing
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.lumian.morningup.core.alarm.PermissionSettings
import com.lumian.morningup.core.designsystem.component.MorningButton
import com.lumian.morningup.core.designsystem.component.MorningDialogSheet
import com.lumian.morningup.core.designsystem.component.MorningIconButton
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.AlarmPermission
import com.lumian.morningup.core.model.PermissionReport

/**
 * O5 필수 권한 목록의 줄(결정 191). 한 줄이 권한 하나 이상을 맡아요.
 * '알람' 줄은 알림, 전체 화면 알림(안드로이드 14 이상), 정확한 알람(안드로이드 12) 중 꺼진 것을 차례로 열어요.
 */
enum class FirstRunPermissionRow(
    @StringRes val title: Int,
    @StringRes val description: Int,
    /** 아이콘과 바탕색을 빌려 올 설정 '권한' 화면 줄. */
    internal val look: AlarmPermission,
) {
    Alarm(R.string.first_run_perm_alarm, R.string.first_run_perm_alarm_desc, AlarmPermission.Notifications),
    Battery(R.string.first_run_perm_battery, R.string.first_run_perm_battery_desc, AlarmPermission.Battery),
    Overlay(R.string.first_run_perm_overlay, R.string.first_run_perm_overlay_desc, AlarmPermission.Overlay),
    PowerGuard(R.string.first_run_perm_power, R.string.first_run_perm_power_desc, AlarmPermission.PowerGuard),
    ;

    /** 이 줄이 켜는 권한, 여는 순서대로. */
    fun permissions(sdk: Int): List<AlarmPermission> = when (this) {
        Alarm -> buildList {
            add(AlarmPermission.Notifications)
            add(AlarmPermission.FullScreen)
            if (exactAlarmRowVisible(sdk)) add(AlarmPermission.ExactAlarm)
        }
        Battery -> listOf(AlarmPermission.Battery)
        Overlay -> listOf(AlarmPermission.Overlay)
        PowerGuard -> listOf(AlarmPermission.PowerGuard)
    }

    /** 맡은 권한이 모두 켜졌는지. */
    fun isOn(report: PermissionReport, sdk: Int): Boolean = permissions(sdk).all(report::isGranted)

    /** 눌렀을 때 열 권한. 꺼진 것 중 첫째예요. 모두 켜졌으면 null. */
    fun nextToOpen(report: PermissionReport, sdk: Int): AlarmPermission? =
        permissions(sdk).firstOrNull { !report.isGranted(it) }

    companion object {
        /** O5에 보일 줄. 꺼진 줄만이고, 비어 있으면 O5를 건너뛰고 바로 홈이에요. */
        fun missing(report: PermissionReport, sdk: Int = Build.VERSION.SDK_INT): List<FirstRunPermissionRow> =
            entries.filterNot { it.isOn(report, sdk) }
    }
}

/** O4-1 '다음'에서 알림 권한 카드를 띄울지. 안드로이드 13 이상에서 알림 권한이 아직 없을 때만이에요. */
fun firstRunNeedsNotificationCard(context: Context): Boolean = needsRuntimeNotificationAsk(context)

/**
 * O4-1a 알림 권한 카드(바텀 모달). '허용하기'는 시스템 창(두 번 거절했으면 앱 알림 설정)을 열고,
 * '나중에'와 뒤로 가기는 그냥 넘어가요. 어느 쪽이든 [onDone]으로 O4-2에 가요. 거절해도 막지 않아요.
 */
@Composable
fun FirstRunNotificationCard(onDone: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refresh()
        onDone()
    }
    MorningDialogSheet(
        title = stringResource(R.string.first_run_notif_title),
        message = stringResource(R.string.first_run_notif_desc),
        confirmLabel = stringResource(R.string.ask_allow),
        onConfirm = {
            var launched = false
            if (needsRuntimeNotificationAsk(context)) {
                askOrOpen(context, viewModel, Manifest.permission.POST_NOTIFICATIONS, AlarmPermission.Notifications) {
                    launched = true
                    ask.launch(it)
                }
            } else {
                openSettings(context, AlarmPermission.Notifications)
            }
            // 시스템 창이 아니라 앱 알림 설정을 열었으면(두 번 거절) 기다리지 않고 넘어가요. 돌아오면 O4-2예요.
            if (!launched) onDone()
        },
        laterLabel = stringResource(R.string.ask_later),
        onLater = onDone,
        tag = FIRST_RUN_NOTIF_CARD_TAG,
        picture = {
            Icon(
                imageVector = Lucide.BellRing,
                contentDescription = null,
                tint = MorningTheme.colors.primaryDefault,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(NotifIconSize),
            )
        },
    )
}

/**
 * O5 필수 권한 목록(결정 191). 처음 들어올 때 꺼져 있던 줄만 보여 주고, 돌아왔을 때 켜진 줄은 그대로 두고 체크로 바꿔요.
 * '시작하기'와 위 X는 늘 누를 수 있고 [onFinish]로 홈에 가요. 꺼진 줄이 처음부터 없으면 바로 [onFinish]예요.
 * [showAll](첫 실행 확인용 앱)이면 켜진 줄도 네 줄 다 보여요.
 */
@Composable
fun FirstRunPermissionRoute(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    showAll: Boolean = false,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val report by viewModel.report.collectAsStateWithLifecycle()
    val sdk = Build.VERSION.SDK_INT
    // 처음 들어올 때 꺼져 있던 줄. 켜고 돌아와도 줄은 그대로 남겨요.
    val rowNames by rememberSaveable {
        viewModel.refresh()
        val rows = if (showAll) FirstRunPermissionRow.entries else FirstRunPermissionRow.missing(viewModel.report.value)
        mutableStateOf(rows.map { it.name })
    }
    val rows = rowNames.map(FirstRunPermissionRow::valueOf)
    LaunchedEffect(rows.isEmpty()) { if (rows.isEmpty()) onFinish() }
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    var askPower by rememberSaveable { mutableStateOf(false) }
    val open = rememberPermissionOpener(viewModel)
    BackHandler(onBack = onFinish)
    if (rows.isEmpty()) return
    FirstRunPermissionPage(
        rows = rows,
        isOn = { it.isOn(report, sdk) },
        onSet = { row ->
            when (row) {
                FirstRunPermissionRow.PowerGuard -> askPower = true
                else -> row.nextToOpen(viewModel.report.value, sdk)?.let(open)
            }
        },
        onFinish = onFinish,
        modifier = modifier,
    )
    if (askPower) {
        // P6 동의를 먼저 받아요(스토어 정책: 접근성은 쓰는 이유를 보여 주고 동의를 받은 뒤 설정으로).
        PowerGuardSheet(
            onAgree = {
                askPower = false
                viewModel.agreePowerGuard()
                // 켜고 돌아오면 설정의 '전원 종료 방지'도 켜진 상태예요. 안 켜면 설정 토글은 꺼진 모양 그대로예요.
                viewModel.setPowerGuardEnabled(true)
                PermissionSettings.open(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            onLater = { askPower = false },
        )
    }
}

/** O5 화면 그림. 위 오른쪽 X, 제목, 권한 줄, 아래 '시작하기'. */
@Composable
fun FirstRunPermissionPage(
    rows: List<FirstRunPermissionRow>,
    isOn: (FirstRunPermissionRow) -> Boolean,
    onSet: (FirstRunPermissionRow) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MorningTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundBase)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = TopRowHeight),
        ) {
            MorningIconButton(
                icon = Lucide.X,
                contentDescription = stringResource(R.string.first_run_close),
                onClick = onFinish,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = MorningDimens.space4),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MorningDimens.layoutScreenMargin),
        ) {
            Text(
                text = stringResource(R.string.first_run_perm_title),
                style = MorningTheme.typography.headlineLarge,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = MorningDimens.space8, bottom = MorningDimens.space32),
            )
            Column(verticalArrangement = Arrangement.spacedBy(MorningDimens.space8)) {
                rows.forEach { row -> FirstRunPermissionRowItem(row, on = isOn(row), onClick = { onSet(row) }) }
            }
        }
        MorningButton(
            text = stringResource(R.string.first_run_start),
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = MorningDimens.space16,
                    end = MorningDimens.space16,
                    top = MorningDimens.space16,
                    bottom = MorningDimens.space8,
                ),
        )
    }
}

/** 권한 한 줄: 원 40 아이콘, 이름과 결과 한 줄, 오른쪽 '설정하기'(켜졌으면 체크). */
@Composable
private fun FirstRunPermissionRowItem(row: FirstRunPermissionRow, on: Boolean, onClick: () -> Unit) {
    val colors = MorningTheme.colors
    val look = spec(row.look)
    val state = stringResource(if (on) R.string.first_run_perm_done else R.string.permission_not_granted)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("$FIRST_RUN_PERM_ROW_TAG${row.name}")
            .background(colors.backgroundElevated, RoundedCornerShape(MorningDimens.radiusTile))
            .morningClickable(enabled = !on, role = Role.Button, onClick = onClick)
            .semantics { stateDescription = state }
            .padding(MorningDimens.space16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MorningDimens.space12),
    ) {
        Box(
            modifier = Modifier
                .size(RowIconCircle)
                .background(look.tint, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            when (val icon = look.icon) {
                is RowIcon.Drawn -> Image(painterResource(icon.res), contentDescription = null)
                is RowIcon.Line -> Icon(icon.vector, null, Modifier.size(RowIconSize), tint = Color.White)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(row.title),
                style = MorningTheme.typography.bodyMediumStrong,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(row.description),
                style = MorningTheme.typography.captionMedium,
                color = colors.textSecondary,
            )
        }
        if (on) {
            Icon(
                imageVector = Lucide.Check,
                contentDescription = null,
                tint = colors.primaryDefault,
                modifier = Modifier.size(CheckSize),
            )
        } else {
            // 버튼 모양(Soft 칠)이지만 줄 전체가 눌려서 따로 누름을 받지 않아요.
            Text(
                text = stringResource(R.string.first_run_perm_set),
                style = MorningTheme.typography.bodySmallStrong,
                color = colors.primaryText,
                modifier = Modifier
                    .background(colors.primarySubtle, RoundedCornerShape(MorningDimens.radiusButton))
                    .padding(horizontal = MorningDimens.space12, vertical = MorningDimens.space8),
            )
        }
    }
}

const val FIRST_RUN_NOTIF_CARD_TAG = "firstRunNotifCard"
const val FIRST_RUN_PERM_ROW_TAG = "firstRunPermRow"

private val TopRowHeight = 56.dp
private val RowIconCircle = 40.dp
private val RowIconSize = 20.dp
private val CheckSize = 24.dp
private val NotifIconSize = 48.dp
