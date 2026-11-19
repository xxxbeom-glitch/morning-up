package com.lumian.morningup.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningShadows
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.designsystem.theme.morningShadow
import com.lumian.morningup.core.model.PermissionBanner

private val BannerIcon = 20.dp

/** 피그마 Permission Banner 제목과 설명 사이 2. */
private val TextGap = 2.dp

/** 배너 문구(결정 120): 제목, 설명, 오른쪽 글자 버튼. */
private data class BannerTexts(val title: Int, val description: Int, val action: Int)

private fun texts(banner: PermissionBanner): BannerTexts = when (banner) {
    PermissionBanner.NotificationsAndOverlay ->
        BannerTexts(R.string.banner_both, R.string.banner_both_desc, R.string.banner_turn_on)
    PermissionBanner.Notifications ->
        BannerTexts(R.string.banner_notifications, R.string.banner_notifications_desc, R.string.banner_turn_on)
    PermissionBanner.Overlay ->
        BannerTexts(R.string.banner_overlay, R.string.banner_overlay_desc, R.string.banner_turn_on)
    PermissionBanner.FullScreen ->
        BannerTexts(R.string.banner_full_screen, R.string.banner_full_screen_desc, R.string.banner_turn_on)
    PermissionBanner.Battery ->
        BannerTexts(R.string.banner_battery, R.string.banner_battery_desc, R.string.banner_release)
}

/**
 * H2 권한 경고 배너(피그마 H2-1 `686:11729`, H2-2 `686:11777`, H2-3 `686:11825`, H2-5 `686:11921`, 다크 `816:10092` 줄).
 * 홈 맨 위 흰 카드(`surface/card`, 모서리 `radius/card`)에 빨간 칠 경고 삼각형, 제목, 설명, 오른쪽 '켜기'(배터리는 '풀기').
 * 배너 어디를 눌러도 나중용 권한 시트 PL2를 열어요([onClick], 결정 120, 129 보충 5).
 */
@Composable
fun PermissionBanner(banner: PermissionBanner, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (title, description, action) = texts(banner)
    val colors = MorningTheme.colors
    val shape = RoundedCornerShape(MorningDimens.radiusCard)
    Row(
        modifier = modifier
            .testTag(PERMISSION_BANNER_TAG)
            .fillMaxWidth()
            .morningShadow(MorningShadows.card, shape)
            .background(colors.surfaceCard, shape)
            .morningClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = MorningDimens.space20, vertical = MorningDimens.space16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MorningDimens.space12),
    ) {
        // 피그마 icon/triangle-alert: status/error 칠 삼각형 안에 카드색 느낌표.
        Box(Modifier.size(BannerIcon)) {
            Icon(painterResource(R.drawable.ic_banner_alert_shape), null, tint = colors.statusError)
            Icon(painterResource(R.drawable.ic_banner_alert_mark), null, tint = colors.surfaceCard)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TextGap)) {
            Text(stringResource(title), style = MorningTheme.typography.bodyMediumStrong, color = colors.textPrimary)
            Text(
                stringResource(description),
                style = MorningTheme.typography.captionMedium,
                color = colors.textSecondary,
            )
        }
        Text(
            text = stringResource(action),
            style = MorningTheme.typography.bodyMediumStrong,
            color = colors.primaryText,
        )
    }
}

const val PERMISSION_BANNER_TAG = "permissionBanner"
