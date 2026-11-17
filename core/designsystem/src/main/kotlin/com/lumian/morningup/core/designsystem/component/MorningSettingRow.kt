package com.lumian.morningup.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme

/** 피그마 Setting Row State. Disabled(`405:753`)는 라벨과 값 모두 흐리고 토글을 누를 수 없어요. */
enum class SettingRowState { On, Off, Disabled }

/**
 * 피그마 Setting Row. 왼쪽 라벨(`Body/Medium` `text/body`)과 그 아래 4 띄운 값, 오른쪽 작은 토글.
 * 값 글자는 켜지면 `text/secondary`, 꺼지면 `text/disabled`이고 파란색은 쓰지 않아요.
 * 줄(라벨 쪽)을 누르면 [onClick]으로 하위 설정 화면을 열어요.
 */
@Composable
fun MorningSettingRow(
    label: String,
    value: String?,
    state: SettingRowState,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    val enabled = state != SettingRowState.Disabled
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MorningDimens.sizeRow),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (onClick != null && enabled) {
                        Modifier.morningClickable(haptic = null, onClick = onClick)
                    } else {
                        Modifier
                    },
                )
                .padding(end = MorningDimens.space12),
        ) {
            Text(
                text = label,
                style = typography.bodyMedium,
                color = if (enabled) colors.textBody else colors.textDisabled,
            )
            if (value != null) {
                Spacer(Modifier.height(MorningDimens.space4))
                Text(
                    text = value,
                    style = typography.captionMedium,
                    color = if (state == SettingRowState.On) colors.textSecondary else colors.textDisabled,
                )
            }
        }
        MorningToggle(
            checked = state == SettingRowState.On,
            onCheckedChange = onCheckedChange,
            size = MorningToggleSize.Small,
            enabled = enabled,
        )
    }
}
