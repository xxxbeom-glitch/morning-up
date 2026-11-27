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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.AlarmClock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Timer
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningShadows
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.designsystem.theme.morningShadow
import com.lumian.morningup.core.ui.AlarmText
import com.lumian.morningup.core.ui.alarmResources
import java.time.LocalTime

private val BannerIcon = 20.dp
private val TextGap = 2.dp

/**
 * H0 위쪽 띠에 무엇을 보일지. 울림 단계는 한 번에 하나라 띠도 하나예요.
 * - [Recheck]: 한 번 더 확인(W3)을 기다려요(결정 145).
 * - [Resting]: 다시 울림 사이에 쉬는 중이에요(결정 165). [at]에 다시 울려요. [snoozed]면 미루기로 쉬는 중(결정 195).
 * - [WakeCheckSoon]: 미션 끝 뒤 W2를 기다려요(결정 168). 글만 보여요. [at]은 W2가 뜰 시각이에요(TSK-201).
 */
sealed interface HomeStatus {
    data class Recheck(val left: Int? = null) : HomeStatus

    data class Resting(val at: LocalTime, val snoozed: Boolean = false) : HomeStatus

    data class WakeCheckSoon(val at: LocalTime? = null) : HomeStatus
}

/**
 * 홈 위쪽 띠 한 줄(시안 tsk-163 h0-recheck). 권한 배너(H2)와 같은 카드 모양에 기본 안내색(primary) 타이머 아이콘이에요.
 * [onClick]이 null이면 눌러도 아무 일 없어요.
 */
@Composable
internal fun StatusBanner(
    title: String,
    tag: String,
    modifier: Modifier = Modifier,
    desc: String? = null,
    action: String? = null,
    onClick: (() -> Unit)? = null,
    icon: ImageVector = Lucide.Timer,
    iconTint: Color? = null,
) {
    val colors = MorningTheme.colors
    val shape = RoundedCornerShape(MorningDimens.radiusCard)
    val base = modifier
        .testTag(tag)
        .fillMaxWidth()
        .morningShadow(MorningShadows.card, shape)
        .background(colors.surfaceCard, shape)
    Row(
        modifier = (if (onClick != null) base.morningClickable(role = Role.Button, onClick = onClick) else base)
            .padding(horizontal = MorningDimens.space20, vertical = MorningDimens.space16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MorningDimens.space12),
    ) {
        Icon(icon, null, tint = iconTint ?: colors.primaryDefault, modifier = Modifier.size(BannerIcon))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TextGap)) {
            Text(title, style = MorningTheme.typography.bodyMediumStrong, color = colors.textPrimary)
            if (desc != null) {
                Text(desc, style = MorningTheme.typography.captionMedium, color = colors.textSecondary)
            }
        }
        if (action != null) {
            Text(text = action, style = MorningTheme.typography.bodyMediumStrong, color = colors.primaryText)
        }
    }
}

/** H0 위쪽 띠 '한 번 더 확인이 남았어요'(결정 145). 어디를 눌러도 W3를 바로 열어 지금 끝낼 수 있어요. */
@Composable
fun RecheckBanner(onClick: () -> Unit, modifier: Modifier = Modifier, left: Int? = null) {
    StatusBanner(
        // 실험실 여러 번 묻기면 남은 횟수를 붙이고, 한 번에 하나만 끝낼 수 있어 '하나'를 더해요(결정 146).
        title = if (left != null) {
            stringResource(R.string.home_recheck_title_count, left)
        } else {
            stringResource(R.string.home_recheck_title)
        },
        desc = stringResource(if (left != null) R.string.home_recheck_desc_one else R.string.home_recheck_desc),
        action = stringResource(R.string.home_recheck_action),
        onClick = onClick,
        tag = RECHECK_BANNER_TAG,
        modifier = modifier,
    )
}

/**
 * 쉬는 중 띠 '오전 6:40에 다시 울려요 / 지금 미션을 하면 바로 끝나요 / 미션 하기'(결정 165, 시안 TSK-201).
 * 누르면 쉬는 중 알림과 같이 울림 화면이 열려요.
 */
@Composable
fun RestingBanner(at: LocalTime, onClick: () -> Unit, modifier: Modifier = Modifier, snoozed: Boolean = false) {
    val time = AlarmText.time(alarmResources(), at)
    StatusBanner(
        title = stringResource(R.string.home_resting_band, time),
        desc = stringResource(if (snoozed) R.string.home_snoozed_band_desc else R.string.home_resting_band_desc),
        action = stringResource(R.string.home_resting_band_action),
        icon = Lucide.AlarmClock,
        iconTint = MorningTheme.colors.statusError,
        onClick = onClick,
        tag = RESTING_BANNER_TAG,
        modifier = modifier,
    )
}

/** W2 대기 띠 '곧 깼는지 물어볼게요 / 오전 6:43쯤 확인 화면이 떠요'(결정 168, 시안 TSK-201). 눌러도 아무 일 없어요. */
@Composable
fun WakeCheckSoonBanner(modifier: Modifier = Modifier, at: LocalTime? = null) {
    StatusBanner(
        title = stringResource(R.string.home_wake_check_soon_band),
        desc = at?.let { stringResource(R.string.home_wake_check_soon_desc, AlarmText.time(alarmResources(), it)) },
        tag = WAKE_CHECK_SOON_BANNER_TAG,
        modifier = modifier,
    )
}

/** 띠가 있으면 권한 배너([banner]) 위에 얹어요. */
internal fun withStatusBanner(
    status: HomeStatus?,
    onOpenRecheck: () -> Unit,
    onOpenRest: () -> Unit,
    banner: (@Composable () -> Unit)?,
): (@Composable () -> Unit)? {
    if (status == null) return banner
    return {
        Column(verticalArrangement = Arrangement.spacedBy(MorningDimens.space12)) {
            when (status) {
                is HomeStatus.Recheck -> RecheckBanner(onClick = onOpenRecheck, left = status.left)
                is HomeStatus.Resting -> RestingBanner(at = status.at, onClick = onOpenRest, snoozed = status.snoozed)
                is HomeStatus.WakeCheckSoon -> WakeCheckSoonBanner(at = status.at)
            }
            banner?.invoke()
        }
    }
}

const val RECHECK_BANNER_TAG = "recheckBanner"
const val RESTING_BANNER_TAG = "restingBanner"
const val WAKE_CHECK_SOON_BANNER_TAG = "wakeCheckSoonBanner"
