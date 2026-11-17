package com.lumian.morningup.feature.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import com.lumian.morningup.core.designsystem.component.MorningRadioRow
import com.lumian.morningup.core.designsystem.component.MorningSettingsCard
import com.lumian.morningup.core.designsystem.component.MorningTopBar
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.WakeCheck
import com.lumian.morningup.core.ui.AlarmText
import com.lumian.morningup.core.ui.alarmResources

/**
 * S4 다시 잠들기 방지(피그마 `170:1672`). '언제 물을까요' 1, 3, 5, 10분 후, '답할 시간' 30초, 1분, 2분.
 * 맨 위 '사용 안 함'을 고르면 꺼지고, 값을 고르면 켜져요.
 */
@Composable
fun WakeCheckPage(
    wakeCheck: WakeCheck,
    onBack: () -> Unit,
    onAsk: (Int) -> Unit,
    onAnswer: (Int) -> Unit,
    onOff: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val on = wakeCheck.enabled
    val colors = MorningTheme.colors
    val resources = alarmResources()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundBase)
            // 아래에 깔린 S0이 눌리지 않게 막아요.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .statusBarsPadding(),
    ) {
        MorningTopBar(
            title = stringResource(R.string.edit_wake_check),
            onBack = onBack,
            backContentDescription = stringResource(R.string.edit_back),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MorningDimens.layoutScreenMargin),
            verticalArrangement = Arrangement.spacedBy(MorningDimens.layoutCardGap),
        ) {
            MorningSettingsCard(title = stringResource(R.string.edit_wake_check_ask_title)) {
                MorningRadioRow(label = stringResource(R.string.edit_off_value), selected = !on, onClick = onOff)
                EditViewModel.WAKE_CHECK_ASK_MINUTES.forEach { minutes ->
                    MorningRadioRow(
                        label = stringResource(R.string.edit_wake_check_ask_option, minutes),
                        selected = on && wakeCheck.askAfterMinutes == minutes,
                        onClick = { onAsk(minutes) },
                    )
                }
            }
            MorningSettingsCard(
                title = stringResource(R.string.edit_wake_check_answer_title),
                modifier = Modifier.alpha(if (on) 1f else WAKE_OFF_ALPHA),
            ) {
                EditViewModel.WAKE_CHECK_ANSWER_SECONDS.forEach { seconds ->
                    MorningRadioRow(
                        label = AlarmText.duration(resources, seconds),
                        selected = on && wakeCheck.answerSeconds == seconds,
                        onClick = { onAnswer(seconds) },
                    )
                }
            }
            Text(
                text = stringResource(R.string.edit_wake_check_help),
                style = MorningTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = MorningDimens.space4),
            )
        }
    }
}

private const val WAKE_OFF_ALPHA = 0.45f
