package com.lumian.morningup.feature.settings

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
import com.lumian.morningup.core.designsystem.component.MorningChoiceChip
import com.lumian.morningup.core.designsystem.component.MorningDialogSheet
import com.lumian.morningup.core.designsystem.component.MorningMessageHost
import com.lumian.morningup.core.designsystem.component.MorningModalSheet
import com.lumian.morningup.core.designsystem.component.MorningOverlay
import com.lumian.morningup.core.designsystem.component.MorningSettingRow
import com.lumian.morningup.core.designsystem.component.MorningSettingsCard
import com.lumian.morningup.core.designsystem.component.MorningSheetHeader
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

/**
 * T1 설정(피그마 `1209:28778`, TSK-287). 카드 8개, 카드마다 바깥 위에 작은 묶음 이름을 달아요.
 * 알람이 잘 울리게, 알람, 소리, 미션 중, 화면, 실험실, 도움, 앱 정보 순서예요.
 * 화면 모드와 시험 알람은 디버그 빌드의 adb 명령으로 바꿔요(docs/qa/README.md). 화면에는 시험용 줄이 없어요(TSK-270).
 */
@Composable
fun SettingsRoute(
    versionName: String,
    onOpenPermissions: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenFaq: () -> Unit = {},
    onOpenLicenses: () -> Unit = {},
    weatherCard: @Composable () -> Unit = { WeatherCard() },
    onOpenLab: () -> Unit = {},
    onOpenDndGuide: () -> Unit = {},
    onOpenSupport: (() -> Unit)? = null,
    privacyUrl: String? = null,
    labCard: @Composable () -> Unit = { LabCard(onOpen = onOpenLab) },
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val report by viewModel.report.collectAsStateWithLifecycle()
    val tryAt by viewModel.tryAlarmAt.collectAsStateWithLifecycle()
    val powerOn by viewModel.powerGuardEnabled.collectAsStateWithLifecycle()
    val nextShown by viewModel.nextAlarmShown.collectAsStateWithLifecycle()
    val weekMonday by viewModel.weekMonday.collectAsStateWithLifecycle()
    val external by viewModel.externalOutput.collectAsStateWithLifecycle()
    var askPowerConsent by rememberSaveable { mutableStateOf(false) }
    var askExternal by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    SettingsScreen(
        report = report,
        versionName = versionName,
        tryAlarmAt = tryAt,
        onOpenPermissions = onOpenPermissions,
        onTryAlarm = viewModel::tryAlarm,
        onFeedback = onOpenSupport ?: { sendFeedback(context, versionName, report) },
        onFaq = onOpenFaq,
        onLicenses = onOpenLicenses,
        modifier = modifier,
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
        onDndGuide = onOpenDndGuide,
        nextAlarmShown = nextShown,
        onNextAlarmShown = viewModel::setNextAlarmShown,
        weekMonday = weekMonday,
        onWeekMonday = viewModel::setWeekMonday,
        // 켤 때만 안내 시트를 한 번 띄우고, 끌 때는 바로 꺼요(plan-11-settings-menu 3절).
        externalOutput = external,
        onExternalOutput = { on -> if (on) askExternal = true else viewModel.setExternalOutput(false) },
        onPrivacy = privacyUrl?.let { url ->
            { PermissionSettings.open(context, Intent(Intent.ACTION_VIEW, url.toUri())) }
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
    if (askExternal) {
        ExternalOutputSheet(
            onTurnOn = {
                viewModel.setExternalOutput(true)
                askExternal = false
            },
            onCancel = { askExternal = false },
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
    weatherCard: @Composable () -> Unit = {},
    labCard: @Composable () -> Unit = {},
    powerGuard: Boolean = false,
    onPowerGuard: (Boolean) -> Unit = {},
    onDndGuide: () -> Unit = {},
    nextAlarmShown: Boolean = false,
    onNextAlarmShown: (Boolean) -> Unit = {},
    weekMonday: Boolean = false,
    onWeekMonday: (Boolean) -> Unit = {},
    externalOutput: Boolean = false,
    onExternalOutput: (Boolean) -> Unit = {},
    onPrivacy: (() -> Unit)? = null,
) {
    var weekSheet by rememberSaveable { mutableStateOf(false) }
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
                verticalArrangement = Arrangement.spacedBy(MorningDimens.space24),
            ) {
                SettingsGroup(stringResource(R.string.settings_card_ringing)) {
                    MorningSettingsCard(title = null) {
                        NavRows {
                            PermissionNavRow(report, onOpenPermissions)
                            NavRow(
                                label = stringResource(R.string.settings_dnd_override),
                                desc = stringResource(R.string.settings_dnd_override_desc),
                                onClick = onDndGuide,
                            )
                            NavRow(
                                label = stringResource(R.string.settings_alarm_try),
                                value = stringResource(R.string.settings_alarm_try_value),
                                onClick = onTryAlarm,
                            )
                        }
                    }
                }
                SettingsGroup(stringResource(R.string.settings_card_alarm)) {
                    MorningSettingsCard(title = null) {
                        NavRows {
                            NavRow(
                                label = stringResource(R.string.settings_week_start),
                                value = stringResource(
                                    if (weekMonday) R.string.settings_week_start_mon else R.string.settings_week_start_sun,
                                ),
                                onClick = { weekSheet = true },
                            )
                        }
                        // '다음 알람 항상 표시'(결정 179, 시안 TSK-224). 처음 꺼짐.
                        MorningSettingRow(
                            label = stringResource(R.string.settings_next_alarm),
                            value = stringResource(R.string.settings_next_alarm_desc),
                            state = if (nextAlarmShown) SettingRowState.On else SettingRowState.Off,
                            onCheckedChange = onNextAlarmShown,
                            modifier = Modifier.heightIn(min = SettingsToggleRowHeight),
                        )
                    }
                }
                SettingsGroup(stringResource(R.string.settings_card_sound)) {
                    MorningSettingsCard(title = null) {
                        MorningSettingRow(
                            label = stringResource(R.string.settings_bt_output),
                            value = stringResource(R.string.settings_bt_output_desc),
                            state = if (externalOutput) SettingRowState.On else SettingRowState.Off,
                            onCheckedChange = onExternalOutput,
                            modifier = Modifier.heightIn(min = SettingsToggleRowHeight),
                        )
                    }
                }
                SettingsGroup(stringResource(R.string.settings_card_mission)) {
                    MorningSettingsCard(title = null) {
                        MorningSettingRow(
                            label = stringResource(R.string.settings_power_guard),
                            value = stringResource(R.string.settings_power_guard_desc),
                            state = if (powerGuard) SettingRowState.On else SettingRowState.Off,
                            onCheckedChange = onPowerGuard,
                            modifier = Modifier.heightIn(min = SettingsToggleRowHeight),
                        )
                    }
                }
                SettingsGroup(stringResource(R.string.settings_card_screen)) { weatherCard() }
                // '실험실' 묶음 이름은 LabCardContent가 직접 달아요(결정 146, 188).
                labCard()
                SettingsGroup(stringResource(R.string.settings_card_help)) {
                    MorningSettingsCard(title = null) {
                        NavRows {
                            NavRow(label = stringResource(R.string.settings_faq), onClick = onFaq)
                            NavRow(label = stringResource(R.string.settings_support), onClick = onFeedback)
                        }
                    }
                }
                SettingsGroup(stringResource(R.string.settings_card_info)) {
                    MorningSettingsCard(title = null) {
                        NavRows {
                            NavRow(label = stringResource(R.string.settings_version), value = versionName, onClick = null)
                            NavRow(label = stringResource(R.string.settings_licenses), onClick = onLicenses)
                            // 앱 전용 한 장 사이트 주소가 정해지기 전에는 숨겨요.
                            if (onPrivacy != null) {
                                NavRow(label = stringResource(R.string.settings_privacy), onClick = onPrivacy)
                            }
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
    if (weekSheet) {
        WeekStartSheet(
            monday = weekMonday,
            onPick = { monday ->
                onWeekMonday(monday)
                weekSheet = false
            },
            onDismiss = { weekSheet = false },
        )
    }
}

/** 카드 바깥 위 작은 묶음 이름(시안 T1, 실험실 카드와 같은 모양). */
@Composable
internal fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MorningDimens.space12)) {
        Text(
            text = title,
            style = MorningTheme.typography.captionMedium,
            color = MorningTheme.colors.textSecondary,
            modifier = Modifier.padding(horizontal = MorningDimens.layoutCardPadding),
        )
        content()
    }
}

/** T1-3 한 주 시작 요일 시트(피그마 `1209:30162`). 제목과 칩 두 개, 고르면 바로 닫혀요. */
@Composable
private fun WeekStartSheet(monday: Boolean, onPick: (Boolean) -> Unit, onDismiss: () -> Unit) {
    MorningOverlay {
        MorningModalSheet(visible = true, onDismiss = onDismiss, tag = WEEK_START_SHEET_TAG) {
            MorningSheetHeader(
                title = stringResource(R.string.settings_week_start),
                modifier = Modifier.padding(top = MorningDimens.space8),
            )
            Spacer(Modifier.height(MorningDimens.space16))
            Row(horizontalArrangement = Arrangement.spacedBy(MorningDimens.space8)) {
                MorningChoiceChip(
                    label = stringResource(R.string.settings_week_start_sun),
                    selected = !monday,
                    onClick = { onPick(false) },
                    modifier = Modifier.weight(1f),
                )
                MorningChoiceChip(
                    label = stringResource(R.string.settings_week_start_mon),
                    selected = monday,
                    onClick = { onPick(true) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(MorningDimens.space24))
        }
    }
}

/** T1-4 이어폰, 블루투스 안내 시트(피그마 `1209:30254`). 켤 때 한 번만 떠요. */
@Composable
private fun ExternalOutputSheet(onTurnOn: () -> Unit, onCancel: () -> Unit) {
    MorningDialogSheet(
        title = stringResource(R.string.settings_bt_sheet_title),
        message = stringResource(R.string.settings_bt_sheet_msg),
        confirmLabel = stringResource(R.string.settings_bt_sheet_on),
        onConfirm = onTurnOn,
        laterLabel = stringResource(R.string.settings_bt_sheet_cancel),
        onLater = onCancel,
        tag = EXTERNAL_OUTPUT_SHEET_TAG,
    )
}

internal const val WEEK_START_SHEET_TAG = "weekStartSheet"
internal const val EXTERNAL_OUTPUT_SHEET_TAG = "externalOutputSheet"

/** 카드 안 진입 줄 묶음. 줄끼리는 사이 없이 44 간격으로 붙어요(시안 줄 간격 44, 데모와 같음). */
@Composable
internal fun NavRows(content: @Composable () -> Unit) {
    Column { content() }
}

/**
 * '권한 점검' 한 줄(plan-11-settings-menu 1절, 4절). 필수 권한이 꺼졌거나 방해 금지 모드가 알람을 막고 있으면
 * 빨간 '확인이 필요해요', 아니면 값 없이 화살표만 보여요.
 */
@Composable
private fun PermissionNavRow(report: PermissionReport, onOpen: () -> Unit) {
    val attention = report.needsAttention || report.alarmsSilenced
    NavRow(
        label = stringResource(R.string.settings_permissions),
        value = if (attention) stringResource(R.string.settings_needs_attention) else null,
        valueError = attention,
        onClick = onOpen,
    )
}

/**
 * 피그마 Nav Row. 한 줄 44(`size/row-plain`), 오른쪽에 값과 chevron-right 16(사이 5).
 * [desc]가 있으면 라벨 아래 작은 설명을 달고 두 줄 높이가 돼요(방해 금지 모드 줄).
 * [valueError]면 값 글자가 빨강이에요. [onClick]이 없으면 화살표 없이 값만 보여요(버전).
 */
@Composable
internal fun NavRow(
    label: String,
    onClick: (() -> Unit)?,
    value: String? = null,
    valueError: Boolean = false,
    desc: String? = null,
) {
    val colors = MorningTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (desc == null) {
                    Modifier.height(MorningDimens.sizeRowPlain)
                } else {
                    Modifier.heightIn(min = SettingsToggleRowHeight)
                },
            )
            .then(if (onClick != null) Modifier.morningClickable(haptic = null, onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NavRowGap),
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = label, style = MorningTheme.typography.bodyMedium, color = colors.textBody)
            if (desc != null) {
                Text(text = desc, style = MorningTheme.typography.captionMedium, color = colors.textSecondary)
            }
        }
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
        .putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
        .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.settings_feedback_subject))
        .putExtra(Intent.EXTRA_TEXT, body)
    PermissionSettings.open(context, intent)
}
