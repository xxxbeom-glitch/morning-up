package com.lumian.morningup.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumian.morningup.core.alarm.ring.LabLog
import com.lumian.morningup.core.alarm.ring.TrickAccessibility
import com.lumian.morningup.core.designsystem.component.MorningButton
import com.lumian.morningup.core.designsystem.component.MorningButtonSize
import com.lumian.morningup.core.designsystem.component.MorningButtonStyle
import com.lumian.morningup.core.designsystem.component.MorningModalSheet
import com.lumian.morningup.core.designsystem.component.MorningOverlay
import com.lumian.morningup.core.designsystem.component.MorningSettingRow
import com.lumian.morningup.core.designsystem.component.MorningSheetHeader
import com.lumian.morningup.core.designsystem.component.MorningSettingsCard
import com.lumian.morningup.core.designsystem.component.MorningTopBar
import com.lumian.morningup.core.designsystem.component.SettingRowState
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.LabSettings
import com.lumian.morningup.core.model.TrickSpeed
import com.lumian.morningup.core.model.WakeCheckSettings
import com.lumian.morningup.core.model.WakeCheckSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** T3에서 여는 시트(결정 146 1절). */
enum class LabSheet { Trick, HideEscape }

const val LAB_TRICK_SHEET_TAG = "lab_trick_sheet"
const val LAB_ESCAPE_SHEET_TAG = "lab_escape_sheet"

/**
 * '확실히 잠깨기' 실험실(결정 146). 값은 앱 전체에 하나이고 다음 알람부터 새 값으로 울려요.
 * 켜고 끌 때 폰 안 진단 기록에 남겨요(4절 `lab_trick_on/off`). 여러 번 묻기는 없앴어요(결정 177).
 */
@HiltViewModel
class LabViewModel @Inject constructor(
    private val store: WakeCheckSettingsStore,
    private val labLog: LabLog,
) : ViewModel() {
    val settings: StateFlow<WakeCheckSettings> = store.settings

    fun setTrick(on: Boolean) {
        val lab = updateLab { it.copy(trickEnabled = on) }
        labLog.record(
            if (on) LabLog.TRICK_ON else LabLog.TRICK_OFF,
            detail = "speed=${lab.trickSpeed.key}",
            kept = true,
        )
    }

    fun setTrickSpeed(speed: TrickSpeed) {
        updateLab { it.copy(trickSpeed = speed) }
    }

    /** 미션 포기 알림 숨기기(결정 151). 켤 때는 T3-4에서 '숨기기'를 누른 뒤에만 불려요. */
    fun setEscapeHidden(hidden: Boolean) {
        updateLab { it.copy(escapeHidden = hidden) }
        labLog.record(if (hidden) LabLog.ESCAPE_HIDE_ON else LabLog.ESCAPE_HIDE_OFF, kept = true)
    }

    private fun updateLab(transform: (LabSettings) -> LabSettings): LabSettings {
        store.update { it.copy(lab = transform(it.lab)) }
        return store.settings.value.lab
    }
}

/** T1 맨 아래 작은 글 '실험실'과 카드 하나('확실히 잠깨기', 값은 '꺼짐' 또는 켠 것 이름). */
@Composable
fun LabCard(onOpen: () -> Unit, viewModel: LabViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    LabCardContent(settings.lab, onOpen)
}

@Composable
fun LabCardContent(lab: LabSettings, onOpen: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MorningDimens.space12)) {
        Text(
            text = stringResource(R.string.settings_lab),
            style = MorningTheme.typography.captionMedium,
            color = MorningTheme.colors.textSecondary,
            modifier = Modifier.padding(horizontal = MorningDimens.layoutCardPadding),
        )
        MorningSettingsCard(title = null) {
            NavRows {
                NavRow(label = stringResource(R.string.settings_lab_title), value = labValue(lab), onClick = onOpen)
            }
        }
    }
}

/** T1 줄 값: '꺼짐' 또는 '버튼 속이기'(결정 177). */
@Composable
private fun labValue(lab: LabSettings): String =
    stringResource(if (lab.trickEnabled) R.string.settings_lab_trick else R.string.settings_lab_off)

/** T3 '확실히 잠깨기' 페이지. [onPreview]는 T3-3 미리 해보기를 열어요. */
@Composable
fun LabRoute(onBack: () -> Unit, onPreview: () -> Unit, viewModel: LabViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val trickOff = remember { TrickAccessibility.offReason(context) != null }
    LabScreen(
        lab = settings.lab,
        onBack = onBack,
        onTrick = viewModel::setTrick,
        onTrickSpeed = viewModel::setTrickSpeed,
        onPreview = onPreview,
        onEscapeHidden = viewModel::setEscapeHidden,
        trickOff = trickOff,
    )
}

/**
 * 상태 없는 T3. 위 설명, 카드(토글 + 시트 줄 둘), 아래 작은 글. 줄 글자 쪽을 누르면 시트,
 * 오른쪽 토글은 켜고 끄기예요. 토글을 켤 때는 시트도 같이 열어 무엇이 바뀌는지 먼저 보여 줘요.
 */
@Suppress("LongParameterList")
@Composable
fun LabScreen(
    lab: LabSettings,
    onBack: () -> Unit,
    onTrick: (Boolean) -> Unit,
    onTrickSpeed: (TrickSpeed) -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier,
    onEscapeHidden: (Boolean) -> Unit = {},
    trickOff: Boolean = false,
) {
    var sheet by rememberSaveable { mutableStateOf<LabSheet?>(null) }
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundBase)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        MorningTopBar(
            title = stringResource(R.string.settings_lab_title),
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
            verticalArrangement = Arrangement.spacedBy(MorningDimens.space12),
        ) {
            Text(
                text = stringResource(R.string.settings_lab_intro),
                style = typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = MorningDimens.layoutCardPadding),
            )
            MorningSettingsCard(title = null) {
                MorningSettingRow(
                    label = stringResource(R.string.settings_lab_trick),
                    value = if (trickOff) stringResource(R.string.settings_lab_trick_a11y) else trickValue(lab),
                    state = if (lab.trickEnabled) SettingRowState.On else SettingRowState.Off,
                    onCheckedChange = { on ->
                        onTrick(on)
                        if (on) sheet = LabSheet.Trick
                    },
                    onClick = { sheet = LabSheet.Trick },
                    modifier = Modifier.heightIn(min = SettingsToggleRowHeight),
                )
            }
            Text(
                text = stringResource(R.string.settings_lab_note),
                style = typography.captionMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = MorningDimens.layoutCardPadding),
            )
            Spacer(Modifier.height(MorningDimens.space4))
            EscapeCard(
                hidden = lab.escapeHidden,
                onToggle = { on -> if (on) sheet = LabSheet.HideEscape else onEscapeHidden(false) },
            )
        }
    }
    val open = sheet
    if (open != null) {
        MorningOverlay {
            MorningModalSheet(
                visible = true,
                onDismiss = { sheet = null },
                tag = when (open) {
                    LabSheet.Trick -> LAB_TRICK_SHEET_TAG
                    LabSheet.HideEscape -> LAB_ESCAPE_SHEET_TAG
                },
            ) {
                when (open) {
                    LabSheet.Trick -> TrickSheet(
                        lab = lab,
                        onSpeed = onTrickSpeed,
                        onPreview = {
                            sheet = null
                            onPreview()
                        },
                        onDone = { sheet = null },
                    )
                    LabSheet.HideEscape -> HideEscapeSheet(
                        onCancel = { sheet = null },
                        onHide = {
                            sheet = null
                            onEscapeHidden(true)
                        },
                    )
                }
            }
        }
    }
}

/**
 * T3 둘째 카드: '미션 포기' 알림 숨기기 토글 한 줄과 아래 작은 글(결정 151, 시안 1084:13871).
 * 켜면 바로 바뀌지 않고 T3-4 확인 시트를 먼저 열어요. 끄는 것은 바로 돼요.
 */
@Composable
private fun EscapeCard(hidden: Boolean, onToggle: (Boolean) -> Unit) {
    MorningSettingsCard(title = null) {
        MorningSettingRow(
            label = stringResource(R.string.settings_lab_escape),
            value = stringResource(R.string.settings_lab_escape_value),
            state = if (hidden) SettingRowState.On else SettingRowState.Off,
            onCheckedChange = onToggle,
            onClick = { onToggle(!hidden) },
            modifier = Modifier.heightIn(min = SettingsToggleRowHeight),
        )
    }
    Text(
        text = stringResource(R.string.settings_lab_escape_note),
        style = MorningTheme.typography.captionMedium,
        color = MorningTheme.colors.textSecondary,
        modifier = Modifier.padding(horizontal = MorningDimens.layoutCardPadding),
    )
}

/** T3-4: 제목, 설명, '숨겨도 이럴 때는 떠요'와 두 줄, 아래 '취소'(보조)와 '숨기기'를 같은 너비로(시안 1084:13939). */
@Composable
private fun ColumnScope.HideEscapeSheet(onCancel: () -> Unit, onHide: () -> Unit) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    MorningSheetHeader(
        title = stringResource(R.string.settings_lab_escape_sheet_title),
        description = stringResource(R.string.settings_lab_escape_sheet_desc),
        modifier = Modifier.padding(top = MorningDimens.space8),
    )
    Spacer(Modifier.height(MorningDimens.space16))
    Column(verticalArrangement = Arrangement.spacedBy(MorningDimens.space8)) {
        Text(
            text = stringResource(R.string.settings_lab_escape_sheet_label),
            style = typography.bodyMediumStrong,
            color = colors.textPrimary,
        )
        Text(
            text = stringResource(R.string.settings_lab_escape_sheet_item1),
            style = typography.bodyMedium,
            color = colors.textBody,
        )
        Text(
            text = stringResource(R.string.settings_lab_escape_sheet_item2),
            style = typography.bodyMedium,
            color = colors.textBody,
        )
    }
    Spacer(Modifier.height(MorningDimens.space24))
    Row(
        modifier = Modifier.fillMaxWidth().bleed(FooterBleed),
        horizontalArrangement = Arrangement.spacedBy(MorningDimens.space8),
    ) {
        MorningButton(
            text = stringResource(R.string.settings_lab_escape_cancel),
            onClick = onCancel,
            style = MorningButtonStyle.Secondary,
            modifier = Modifier.weight(1f),
        )
        MorningButton(
            text = stringResource(R.string.settings_lab_escape_hide),
            onClick = onHide,
            modifier = Modifier.weight(1f),
        )
    }
}

/** T3 '버튼 속이기' 둘째 줄: 꺼지면 '울릴 때 버튼이 움직여요', 켜면 '속도 1250ms, 20초 동안'. */
@Composable
private fun trickValue(lab: LabSettings): String = if (lab.trickEnabled) {
    stringResource(
        R.string.settings_lab_trick_on,
        stringResource(R.string.settings_lab_speed_ms, lab.trickSpeed.millis),
        LabSettings.TRICK_SECONDS,
    )
} else {
    stringResource(R.string.settings_lab_trick_off)
}

/** T3-1: 빠르기 ms 숫자 칸과 설명(결정 181), '미리 해보기', '완료'. */
@Composable
private fun ColumnScope.TrickSheet(
    lab: LabSettings,
    onSpeed: (TrickSpeed) -> Unit,
    onPreview: () -> Unit,
    onDone: () -> Unit,
) {
    SheetFrame(
        title = stringResource(R.string.settings_lab_trick),
        description = stringResource(R.string.settings_lab_trick_desc, LabSettings.TRICK_SECONDS),
        onDone = onDone,
    ) {
        TrickSpeedInput(speed = lab.trickSpeed, onSpeed = onSpeed)
        Spacer(Modifier.height(MorningDimens.space20))
        MorningButton(
            text = stringResource(R.string.settings_lab_preview),
            onClick = onPreview,
            size = MorningButtonSize.Medium,
            style = MorningButtonStyle.Soft,
        )
    }
}
