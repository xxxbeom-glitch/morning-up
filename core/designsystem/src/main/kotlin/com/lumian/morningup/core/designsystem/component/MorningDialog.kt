package com.lumian.morningup.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningFixedColors
import com.lumian.morningup.core.designsystem.theme.MorningShadows
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.designsystem.theme.morningShadow

/** 다이얼로그 최대 폭. 360 폭 화면에서 좌우 16을 뺀 328(11-16 카드 폭과 같게)이고, 큰 화면에서도 이 이상 넓히지 않아요. */
private val DialogMaxWidth = 328.dp

/** 나타날 때 시작 크기(dev-plan '모션과 인터랙션': 0.94배에서 1배로). */
private const val DIALOG_START_SCALE = 0.94f

/**
 * 공용 Dialog(`137:709`). 가운데 정렬 제목과 본문, 버튼 두 개(왼쪽 보조, 오른쪽 확인). 아이콘은 넣지 않아요.
 * 뒤 막은 `#14202E` 40%, 나타날 때 0.94배에서 커지며 페이드 인(`spatialFast`).
 * [danger]가 true면 확인 버튼이 빨강(Button Alt Danger)이에요.
 * [onDismissRequest]는 막을 누르거나 뒤로 갈 때예요. 안 주면 [onDismiss]와 같아요(보조 버튼이 '닫기'가 아닐 때 따로 줘요).
 */
@Composable
fun MorningDialog(
    title: String,
    message: String,
    dismissLabel: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    onDismissRequest: () -> Unit = onDismiss,
) {
    // 다이얼로그는 따로 뜨는 창이라 기기 원래 밀도로 돌아가요. 시안 폭 맞춤과 글꼴 상한이 적용된 밀도를 그대로 넘겨요.
    val density = LocalDensity.current
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        CompositionLocalProvider(LocalDensity provides density) {
            DialogBody(
                DialogTexts(title, message, dismissLabel, confirmLabel),
                onDismiss,
                onConfirm,
                onDismissRequest,
                modifier,
                danger,
            )
        }
    }
}

private data class DialogTexts(val title: String, val message: String, val dismiss: String, val confirm: String)

@Composable
private fun DialogBody(
    texts: DialogTexts,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    danger: Boolean,
) {
    run {
        // 시스템 기본 어둡게 하기 대신 디자인의 막 색을 직접 그려요.
        (LocalView.current.parent as? DialogWindowProvider)?.window?.setDimAmount(0f)
        val reduceMotion = MorningTheme.reduceMotion
        val progress = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            progress.animateTo(1f, MorningMotion.spatial(MorningMotion.SpatialFast, reduceMotion))
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value.coerceIn(0f, 1f) }
                .background(MorningFixedColors.Scrim)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    onDismissRequest()
                },
            contentAlignment = Alignment.Center,
        ) {
            DialogCard(
                title = texts.title,
                message = texts.message,
                dismissLabel = texts.dismiss,
                confirmLabel = texts.confirm,
                onDismiss = onDismiss,
                onConfirm = onConfirm,
                danger = danger,
                modifier = modifier.graphicsLayer {
                    val scale = DIALOG_START_SCALE + (1f - DIALOG_START_SCALE) * progress.value
                    scaleX = scale
                    scaleY = scale
                },
            )
        }
    }
}

@Composable
private fun DialogCard(
    title: String,
    message: String,
    dismissLabel: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    danger: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    val shape = RoundedCornerShape(MorningDimens.radiusCard)
    Column(
        modifier = modifier
            .padding(horizontal = MorningDimens.layoutScreenMargin)
            .widthIn(max = DialogMaxWidth)
            .fillMaxWidth()
            .morningShadow(MorningShadows.float, shape)
            .background(colors.backgroundElevated, shape)
            // 카드 안을 눌러도 뒤 막의 '닫기'가 불리지 않게 막아요.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .semantics { paneTitle = title }
            .padding(MorningDimens.layoutCardPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = typography.titleMedium, color = colors.textPrimary, textAlign = TextAlign.Center)
        // 피그마 Dialog(`137:709`): 제목과 본문 사이 8, 본문 Body/Medium 14.
        Spacer(Modifier.height(MorningDimens.space8))
        Text(text = message, style = typography.bodyMedium, color = colors.textSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(MorningDimens.space24))
        Row(horizontalArrangement = Arrangement.spacedBy(MorningDimens.space8)) {
            MorningButton(
                text = dismissLabel,
                onClick = onDismiss,
                size = MorningButtonSize.Medium,
                style = MorningButtonStyle.Secondary,
                haptic = MorningHaptic.Press,
                modifier = Modifier.weight(1f),
            )
            MorningButton(
                text = confirmLabel,
                onClick = onConfirm,
                size = MorningButtonSize.Medium,
                style = if (danger) MorningButtonStyle.Danger else MorningButtonStyle.Primary,
                haptic = if (danger) MorningHaptic.Confirm else MorningHaptic.Press,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
