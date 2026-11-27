package com.lumian.morningup.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumian.morningup.core.alarm.PermissionSettings
import com.lumian.morningup.core.designsystem.component.MorningButton
import com.lumian.morningup.core.designsystem.component.MorningSettingRow
import com.lumian.morningup.core.designsystem.component.MorningSettingsCard
import com.lumian.morningup.core.designsystem.component.SettingRowState
import com.lumian.morningup.core.designsystem.component.MorningToggleSize
import com.lumian.morningup.core.designsystem.component.MorningTopBar
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningShadows
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.designsystem.theme.morningShadow
import com.lumian.morningup.core.model.AlarmPermission
import com.lumian.morningup.core.model.PermissionBanner

/**
 * T2-1을 열고 버튼으로 폰 방해 금지 설정을 열어요. 맨 위 스위치는 설정 카드 1에서 옮겨 왔어요(TSK-287).
 * 방해 금지 접근 권한이 있을 때만 켜진 모양이에요. 없으면 켤 때 그 권한 화면으로 보내요(TSK-177 답 3).
 * 안드로이드 15부터는 앱이 사용자 방해 금지를 풀 수 없어서 스위치를 숨기고 안내만 보여요(ISS-057).
 */
@Composable
fun DndGuideRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val report by viewModel.report.collectAsStateWithLifecycle()
    val dndOn by viewModel.dndOverrideEnabled.collectAsStateWithLifecycle()
    val granted = report.isGranted(AlarmPermission.DoNotDisturb)
    DndGuidePage(
        onBack = onBack,
        onOpenSettings = { openBannerTarget(context, PermissionBanner.AlarmsSilenced) },
        modifier = modifier,
        override = if (viewModel.dndOverrideSupported) dndOn && granted else null,
        onOverride = { on ->
            viewModel.setDndOverrideEnabled(on)
            if (on && !granted) {
                PermissionSettings.open(context, PermissionSettings.intentFor(context, AlarmPermission.DoNotDisturb))
            }
        },
    )
}

/**
 * T2-1 '방해 금지 모드에서 허용하기'(TSK-177 답 4, 시안 TSK-220, 글은 TSK-214 결과).
 * '알람' 켜기 한 단계를 크게 보여 주고, 모드 및 루틴과 허용할 앱은 작은 '알아 두세요' 카드로 알려요.
 * 앱이 허용 상태를 읽을 수 없어서 상태 표시 없이 안내만 해요. 카드 안 스위치는 폰 설정을 보여 주는 그림이에요.
 * 홈 배너, 저장 경고 '설정 열기', '소리 없이 울렸어요' 창에서 열려요.
 */
@Composable
fun DndGuidePage(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    override: Boolean? = null,
    onOverride: (Boolean) -> Unit = {},
) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundBase)
            .statusBarsPadding(),
    ) {
        MorningTopBar(
            title = stringResource(R.string.dnd_guide_title),
            onBack = onBack,
            backContentDescription = stringResource(R.string.settings_back),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = MorningDimens.layoutScreenMargin,
                    end = MorningDimens.layoutScreenMargin,
                    top = MorningDimens.space16,
                    bottom = MorningDimens.space16,
                ),
            verticalArrangement = Arrangement.spacedBy(MorningDimens.layoutCardGap),
        ) {
            // 맨 위 스위치 카드(피그마 T2-1 `1209:30511`). [override]가 null이면(안드로이드 15 이상) 숨겨요.
            if (override != null) {
                MorningSettingsCard(title = null) {
                    MorningSettingRow(
                        label = stringResource(R.string.dnd_switch_title),
                        value = stringResource(R.string.dnd_switch_desc),
                        state = if (override) SettingRowState.On else SettingRowState.Off,
                        onCheckedChange = onOverride,
                        modifier = Modifier.heightIn(min = SettingsToggleRowHeight),
                    )
                }
            }
            Text(
                text = stringResource(R.string.dnd_guide_intro),
                style = typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = MorningDimens.layoutCardPadding),
            )
            GuideCard {
                Text(
                    text = stringResource(R.string.dnd_guide_step_title),
                    style = typography.titleMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(MorningDimens.space8))
                Text(
                    text = stringResource(R.string.dnd_guide_step_body),
                    style = typography.bodyMedium,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(MorningDimens.space16))
                SwitchPicture()
            }
            GuideCard {
                Text(
                    text = stringResource(R.string.dnd_guide_note_title),
                    style = typography.bodyMediumStrong,
                    color = colors.textPrimary,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(MorningDimens.space8))
                Text(
                    text = stringResource(R.string.dnd_guide_note_routine),
                    style = typography.bodySmall,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(MorningDimens.space8))
                Text(
                    text = stringResource(R.string.dnd_guide_note_apps),
                    style = typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            Text(
                text = stringResource(R.string.dnd_guide_footnote),
                style = typography.bodySmall,
                color = colors.textTertiary,
                modifier = Modifier.padding(horizontal = MorningDimens.layoutCardPadding),
            )
        }
        MorningButton(
            text = stringResource(R.string.dnd_guide_open),
            onClick = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = MorningDimens.layoutScreenMargin,
                    end = MorningDimens.layoutScreenMargin,
                    bottom = MorningDimens.space16,
                ),
        )
    }
}

@Composable
private fun GuideCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(MorningDimens.radiusCard)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .morningShadow(MorningShadows.card, shape)
            .background(MorningTheme.colors.surfaceCard, shape)
            .padding(MorningDimens.layoutCardPadding),
        content = content,
    )
}

/** 폰 방해 금지 설정의 '알람' 스위치를 켠 모습. 누를 수 없는 그림이라 읽기 도구에는 글만 읽혀요. */
@Composable
private fun SwitchPicture() {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.backgroundBase, RoundedCornerShape(MorningDimens.radiusTile))
            .padding(horizontal = MorningDimens.space16, vertical = MorningDimens.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.dnd_guide_switch_label),
                style = typography.bodyMediumStrong,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.dnd_guide_switch_desc),
                style = typography.captionMedium,
                color = colors.textSecondary,
            )
        }
        Spacer(Modifier.width(MorningDimens.space12))
        val size = MorningToggleSize.Default
        val inset = MorningDimens.sizeSwitchInset
        Box(
            modifier = Modifier
                .size(size.width, size.height)
                .clearAndSetSemantics {}
                .background(colors.primaryDefault, RoundedCornerShape(MorningDimens.radiusFull))
                .padding(inset),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                Modifier
                    .size(size.height - inset * 2)
                    .morningShadow(MorningShadows.knob, CircleShape)
                    .background(colors.controlKnob, CircleShape),
            )
        }
    }
}
