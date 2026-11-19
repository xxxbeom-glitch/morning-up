package com.lumian.morningup.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningFixedColors
import com.lumian.morningup.core.designsystem.theme.MorningShadows
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.designsystem.theme.morningShadow

/** 피그마 Dialog Sheet(709:17634) 그림 칸 120, 모서리 28. */
private val PictureSize = 120.dp
private val PictureCorner = 28.dp

/** 다크 화면 뒤 막: 검정 60%(결정 134). 라이트는 [MorningFixedColors.Scrim]. */
private val DarkScrim = Color.Black.copy(alpha = 0.6f)

/** 바탕 밝기가 이보다 낮으면 다크 화면으로 봐요. */
private const val DARK_LUMINANCE = 0.5f

/**
 * 결정을 받는 모달 바텀시트(피그마 Dialog Sheet, Handle=Off). 앱 맨 위 [MorningOverlayRoot] 자리에 그려서 화면 전체(하단 메뉴 포함) 위에 막을 깔고 아래에서 올라와요.
 * 손잡이가 없고, 막을 누르거나 끌어 내려도 닫히지 않아요. 뒤로 가기와 [laterLabel] 글은 [onLater]를 불러요.
 * 그림 칸([picture])은 120x120 `background/base` 칸이에요. 비워 두면 빈 칸으로 보여요.
 */
@Composable
fun MorningDialogSheet(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    laterLabel: String,
    onLater: () -> Unit,
    tag: String = "dialogSheet",
    picture: @Composable BoxScope.() -> Unit = {},
) {
    val sheet: @Composable () -> Unit = {
        BackHandler(onBack = onLater)
        DialogSheetContent(title, message, confirmLabel, onConfirm, laterLabel, onLater, tag, picture)
    }
    val host = LocalMorningOverlay.current
    if (host == null) {
        sheet()
    } else {
        // 앱 맨 위 [MorningOverlayRoot]에 그려서 하단 메뉴와 시스템 바 뒤까지 막이 덮여요.
        val latest by rememberUpdatedState(sheet)
        DisposableEffect(host) {
            val entry: @Composable () -> Unit = { latest() }
            host.show(entry)
            onDispose { host.hide(entry) }
        }
    }
}

/** 화면 맨 위에 덮어 그릴 것(결정 시트)을 받아 두는 자리. [MorningOverlayRoot]가 만들어요. */
class MorningOverlayHost internal constructor() {
    internal var content by mutableStateOf<(@Composable () -> Unit)?>(null)
        private set

    internal fun show(entry: @Composable () -> Unit) {
        content = entry
    }

    internal fun hide(entry: @Composable () -> Unit) {
        if (content === entry) content = null
    }
}

val LocalMorningOverlay = staticCompositionLocalOf<MorningOverlayHost?> { null }

/** 앱 전체를 감싸고, [MorningDialogSheet] 같은 덮개를 모든 화면(하단 메뉴 포함) 위에 그려요. */
@Composable
fun MorningOverlayRoot(content: @Composable () -> Unit) {
    val host = remember { MorningOverlayHost() }
    CompositionLocalProvider(LocalMorningOverlay provides host) {
        Box(Modifier.fillMaxSize()) {
            content()
            host.content?.invoke()
        }
    }
}

@Composable
internal fun DialogSheetContent(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    laterLabel: String,
    onLater: () -> Unit,
    tag: String,
    picture: @Composable BoxScope.() -> Unit,
) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    val reduceMotion = MorningTheme.reduceMotion
    val shown = remember { MutableTransitionState(false).apply { targetState = true } }
    val scrim = if (colors.backgroundBase.luminance() < DARK_LUMINANCE) DarkScrim else MorningFixedColors.Scrim
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visibleState = shown, enter = fadeIn(MorningMotion.effects(MorningMotion.EffectsDefault))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(scrim)
                    // 막을 눌러도 닫지 않아요(결정을 받아야 하는 시트).
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            )
        }
        AnimatedVisibility(
            visibleState = shown,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(MorningMotion.spatial<IntOffset>(MorningMotion.SpatialDefault, reduceMotion)) {
                it
            },
        ) {
            val shape = RoundedCornerShape(topStart = MorningDimens.radiusCard, topEnd = MorningDimens.radiusCard)
            Column(
                modifier = Modifier
                    .testTag(tag)
                    .fillMaxWidth()
                    .morningShadow(MorningShadows.float, shape)
                    .background(colors.backgroundElevated, shape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .navigationBarsPadding()
                    .padding(top = MorningDimens.space32),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(PictureSize)
                        .background(colors.backgroundBase, RoundedCornerShape(PictureCorner)),
                    contentAlignment = Alignment.Center,
                    content = picture,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = MorningDimens.space24,
                            end = MorningDimens.space24,
                            top = MorningDimens.space24,
                        ),
                    verticalArrangement = Arrangement.spacedBy(MorningDimens.space8),
                ) {
                    Text(
                        text = title,
                        style = typography.titleLarge,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = message,
                        style = typography.bodyLarge,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = MorningDimens.space16,
                            end = MorningDimens.space16,
                            top = MorningDimens.space24,
                            bottom = MorningDimens.space16,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MorningDimens.space4),
                ) {
                    MorningButton(text = confirmLabel, onClick = onConfirm, modifier = Modifier.fillMaxWidth())
                    Text(
                        text = laterLabel,
                        style = typography.bodyMediumStrong,
                        color = colors.textTertiary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(MorningDimens.space8))
                            .morningClickable(onClick = onLater)
                            .padding(horizontal = MorningDimens.space16, vertical = MorningDimens.space12),
                    )
                }
            }
        }
    }
}
