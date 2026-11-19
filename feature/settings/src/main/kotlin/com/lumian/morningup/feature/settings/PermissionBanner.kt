package com.lumian.morningup.feature.settings

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.TriangleAlert
import com.lumian.morningup.core.alarm.PermissionSettings
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningShadows
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.designsystem.theme.morningShadow
import com.lumian.morningup.core.model.AlarmPermission
import com.lumian.morningup.core.model.PermissionReport

private val BannerIcon = 20.dp

/** 배너 제목과 이유. 꺼진 필수 권한 중 맨 앞 하나만 보여 줘요(설정 화면에서 나머지를 볼 수 있어요). */
private fun bannerTexts(permission: AlarmPermission): Pair<Int, Int> = when (permission) {
    AlarmPermission.ExactAlarm -> R.string.banner_exact to R.string.banner_exact_desc
    AlarmPermission.Battery -> R.string.banner_battery to R.string.banner_battery_desc
    AlarmPermission.AlarmVolume -> R.string.banner_volume to R.string.banner_volume_desc
    AlarmPermission.Notifications -> R.string.banner_notifications to R.string.banner_notifications_desc
    AlarmPermission.FullScreen -> R.string.banner_full_screen to R.string.banner_full_screen_desc
    AlarmPermission.DoNotDisturb -> R.string.permission_dnd to R.string.permission_dnd_desc
    AlarmPermission.Overlay -> R.string.permission_overlay to R.string.permission_overlay_desc
    AlarmPermission.PowerGuard -> R.string.permission_power to R.string.permission_power_desc
    AlarmPermission.Location -> R.string.permission_location to R.string.permission_location_desc
}

/**
 * H2 권한 경고 배너(피그마 `138:126`). 홈 맨 위 흰 카드에 빨간 경고 삼각형, 제목, 이유, 오른쪽 '켜기'.
 * '켜기'를 누르면 그 권한의 기기 설정 화면을 열어요. 꺼진 필수 권한이 없으면 아무것도 그리지 않아요.
 */
@Composable
fun PermissionBanner(report: PermissionReport, modifier: Modifier = Modifier) {
    val permission = report.missingRequired.firstOrNull() ?: return
    val context = LocalContext.current
    val (title, description) = bannerTexts(permission)
    val colors = MorningTheme.colors
    val shape = RoundedCornerShape(MorningDimens.radiusTile)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .morningShadow(MorningShadows.card, shape)
            .background(colors.surfaceCard, shape)
            .padding(horizontal = MorningDimens.space20, vertical = MorningDimens.space16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MorningDimens.space12),
    ) {
        Icon(Lucide.TriangleAlert, null, Modifier.size(BannerIcon), tint = colors.statusError)
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MorningTheme.typography.bodyMediumStrong, color = colors.textPrimary)
            Text(
                stringResource(description),
                style = MorningTheme.typography.captionMedium,
                color = colors.textSecondary,
            )
        }
        Text(
            text = stringResource(R.string.banner_turn_on),
            style = MorningTheme.typography.bodyMediumStrong,
            color = colors.primaryText,
            modifier = Modifier
                .morningClickable {
                    PermissionSettings.open(context, PermissionSettings.intentFor(context, permission))
                }
                .padding(MorningDimens.space8),
        )
    }
}
