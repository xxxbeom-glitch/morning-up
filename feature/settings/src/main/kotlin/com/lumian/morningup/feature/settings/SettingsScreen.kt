package com.lumian.morningup.feature.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.TriangleAlert
import com.lumian.morningup.core.alarm.PermissionSettings
import com.lumian.morningup.core.designsystem.component.MorningMessageHost
import com.lumian.morningup.core.designsystem.component.MorningSettingsCard
import com.lumian.morningup.core.designsystem.component.MorningToast
import com.lumian.morningup.core.designsystem.component.MorningTopBar
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.PermissionReport
import com.lumian.morningup.core.model.PermissionScreen
import java.time.LocalTime

/** 하단 메뉴가 덮는 높이. 마지막 카드와 메뉴 사이 24를 더해요(design-system T1). */
private val BottomNavSpace = 120.dp
private val ChevronSize = 16.dp
private val AlertSize = 16.dp
private val NavRowGap = 5.dp

/** 의견 보내기 받는 주소(design-system T1, 11-16 사용자 결정). */
private const val FEEDBACK_EMAIL = "lumianthecompany@gmail.com"

/**
 * T1 설정(피그마 `141:1293`). 카드: 알람, 도움말, 앱 정보.
 * 날씨 애니메이션 카드는 9단계, 자주 묻는 질문과 개인정보 처리방침, 오픈소스 라이선스는 내용이 생기면 더해요.
 * [extra]는 디버그 빌드에서만 맨 아래에 붙는 개발용 도구예요.
 */
@Composable
fun SettingsRoute(
    versionName: String,
    onOpenPermissions: (PermissionScreen) -> Unit,
    modifier: Modifier = Modifier,
    extra: @Composable () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val report by viewModel.report.collectAsStateWithLifecycle()
    val tryAt by viewModel.tryAlarmAt.collectAsStateWithLifecycle()
    val context = LocalContext.current
    SettingsScreen(
        report = report,
        versionName = versionName,
        tryAlarmAt = tryAt,
        onOpenPermissions = onOpenPermissions,
        onTryAlarm = viewModel::tryAlarm,
        onFeedback = { sendFeedback(context, versionName, report) },
        modifier = modifier,
        extra = extra,
    )
}

@Composable
fun SettingsScreen(
    report: PermissionReport,
    versionName: String,
    tryAlarmAt: LocalTime?,
    onOpenPermissions: (PermissionScreen) -> Unit,
    onTryAlarm: () -> Unit,
    onFeedback: () -> Unit,
    modifier: Modifier = Modifier,
    extra: @Composable () -> Unit = {},
) {
    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            MorningTopBar(title = stringResource(R.string.settings_title))
            Column(
                modifier = Modifier.padding(
                    start = MorningDimens.layoutScreenMargin,
                    end = MorningDimens.layoutScreenMargin,
                    top = MorningDimens.space16,
                ),
                verticalArrangement = Arrangement.spacedBy(MorningDimens.layoutCardGap),
            ) {
                MorningSettingsCard(title = stringResource(R.string.settings_alarm_card)) {
                    NavRow(
                        label = stringResource(R.string.settings_alarm_ring),
                        alert = report.needsAttention(PermissionScreen.Ring),
                        onClick = { onOpenPermissions(PermissionScreen.Ring) },
                    )
                    NavRow(
                        label = stringResource(R.string.settings_alarm_screen),
                        alert = report.needsAttention(PermissionScreen.Screen),
                        onClick = { onOpenPermissions(PermissionScreen.Screen) },
                    )
                    NavRow(
                        label = stringResource(R.string.settings_alarm_try),
                        value = stringResource(R.string.settings_alarm_try_value),
                        onClick = onTryAlarm,
                    )
                }
                MorningSettingsCard(title = stringResource(R.string.settings_help_card)) {
                    NavRow(label = stringResource(R.string.settings_feedback), onClick = onFeedback)
                }
                MorningSettingsCard(title = stringResource(R.string.settings_info_card)) {
                    NavRow(label = stringResource(R.string.settings_version), value = versionName, onClick = null)
                }
                extra()
            }
            Spacer(Modifier.navigationBarsPadding().height(BottomNavSpace))
        }
        MorningMessageHost(
            message = tryAlarmAt,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = BottomNavSpace),
        ) { time ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                MorningToast(stringResource(R.string.settings_alarm_try_done, time.toString()))
            }
        }
    }
}

/**
 * 피그마 Nav Row. 한 줄 44(`size/row-plain`), 오른쪽에 값과 chevron-right 16(사이 5).
 * [alert]면 값 대신 빨간 경고 삼각형 16을 켜요. [onClick]이 없으면 화살표 없이 값만 보여요(버전).
 */
@Composable
private fun NavRow(label: String, onClick: (() -> Unit)?, value: String? = null, alert: Boolean = false) {
    val colors = MorningTheme.colors
    val alertText = stringResource(R.string.settings_needs_attention)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(MorningDimens.sizeRowPlain)
            .then(if (onClick != null) Modifier.morningClickable(haptic = null, onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NavRowGap),
    ) {
        Text(
            text = label,
            style = MorningTheme.typography.bodyMedium,
            color = colors.textBody,
            modifier = Modifier.weight(1f),
        )
        if (alert) {
            Icon(Lucide.TriangleAlert, alertText, Modifier.size(AlertSize), tint = colors.statusError)
        } else if (value != null) {
            Text(text = value, style = MorningTheme.typography.captionMedium, color = colors.textSecondary)
        }
        if (onClick != null) {
            Icon(Lucide.ChevronRight, null, Modifier.size(ChevronSize), tint = colors.textTertiary)
        }
    }
}

/** 메일 앱을 열고 기기 정보와 꺼진 권한을 본문에 미리 채워요. 로그인은 없어요. */
private fun sendFeedback(context: Context, versionName: String, report: PermissionReport) {
    val missing = report.missingRequired.joinToString { it.name }
        .ifEmpty { context.getString(R.string.settings_feedback_none) }
    val body = context.getString(
        R.string.settings_feedback_body,
        "${Build.MANUFACTURER} ${Build.MODEL}",
        Build.VERSION.RELEASE,
        versionName,
        missing,
    )
    val intent = Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
        .putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
        .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.settings_feedback_subject))
        .putExtra(Intent.EXTRA_TEXT, body)
    PermissionSettings.open(context, intent)
}
