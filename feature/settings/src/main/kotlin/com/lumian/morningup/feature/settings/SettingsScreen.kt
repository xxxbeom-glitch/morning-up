package com.lumian.morningup.feature.settings

import com.lumian.morningup.core.designsystem.theme.ThemeMode
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.lumian.morningup.core.alarm.PermissionSettings
import com.lumian.morningup.core.designsystem.component.MorningMessageHost
import com.lumian.morningup.core.designsystem.component.MorningSettingRow
import com.lumian.morningup.core.designsystem.component.MorningSettingsCard
import com.lumian.morningup.core.designsystem.component.SettingRowState
import com.lumian.morningup.core.designsystem.component.MorningToast
import com.lumian.morningup.core.designsystem.component.MorningTopBar
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.AlarmPermission
import com.lumian.morningup.core.model.PermissionReport
import java.time.LocalTime

/** 하단 메뉴가 덮는 높이. 마지막 카드와 메뉴 사이 24를 더해요(design-system T1). */
private val BottomNavSpace = 120.dp
private val ChevronSize = 16.dp
private val NavRowGap = 5.dp

/** 피그마 T1 두 줄짜리 Setting Row 높이(라벨과 설명, 위아래 7). 편집 화면 줄은 그대로 둬요. */
internal val SettingsToggleRowHeight = 54.dp

/** 의견 보내기 받는 주소(design-system T1, 11-16 사용자 결정). */
private const val FEEDBACK_EMAIL = "lumianthecompany@gmail.com"

/**
 * T1 설정(피그마 `141:1293`). 카드: 알람, 기상 미션(전원 종료 방지), 날씨 애니메이션, 도움말, 앱 정보, 실험실. 제목은 숨겨요.
 * [weatherCard]는 알람 카드 아래 '날씨 애니메이션' 카드 자리예요(9단계).
 * [themeMode]가 있으면(디버그 빌드만) 맨 아래에 '화면 모드(임시)' 줄을 붙여요. 시험 알람은 adb 명령으로 걸어요(docs/qa/README.md).
 */
@Composable
fun SettingsRoute(
    versionName: String,
    onOpenPermissions: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenFaq: () -> Unit = {},
    onOpenLicenses: () -> Unit = {},
    themeMode: ThemeMode? = null,
    onThemeMode: (ThemeMode) -> Unit = {},
    weatherCard: @Composable () -> Unit = { WeatherCard() },
    onOpenLab: () -> Unit = {},
    onOpenDndGuide: () -> Unit = {},
    labCard: @Composable () -> Unit = { LabCard(onOpen = onOpenLab) },
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val report by viewModel.report.collectAsStateWithLifecycle()
    val tryAt by viewModel.tryAlarmAt.collectAsStateWithLifecycle()
    val powerOn by viewModel.powerGuardEnabled.collectAsStateWithLifecycle()
    val dndOn by viewModel.dndOverrideEnabled.collectAsStateWithLifecycle()
    val nextShown by viewModel.nextAlarmShown.collectAsStateWithLifecycle()
    var askPowerConsent by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    SettingsScreen(
        report = report,
        versionName = versionName,
        tryAlarmAt = tryAt,
        onOpenPermissions = onOpenPermissions,
        onTryAlarm = viewModel::tryAlarm,
        onFeedback = { sendFeedback(context, versionName, report) },
        onFaq = onOpenFaq,
        onLicenses = onOpenLicenses,
        modifier = modifier,
        themeMode = themeMode,
        onThemeMode = onThemeMode,
        weatherCard = weatherCard,
        labCard = labCard,
        // 접근성 서비스가 켜져 있을 때만 켜진 모양이에요. 꺼져 있으면 P6 바텀시트를 띄워요(결정 134).
        // 켤 때 값은 먼저 켬으로 둬서, 서비스를 켜고 돌아오면 토글이 켜진 채로 보이고, 안 켜고 돌아오면 꺼진 채예요.
        powerGuard = powerOn && report.isGranted(AlarmPermission.PowerGuard),
        onPowerGuard = { on ->
            viewModel.setPowerGuardEnabled(on)
            if (on && !report.isGranted(AlarmPermission.PowerGuard)) {
                askPowerConsent = true
            }
        },
        // 방해 금지 접근 권한이 있을 때만 켜진 모양이에요. 없으면 켤 때 그 권한 화면으로 보내요(TSK-177 답 3).
        nextAlarmShown = nextShown,
        onNextAlarmShown = viewModel::setNextAlarmShown,
        // 안드로이드 15부터는 앱이 사용자 방해 금지를 풀 수 없어서 켜지 않고 '알람 허용' 안내(T2-1)로 보내요(ISS-057).
        dndOverride = dndOn && report.isGranted(AlarmPermission.DoNotDisturb) && viewModel.dndOverrideSupported,
        onDndOverride = { on ->
            val guide = on && !viewModel.dndOverrideSupported
            viewModel.setDndOverrideEnabled(on && !guide)
            if (guide) onOpenDndGuide()
            if (on && !guide && !report.isGranted(AlarmPermission.DoNotDisturb)) {
                PermissionSettings.open(context, PermissionSettings.intentFor(context, AlarmPermission.DoNotDisturb))
            }
        },
    )
    if (askPowerConsent) {
        PowerGuardSheet(
            onAgree = {
                viewModel.agreePowerGuard()
                askPowerConsent = false
                PermissionSettings.open(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            onLater = {
                askPowerConsent = false
                viewModel.setPowerGuardEnabled(false)
            },
        )
    }
}

@Composable
fun SettingsScreen(
    report: PermissionReport,
    versionName: String,
    tryAlarmAt: LocalTime?,
    onOpenPermissions: () -> Unit,
    onTryAlarm: () -> Unit,
    onFeedback: () -> Unit,
    modifier: Modifier = Modifier,
    onFaq: () -> Unit = {},
    onLicenses: () -> Unit = {},
    themeMode: ThemeMode? = null,
    onThemeMode: (ThemeMode) -> Unit = {},
    weatherCard: @Composable () -> Unit = {},
    labCard: @Composable () -> Unit = {},
    powerGuard: Boolean = false,
    onPowerGuard: (Boolean) -> Unit = {},
    dndOverride: Boolean = false,
    onDndOverride: (Boolean) -> Unit = {},
    nextAlarmShown: Boolean = false,
    onNextAlarmShown: (Boolean) -> Unit = {},
) {
    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // 제목 줄은 위에 고정하고, 카드만 그 아래에서 스크롤돼요.
            MorningTopBar(title = stringResource(R.string.settings_title))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column(
                modifier = Modifier.padding(
                    start = MorningDimens.layoutScreenMargin,
                    end = MorningDimens.layoutScreenMargin,
                    top = MorningDimens.space16,
                ),
                verticalArrangement = Arrangement.spacedBy(MorningDimens.layoutCardGap),
            ) {
                // 카드 제목은 숨겨요(결정 101). 순서: 알람, 기상 미션, 날씨 애니메이션, 도움말, 앱 정보.
                MorningSettingsCard(title = null) {
                    NavRows {
                        PermissionNavRow(report, onOpenPermissions)
                        NavRow(
                            label = stringResource(R.string.settings_alarm_try),
                            value = stringResource(R.string.settings_alarm_try_value),
                            onClick = onTryAlarm,
                        )
                    }
                    // 알람 카드 맨 아래 '다음 알람 늘 보이기'(결정 179, 시안 TSK-224). 처음 꺼짐.
                    MorningSettingRow(
                        label = stringResource(R.string.settings_next_alarm),
                        value = stringResource(R.string.settings_next_alarm_desc),
                        state = if (nextAlarmShown) SettingRowState.On else SettingRowState.Off,
                        onCheckedChange = onNextAlarmShown,
                        modifier = Modifier.heightIn(min = SettingsToggleRowHeight),
                    )
                }
                // 깼는지 묻기 값은 알람마다라 알람 편집 S4 시트에 있어요(결정 180, 시안 TSK-226).
                MorningSettingsCard(title = null) {
                    MorningSettingRow(
                        label = stringResource(R.string.settings_power_guard),
                        value = stringResource(R.string.settings_power_guard_desc),
                        state = if (powerGuard) SettingRowState.On else SettingRowState.Off,
                        onCheckedChange = onPowerGuard,
                        modifier = Modifier.heightIn(min = SettingsToggleRowHeight),
                    )
                    MorningSettingRow(
                        label = stringResource(R.string.settings_dnd_override),
                        value = stringResource(R.string.settings_dnd_override_desc),
                        state = if (dndOverride) SettingRowState.On else SettingRowState.Off,
                        onCheckedChange = onDndOverride,
                        modifier = Modifier.heightIn(min = SettingsToggleRowHeight),
                    )
                }
                weatherCard()
                MorningSettingsCard(title = null) {
                    NavRows {
                        NavRow(label = stringResource(R.string.settings_faq), onClick = onFaq)
                        NavRow(label = stringResource(R.string.settings_feedback), onClick = onFeedback)
                    }
                }
                MorningSettingsCard(title = null) {
                    NavRows {
                        NavRow(label = stringResource(R.string.settings_version), value = versionName, onClick = null)
                        NavRow(label = stringResource(R.string.settings_licenses), onClick = onLicenses)
                    }
                }
                // 맨 아래 '실험실' 작은 글과 '확실히 잠깨기' 카드(결정 146).
                labCard()
                // 디버그 빌드만: 화면 모드(임시, TSK-166). 출시 빌드에는 이 카드가 없어요.
                if (themeMode != null) {
                    MorningSettingsCard(title = null) {
                        NavRows {
                            // 누를 때마다 자동 → 라이트 → 다크 → 자동으로 바뀌어요.
                            NavRow(
                                label = stringResource(R.string.settings_theme_mode),
                                value = stringResource(themeMode.labelRes()),
                                onClick = { onThemeMode(themeMode.next()) },
                            )
                        }
                    }
                }
            }
            // 끝까지 내리면 마지막 카드가 하단 메뉴 위에 보여요.
            Spacer(Modifier.navigationBarsPadding().height(BottomNavSpace))
            }
        }
        MorningMessageHost(
            message = tryAlarmAt,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = BottomNavSpace),
        ) { time ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                MorningToast(stringResource(R.string.settings_alarm_try_done, time.toString()))
            }
        }
    }
}

/** 카드 안 진입 줄 묶음. 줄끼리는 사이 없이 44 간격으로 붙어요(시안 줄 간격 44, 데모와 같음). */
@Composable
internal fun NavRows(content: @Composable () -> Unit) {
    Column { content() }
}

/**
 * '권한' 한 줄(결정 102, 130 보충). 필수 셋(알림, 전체 화면 알림, 다른 앱 위에 표시)이 모두 켜지면 '허용됨'(회색),
 * 일부만 켜지면 '일부 허용됨', 하나도 안 켜지면 '허용 안 됨'(둘 다 빨강). 선택, 기타 권한은 세지 않아요.
 */
@Composable
private fun PermissionNavRow(report: PermissionReport, onOpen: () -> Unit) {
    val granted = report.grantedRequiredCount
    val status = when (granted) {
        PermissionReport.RequiredCount -> R.string.permission_granted
        0 -> R.string.permission_status_none
        else -> R.string.permission_status_partial
    }
    NavRow(
        label = stringResource(R.string.settings_permissions),
        value = stringResource(status),
        valueError = granted < PermissionReport.RequiredCount,
        onClick = onOpen,
    )
}

/**
 * 피그마 Nav Row. 한 줄 44(`size/row-plain`), 오른쪽에 값과 chevron-right 16(사이 5).
 * [valueError]면 값 글자가 빨강이에요(권한 상태, 결정 102). [onClick]이 없으면 화살표 없이 값만 보여요(버전).
 */
@Composable
internal fun NavRow(label: String, onClick: (() -> Unit)?, value: String? = null, valueError: Boolean = false) {
    val colors = MorningTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(MorningDimens.sizeRowPlain)
            .then(if (onClick != null) Modifier.morningClickable(haptic = null, onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NavRowGap),
    ) {
        Text(
            text = label,
            style = MorningTheme.typography.bodyMedium,
            color = colors.textBody,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                text = value,
                style = MorningTheme.typography.captionMedium,
                color = if (valueError) colors.statusError else colors.textSecondary,
            )
        }
        if (onClick != null) {
            Icon(Lucide.ChevronRight, null, Modifier.size(ChevronSize), tint = colors.textTertiary)
        }
    }
}

/** 메일 앱을 열고 기기 정보와 꺼진 권한을 본문에 미리 채워요. 로그인은 없어요. */
private fun sendFeedback(context: Context, versionName: String, report: PermissionReport) {
    val missing = report.missingRequired.joinToString { it.name }
        .ifEmpty { context.getString(R.string.settings_feedback_none) }
    val body = context.getString(
        R.string.settings_feedback_body,
        "${Build.MANUFACTURER} ${Build.MODEL}",
        Build.VERSION.RELEASE,
        versionName,
        missing,
    )
    val intent = Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
        .putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
        .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.settings_feedback_subject))
        .putExtra(Intent.EXTRA_TEXT, body)
    PermissionSettings.open(context, intent)
}

private fun ThemeMode.next(): ThemeMode = ThemeMode.entries[(ordinal + 1) % ThemeMode.entries.size]

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.Auto -> R.string.settings_theme_mode_auto
    ThemeMode.Light -> R.string.settings_theme_mode_light
    ThemeMode.Dark -> R.string.settings_theme_mode_dark
}
