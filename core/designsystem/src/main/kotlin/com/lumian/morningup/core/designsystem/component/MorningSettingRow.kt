package com.lumian.morningup.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme

private val ChevronSize = 20.dp

/**
 * 피그마 Setting Row State. Disabled(`405:753`)는 라벨과 값 모두 흐리고 토글을 누를 수 없어요.
 * Navigate는 토글 대신 오른쪽에 '>'를 두고, 눌러서 하위 화면으로 들어가기만 하는 줄이에요(값은 늘 켜짐 색).
 */
enum class SettingRowState { On, Off, Disabled, Navigate }

/**
 * 피그마 Setting Row. 왼쪽 라벨(`Body/Medium` `text/body`)과 그 아래 4 띄운 값, 오른쪽 작은 토글.
 * 값 글자는 켜지면 `text/secondary`, 꺼지면 `text/disabled`이고 파란색은 쓰지 않아요.
 * 줄 어디를 눌러도 반응해요. [onClick]이 있으면 하위 설정 화면을 열고, 없으면 토글을 바꿔요.
 * [toggleLocked]면 토글만 투명도 0.45로 잠겨 누를 수 없고, 라벨과 값은 그대로예요(T1 '한 번 더 확인', 결정 146).
 */
@Composable
fun MorningSettingRow(
    label: String,
    value: String?,
    state: SettingRowState,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    toggleLocked: Boolean = false,
) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    val enabled = state != SettingRowState.Disabled
    val checked = state == SettingRowState.On
    val toggleEnabled = enabled && !toggleLocked
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MorningDimens.sizeRow)
            .morningClickable(enabled = enabled, haptic = null) {
                if (onClick != null) onClick() else if (toggleEnabled) onCheckedChange(!checked)
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
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
                    color = if (state == SettingRowState.On || state == SettingRowState.Navigate) {
                        colors.textSecondary
                    } else {
                        colors.textDisabled
                    },
                )
            }
        }
        if (state == SettingRowState.Navigate) {
            Icon(
                imageVector = Lucide.ChevronRight,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(ChevronSize),
            )
        } else {
            MorningToggle(
                checked = checked,
                onCheckedChange = onCheckedChange,
                size = MorningToggleSize.Small,
                enabled = toggleEnabled,
            )
        }
    }
}
