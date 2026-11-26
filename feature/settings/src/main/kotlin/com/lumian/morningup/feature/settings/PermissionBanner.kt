package com.lumian.morningup.feature.settings

import android.content.Context
import android.content.Intent
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
import com.lumian.morningup.core.alarm.PermissionSettings
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningShadows
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.designsystem.theme.morningShadow
import com.lumian.morningup.core.model.AlarmPermission
import com.lumian.morningup.core.model.PermissionBanner

private val BannerIcon = 20.dp

/** 피그마 Permission Banner 제목과 설명 사이 2. */
private val TextGap = 2.dp

/** 배너 문구(결정 120): 제목, 설명, 오른쪽 글자 버튼. */
private data class BannerTexts(val title: Int, val description: Int, val action: Int)

private fun texts(banner: PermissionBanner): BannerTexts = when (banner) {
    PermissionBanner.BatteryRestricted ->
        BannerTexts(R.string.banner_restricted, R.string.banner_restricted_desc, R.string.banner_turn_on)
    PermissionBanner.AlarmsSilenced ->
        BannerTexts(R.string.banner_silenced, R.string.banner_silenced_desc, R.string.banner_view)
    PermissionBanner.ExactAlarm ->
        BannerTexts(R.string.banner_exact, R.string.banner_exact_desc, R.string.banner_turn_on)
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
 * 배너 어디를 눌러도 나중용 권한 시트 PL2를 열어요([onClick], 결정 120, 129 보충 5). 시트가 없는 배너는
 * [openBannerTarget]으로 기기 설정을 열어요. 더 걸린 것이 있으면 설명 끝에 '그 밖에 n개 더'([more], 시안 H2-6).
 */
@Composable
fun PermissionBanner(banner: PermissionBanner, onClick: () -> Unit, modifier: Modifier = Modifier, more: Int = 0) {
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
                if (more > 0) {
                    stringResource(R.string.banner_more, stringResource(description), more)
                } else {
                    stringResource(description)
                },
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

/**
 * 시트가 없는 배너를 눌렀을 때 여는 화면. 배터리 '제한'은 앱 정보(배터리 항목이 거기 있어요), 정확한 알람은
 * 그 권한 화면, 방해 금지는 기기 방해 금지 설정이에요(T2-1 안내 화면이 생기면 그리로 바꿔요).
 * 열 것이 없거나 못 열면 false예요.
 */
fun openBannerTarget(context: Context, banner: PermissionBanner): Boolean {
    val intent = when (banner) {
        PermissionBanner.BatteryRestricted -> PermissionSettings.appDetails(context)
        PermissionBanner.ExactAlarm -> PermissionSettings.intentFor(context, AlarmPermission.ExactAlarm)
        PermissionBanner.AlarmsSilenced -> Intent(ZEN_MODE_SETTINGS)
        else -> return false
    }
    return PermissionSettings.open(context, intent)
}

/** 기기 방해 금지 설정 화면(공개 상수가 없어 이름으로 열어요. 없으면 앱 정보로 가요). */
private const val ZEN_MODE_SETTINGS = "android.settings.ZEN_MODE_SETTINGS"
