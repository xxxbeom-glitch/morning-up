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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumian.morningup.core.alarm.ring.LabLog
import com.lumian.morningup.core.alarm.ring.TrickAccessibility
import com.lumian.morningup.core.designsystem.component.MorningButton
import com.lumian.morningup.core.designsystem.component.MorningButtonSize
import com.lumian.morningup.core.designsystem.component.MorningButtonStyle
import com.lumian.morningup.core.designsystem.component.MorningChoiceChip
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
enum class LabSheet { Trick, MultiAsk, HideEscape }

const val LAB_TRICK_SHEET_TAG = "lab_trick_sheet"
const val LAB_MULTI_SHEET_TAG = "lab_multi_sheet"
const val LAB_ESCAPE_SHEET_TAG = "lab_escape_sheet"

private val ChipGap = 6.dp

/**
 * '확실히 잠깨기' 실험실(결정 146). 값은 앱 전체에 하나이고 다음 알람부터 새 값으로 울려요.
 * 켜고 끌 때 폰 안 진단 기록에 남겨요(4절 `lab_trick_on/off`, `lab_recheck_on/off`).
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

    fun setMultiAsk(on: Boolean) {
        val lab = updateLab { it.copy(multiAskEnabled = on) }
        labLog.record(
            if (on) LabLog.RECHECK_ON else LabLog.RECHECK_OFF,
            detail = "count=${lab.multiAskCount}",
            kept = true,
        )
    }

    /** 미션 포기 알림 숨기기(결정 151). 켤 때는 T3-4에서 '숨기기'를 누른 뒤에만 불려요. */
    fun setEscapeHidden(hidden: Boolean) {
        updateLab { it.copy(escapeHidden = hidden) }
        labLog.record(if (hidden) LabLog.ESCAPE_HIDE_ON else LabLog.ESCAPE_HIDE_OFF, kept = true)
    }

    fun setMultiAskCount(count: Int) {
        if (count in LabSettings.ASK_COUNTS) updateLab { it.copy(multiAskCount = count) }
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

/** T1 줄 값: '꺼짐', '버튼 속이기', '여러 번 묻기', 둘 다면 '버튼 속이기, 여러 번 묻기'. */
@Composable
private fun labValue(lab: LabSettings): String {
    val trick = stringResource(R.string.settings_lab_trick)
    val multi = stringResource(R.string.settings_lab_multi)
    return when {
        lab.trickEnabled && lab.multiAskEnabled -> stringResource(R.string.settings_lab_both, trick, multi)
        lab.trickEnabled -> trick
        lab.multiAskEnabled -> multi
        else -> stringResource(R.string.settings_lab_off)
    }
}

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
        onMultiAsk = viewModel::setMultiAsk,
        onMultiAskCount = viewModel::setMultiAskCount,
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
    onMultiAsk: (Boolean) -> Unit,
    onMultiAskCount: (Int) -> Unit,
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
                MorningSettingRow(
                    label = stringResource(R.string.settings_lab_multi),
                    value = stringResource(R.string.settings_lab_multi_value, lab.multiAskCount),
                    state = if (lab.multiAskEnabled) SettingRowState.On else SettingRowState.Off,
                    onCheckedChange = { on ->
                        onMultiAsk(on)
                        if (on) sheet = LabSheet.MultiAsk
                    },
                    onClick = { sheet = LabSheet.MultiAsk },
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
                    LabSheet.MultiAsk -> LAB_MULTI_SHEET_TAG
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
                    LabSheet.MultiAsk -> MultiAskSheet(lab, onMultiAskCount) { sheet = null }
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

/** T3 '버튼 속이기' 둘째 줄: 꺼지면 '울릴 때 버튼이 움직여요', 켜면 '속도 보통, 20초 동안'. */
@Composable
private fun trickValue(lab: LabSettings): String = if (lab.trickEnabled) {
    stringResource(
        R.string.settings_lab_trick_on,
        stringResource(lab.trickSpeed.labelRes()),
        LabSettings.TRICK_SECONDS,
    )
} else {
    stringResource(R.string.settings_lab_trick_off)
}

/** T3-1: 속도 칩과 설명, '미리 해보기', '완료'(결정 155로 강도 칩은 없앰). */
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
        FullChipGroup(
            label = stringResource(R.string.settings_lab_speed),
            options = TrickSpeed.entries,
            selected = lab.trickSpeed,
            optionLabel = { stringResource(it.labelRes()) },
            onSelect = onSpeed,
            note = stringResource(R.string.settings_lab_speed_desc),
        )
        Spacer(Modifier.height(MorningDimens.space20))
        MorningButton(
            text = stringResource(R.string.settings_lab_preview),
            onClick = onPreview,
            size = MorningButtonSize.Medium,
            style = MorningButtonStyle.Soft,
        )
    }
}

/** T3-2: 3번, 4번, 5번 칩과 아래 작은 글, '완료'. */
@Composable
private fun ColumnScope.MultiAskSheet(lab: LabSettings, onCount: (Int) -> Unit, onDone: () -> Unit) {
    SheetFrame(
        title = stringResource(R.string.settings_lab_multi),
        description = stringResource(R.string.settings_lab_multi_desc),
        onDone = onDone,
    ) {
        FullChipGroup(
            label = null,
            options = LabSettings.ASK_COUNTS,
            selected = lab.multiAskCount,
            optionLabel = { stringResource(R.string.settings_lab_multi_count, it) },
            onSelect = onCount,
            note = stringResource(R.string.settings_lab_multi_note),
            noteSmall = true,
        )
    }
}

/** 위에 이름, 줄 너비에 꽉 차는 Choice Chip(칠로만 선택, 테두리 없음), 아래 설명 한 줄. */
@Suppress("LongParameterList")
@Composable
private fun <T> FullChipGroup(
    label: String?,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    note: String,
    noteSmall: Boolean = false,
) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(MorningDimens.space8)) {
        if (label != null) {
            Text(text = label, style = typography.bodyMediumStrong, color = colors.textPrimary)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ChipGap)) {
            options.forEach { option ->
                MorningChoiceChip(
                    label = optionLabel(option),
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            text = note,
            style = if (noteSmall) typography.captionMedium else typography.bodyMedium,
            color = if (noteSmall) colors.textSecondary else colors.textBody,
            modifier = Modifier.padding(top = MorningDimens.space4),
        )
    }
}

private fun TrickSpeed.labelRes(): Int = when (this) {
    TrickSpeed.Slow -> R.string.settings_lab_speed_slow
    TrickSpeed.Normal -> R.string.settings_lab_speed_normal
    TrickSpeed.Fast -> R.string.settings_lab_speed_fast
}
