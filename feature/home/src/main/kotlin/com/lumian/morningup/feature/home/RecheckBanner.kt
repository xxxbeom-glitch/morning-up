package com.lumian.morningup.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Timer
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningShadows
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.designsystem.theme.morningShadow

private val BannerIcon = 20.dp
private val TextGap = 2.dp

/**
 * H0 위쪽 띠 '한 번 더 확인이 남았어요'(결정 145, 시안 tsk-163 h0-recheck). 권한 배너(H2)와 같은 카드 모양에
 * 기본 안내색(primary) 타이머 아이콘이에요. 어디를 눌러도 W3를 바로 열어 지금 끝낼 수 있어요.
 */
@Composable
fun RecheckBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MorningTheme.colors
    val shape = RoundedCornerShape(MorningDimens.radiusCard)
    Row(
        modifier = modifier
            .testTag(RECHECK_BANNER_TAG)
            .fillMaxWidth()
            .morningShadow(MorningShadows.card, shape)
            .background(colors.surfaceCard, shape)
            .morningClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = MorningDimens.space20, vertical = MorningDimens.space16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MorningDimens.space12),
    ) {
        Icon(Lucide.Timer, null, tint = colors.primaryDefault, modifier = Modifier.size(BannerIcon))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TextGap)) {
            Text(
                stringResource(R.string.home_recheck_title),
                style = MorningTheme.typography.bodyMediumStrong,
                color = colors.textPrimary,
            )
            Text(
                stringResource(R.string.home_recheck_desc),
                style = MorningTheme.typography.captionMedium,
                color = colors.textSecondary,
            )
        }
        Text(
            text = stringResource(R.string.home_recheck_action),
            style = MorningTheme.typography.bodyMediumStrong,
            color = colors.primaryText,
        )
    }
}

/** W3가 남았으면 띠를 권한 배너([banner]) 위에 얹어요. */
internal fun withRecheckBanner(
    pending: Boolean,
    onOpen: () -> Unit,
    banner: (@Composable () -> Unit)?,
): (@Composable () -> Unit)? {
    if (!pending) return banner
    return {
        Column(verticalArrangement = Arrangement.spacedBy(MorningDimens.space12)) {
            RecheckBanner(onClick = onOpen)
            banner?.invoke()
        }
    }
}

const val RECHECK_BANNER_TAG = "recheckBanner"
