package com.lumian.morningup.feature.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lumian.morningup.core.alarm.PermissionSettings
import com.lumian.morningup.core.designsystem.component.MorningButton
import com.lumian.morningup.core.designsystem.component.MorningDialog
import com.lumian.morningup.core.designsystem.component.MorningSettingsCard
import com.lumian.morningup.core.designsystem.component.MorningTopBar
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme

private val StepCircle = 24.dp
private val GuideSteps = listOf(R.string.power_guide_step1, R.string.power_guide_step2, R.string.power_guide_step3)

/**
 * P6 권한 동의: 전원 종료 방지(접근성). 구글플레이 접근성 권한 안내용으로, 무엇에 쓰는지 알리고 동의를 받아요.
 * '동의하기'를 누르면 P7 안내 화면으로 가요.
 */
@Composable
fun PowerGuardConsentDialog(onDismiss: () -> Unit, onAgree: () -> Unit) {
    MorningDialog(
        title = stringResource(R.string.power_consent_title),
        message = stringResource(R.string.power_consent_msg),
        dismissLabel = stringResource(R.string.ask_later),
        confirmLabel = stringResource(R.string.power_consent_agree),
        onDismiss = onDismiss,
        onConfirm = onAgree,
    )
}

/**
 * P7 접근성 켜기 안내(피그마 `402:11407`). 카드 '접근성에서 켜 주세요' 3단계와 하단 버튼 '접근성 설정 열기'.
 * 피그마 맨 위 '이렇게 찾아요' 스켈레톤 움직임 카드는 장면 다듬기가 남아 있어 아직 넣지 않았어요(decisions-11-17).
 */
@Composable
fun PowerGuardGuidePage(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MorningTheme.colors.backgroundBase)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        MorningTopBar(
            title = stringResource(R.string.power_guide_title),
            onBack = onBack,
            backContentDescription = stringResource(R.string.settings_back),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = MorningDimens.layoutScreenMargin,
                    end = MorningDimens.layoutScreenMargin,
                    top = MorningDimens.space16,
                ),
        ) {
            MorningSettingsCard(title = stringResource(R.string.power_guide_card)) {
                Column(verticalArrangement = Arrangement.spacedBy(MorningDimens.space16)) {
                    GuideSteps.forEachIndexed { index, step -> Step(index + 1, stringResource(step)) }
                }
            }
        }
        MorningButton(
            text = stringResource(R.string.power_guide_open),
            onClick = { PermissionSettings.open(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(MorningDimens.layoutScreenMargin),
        )
    }
}

@Composable
private fun Step(number: Int, text: String) {
    val colors = MorningTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MorningDimens.space12),
    ) {
        Box(
            modifier = Modifier
                .size(StepCircle)
                .background(colors.primarySubtle, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(number.toString(), style = MorningTheme.typography.captionStrong, color = colors.primaryText)
        }
        Text(text, style = MorningTheme.typography.bodyMedium, color = colors.textBody)
    }
}
