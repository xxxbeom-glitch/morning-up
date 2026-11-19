package com.lumian.morningup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumian.morningup.R
import com.lumian.morningup.core.designsystem.component.MorningButton
import com.lumian.morningup.core.designsystem.component.MorningButtonSize
import com.lumian.morningup.core.designsystem.component.MorningSettingsCard
import com.lumian.morningup.core.designsystem.component.MorningToggle
import com.lumian.morningup.core.designsystem.component.MorningToggleSize
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme

private val SwatchSize = 40.dp
private val BottomNavSpace = 120.dp

/**
 * 디버그 빌드 전용 견본. 폰에서 토큰과 부품이 피그마와 같은지 눈으로 확인하는 화면이에요.
 * [tools]에는 시험 알람처럼 앱 연결이 필요한 도구가 들어와요(테스트에서는 비워 둬요).
 */
@Composable
fun SampleScreen(modifier: Modifier = Modifier, tools: @Composable () -> Unit = {}) {
    val colors = MorningTheme.colors
    val type = MorningTheme.typography
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = MorningDimens.layoutScreenMargin),
        verticalArrangement = Arrangement.spacedBy(MorningDimens.space24),
    ) {
        Spacer(Modifier.height(MorningDimens.space24))
        Text(stringResource(R.string.sample_title), style = type.headlineLarge, color = colors.textPrimary)

        Section(stringResource(R.string.sample_colors)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MorningDimens.space8),
                verticalArrangement = Arrangement.spacedBy(MorningDimens.space8),
            ) {
                listOf(
                    colors.primaryDefault, colors.primaryPressed, colors.primarySubtle, colors.surfaceCard,
                    colors.backgroundRaised, colors.controlTrackOff, colors.textPrimary, colors.textSecondary,
                    colors.textDisabled, colors.accentSun, colors.statusSuccess, colors.statusError,
                    colors.sky0, colors.sky1, colors.sky2, colors.sky3,
                ).forEach { Swatch(it) }
            }
        }

        Section(stringResource(R.string.sample_type)) {
            listOf(
                "Display/Large" to type.displayLarge,
                "Headline/Large" to type.headlineLarge,
                "Title/Medium" to type.titleMedium,
                "Body/Medium" to type.bodyMedium,
                "Caption/Medium" to type.captionMedium,
            ).forEach { (name, style) -> TypeLine(name, style) }
        }

        Section(stringResource(R.string.sample_controls)) {
            MorningButton(stringResource(R.string.sample_button_large), onClick = {})
            MorningButton(stringResource(R.string.sample_button_medium), onClick = {}, size = MorningButtonSize.Medium)
            MorningButton(stringResource(R.string.sample_button_circle), onClick = {}, size = MorningButtonSize.Circle)
            var on by remember { mutableStateOf(true) }
            var small by remember { mutableStateOf(false) }
            Row(
                horizontalArrangement = Arrangement.spacedBy(MorningDimens.space16),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MorningToggle(checked = on, onCheckedChange = { on = it })
                MorningToggle(checked = small, onCheckedChange = { small = it }, size = MorningToggleSize.Small)
                MorningToggle(checked = true, onCheckedChange = {}, enabled = false)
            }
        }
        tools()
        Spacer(Modifier.height(BottomNavSpace))
    }
}

/**
 * 시험 알람 A~G. 버튼마다 정해진 설정으로 1분 뒤 울려요(docs/test-alarm-cases.md).
 * 설정 '테스트' 페이지(디버그 빌드만)에 카드 하나로 들어가요.
 */
@Composable
fun DebugAlarmTools(viewModel: DebugAlarmViewModel = hiltViewModel()) {
    val scheduled by viewModel.scheduled.collectAsStateWithLifecycle()
    MorningSettingsCard(title = stringResource(R.string.sample_alarm)) {
        AlarmTestCase.entries.forEach { case ->
            MorningButton(
                stringResource(case.label),
                onClick = { viewModel.schedule(case) },
                size = MorningButtonSize.Medium,
            )
        }
        scheduled?.let { (case, time) ->
            Text(
                stringResource(R.string.sample_alarm_scheduled, case.name, time.toString()),
                style = MorningTheme.typography.bodyMedium,
                color = MorningTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MorningDimens.space12)) {
        Text(title, style = MorningTheme.typography.bodySmallStrong, color = MorningTheme.colors.textSecondary)
        content()
    }
}

@Composable
private fun Swatch(color: Color) {
    Spacer(
        Modifier
            .size(SwatchSize)
            .background(color, RoundedCornerShape(MorningDimens.radiusChip)),
    )
}

@Composable
private fun TypeLine(name: String, style: TextStyle) {
    Text(text = name, style = style, color = MorningTheme.colors.textPrimary, maxLines = 1)
}
