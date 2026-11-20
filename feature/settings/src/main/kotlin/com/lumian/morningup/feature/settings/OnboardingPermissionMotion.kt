package com.lumian.morningup.feature.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.lumian.morningup.core.designsystem.theme.MorningShadows
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.designsystem.theme.morningShadow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.lumian.morningup.feature.settings.OnboardingMotionTokens as T

/** 피그마 GENTLE 스프링(질량 1, 강성 100, 감쇠 15). */
internal fun gentle(): AnimationSpec<Float> =
    spring(dampingRatio = T.GENTLE_DAMPING_RATIO, stiffness = T.GENTLE_STIFFNESS)

/** 누름 원. (cx, cy)를 가운데로 36에서 44로 커지며 [alpha]만큼 보여요. */
@Composable
private fun TapCircle(centerX: Float, centerY: Float, grow: Float, alpha: Float) {
    if (alpha <= 0f) return
    val diameter = lerp(T.TAP_FROM, T.TAP_TO, grow)
    Box(
        Modifier
            .figma(FigmaRect(centerX - diameter / 2, centerY - diameter / 2, diameter, diameter))
            .background(MorningTheme.colors.textPrimary.copy(alpha = alpha.coerceIn(0f, 1f)), CircleShape),
    )
}

// ---------------------------------------------------------------- Motion Battery Dialog (O2-3)

/**
 * 온보딩 전용 배터리 최적화 해제 대화 상자(11-20 사용자 지시). 0.2초 뒤 16 아래에서 GENTLE 0.6초로 떠오르며 나타나고,
 * 0.6초 쉰 뒤 '허용'을 눌러(0.15초) 0.1초 뒤 대화 상자가 12 아래로 내려가며 사라져요(GENTLE 0.5초).
 * 0.8초 뒤 다시 떠올라요(GENTLE 0.5초). 등장은 처음 한 번만이에요.
 */
@Composable
internal fun MotionBatteryDialog(modifier: Modifier = Modifier) {
    val reduceMotion = MorningTheme.reduceMotion
    val y = remember { Animatable(T.DIALOG_ENTER_Y) }
    val shown = remember { Animatable(0f) }
    val pressed = remember { Animatable(0f) }
    val tapAlpha = remember { Animatable(0f) }
    val tapGrow = remember { Animatable(0f) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            y.snapTo(T.DIALOG_Y)
            shown.snapTo(1f)
            pressed.snapTo(0f)
            tapAlpha.snapTo(0f)
            return@LaunchedEffect
        }
        y.snapTo(T.DIALOG_ENTER_Y)
        shown.snapTo(0f)
        delay(T.ENTER_WAIT)
        coroutineScope {
            launch { y.animateTo(T.DIALOG_Y, gentle()) }
            launch { shown.animateTo(1f, gentle()) }
            delay(T.ENTER_RISE)
        }
        while (true) {
            delay(T.DIALOG_START_WAIT)
            coroutineScope {
                launch { tapAlpha.animateTo(T.TAP_ALPHA, tween(T.DIALOG_PRESS, easing = T.EaseOut)) }
                launch { pressed.animateTo(1f, tween(T.DIALOG_PRESS, easing = T.EaseOut)) }
            }
            delay(T.DIALOG_PRESS_HOLD)
            coroutineScope {
                launch { y.animateTo(T.DIALOG_CLOSED_Y, gentle()) }
                launch { shown.animateTo(0f, gentle()) }
                launch { tapGrow.animateTo(1f, gentle()) }
                launch { tapAlpha.animateTo(0f, gentle()) }
                delay(T.DIALOG_CLOSE)
            }
            delay(T.DIALOG_CLOSED_HOLD)
            tapGrow.snapTo(0f)
            coroutineScope {
                launch { y.animateTo(T.DIALOG_Y, gentle()) }
                launch { shown.animateTo(1f, gentle()) }
                launch { pressed.animateTo(0f, gentle()) }
                delay(T.DIALOG_REOPEN)
            }
        }
    }
    val colors = MorningTheme.colors
    val shape = RoundedCornerShape(T.DIALOG_RADIUS.dp)
    Box(modifier) {
        Box(
            Modifier
                .figma(FigmaRect(T.DIALOG_X, y.value, T.DIALOG_WIDTH, T.DIALOG_HEIGHT))
                .graphicsLayer { alpha = shown.value.coerceIn(0f, 1f) }
                .clip(shape)
                .background(colors.surfaceCard, shape),
        ) {
            FigmaText(
                stringResource(R.string.onboarding_motion_dialog_title),
                T.DialogTitle,
                T.DIALOG_TITLE_SIZE,
                FontWeight.SemiBold,
                colors.textPrimary,
            )
            FigmaText(
                stringResource(R.string.onboarding_motion_dialog_body),
                T.DialogBody,
                T.DIALOG_BODY_SIZE,
                FontWeight.Medium,
                colors.textTertiary,
                lineHeight = T.DIALOG_BODY_LINE,
            )
            Box(
                Modifier
                    .figma(T.DialogAllow)
                    .background(colors.primarySubtle.copy(alpha = colors.primarySubtle.alpha * pressed.value)),
            )
            FigmaText(
                stringResource(R.string.onboarding_motion_dialog_deny),
                T.DialogDeny,
                T.DIALOG_BUTTON_SIZE,
                FontWeight.SemiBold,
                colors.textTertiary,
            )
            FigmaText(
                stringResource(R.string.onboarding_motion_dialog_allow),
                T.DialogAllow,
                T.DIALOG_BUTTON_SIZE,
                FontWeight.SemiBold,
                colors.primaryDefault,
            )
            TapCircle(T.DIALOG_TAP_CENTER_X, T.DIALOG_TAP_CENTER_Y, tapGrow.value, tapAlpha.value)
            Box(Modifier.figma(T.DialogDividerH).background(colors.borderDivider))
            Box(Modifier.figma(T.DialogDividerV).background(colors.borderDivider))
        }
    }
}

// ---------------------------------------------------------------- Motion Overlay Toggle (O2-4)

/**
 * '다른 앱 위에 표시' 카드(앱 이름 + 스위치). 0.2초 뒤 16 아래에서 GENTLE 0.6초로 떠오르며 나타나고,
 * 0.6초 쉰 뒤 누르는 원(0.15초) → 0.1초 뒤 스위치가 GENTLE 0.5초로 켜지며 서브 글이 '안 함'에서 '함'으로 바뀌어요
 * (옛 글은 4 위로 사라지고 새 글은 4 아래에서 올라와요). 1.6초 뒤 0.4초 동안 처음 장면으로 겹쳐 바뀌어요.
 */
@Composable
internal fun MotionOverlayToggle(modifier: Modifier = Modifier) {
    val reduceMotion = MorningTheme.reduceMotion
    val enter = remember { Animatable(0f) }
    val on = remember { Animatable(0f) }
    val tapAlpha = remember { Animatable(0f) }
    val tapGrow = remember { Animatable(0f) }
    val leaving = remember { Animatable(0f) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            enter.snapTo(1f)
            on.snapTo(1f)
            tapAlpha.snapTo(0f)
            leaving.snapTo(0f)
            return@LaunchedEffect
        }
        enter.snapTo(0f)
        delay(T.ENTER_WAIT)
        coroutineScope {
            launch { enter.animateTo(1f, gentle()) }
            delay(T.ENTER_RISE)
        }
        while (true) {
            on.snapTo(0f)
            tapAlpha.snapTo(0f)
            tapGrow.snapTo(0f)
            leaving.snapTo(0f)
            delay(T.TOGGLE_START_WAIT)
            tapAlpha.animateTo(T.TAP_ALPHA, tween(T.TOGGLE_PRESS, easing = T.EaseOut))
            delay(T.TOGGLE_PRESS_HOLD)
            coroutineScope {
                launch { on.animateTo(1f, gentle()) }
                launch { tapGrow.animateTo(1f, gentle()) }
                launch { tapAlpha.animateTo(0f, gentle()) }
                delay(T.TOGGLE_SWITCH)
            }
            delay(T.TOGGLE_END_HOLD)
            // 겹쳐 바뀌기(Dissolve): 밑에는 꺼진 장면, 위에는 켜진 장면을 두고 위쪽만 흐려지게 해요.
            on.snapTo(0f)
            leaving.snapTo(1f)
            leaving.animateTo(0f, tween(T.TOGGLE_DISSOLVE, easing = T.EaseInOut))
        }
    }
    val colors = MorningTheme.colors
    Box(
        modifier.graphicsLayer {
            alpha = enter.value.coerceIn(0f, 1f)
            translationY = ((1f - enter.value) * T.ENTER_FROM).dp.toPx()
        },
    ) {
        Box(Modifier.figma(T.ToggleCard).background(colors.surfaceCard, RoundedCornerShape(T.TOGGLE_CARD_RADIUS.dp)))
        Box(
            Modifier
                .figma(T.ToggleIcon)
                .graphicsLayer { alpha = T.TOGGLE_ICON_ALPHA }
                .background(colors.controlTrackOff, RoundedCornerShape(T.TOGGLE_ICON_RADIUS.dp)),
        )
        FigmaText(
            stringResource(R.string.onboarding_motion_app_name),
            T.ToggleLabel,
            T.TOGGLE_LABEL_SIZE,
            FontWeight.SemiBold,
            colors.textPrimary,
            Alignment.CenterStart,
        )
        ToggleState(on.value, alpha = 1f)
        if (leaving.value > 0f) ToggleState(1f, alpha = leaving.value)
        TapCircle(T.TOGGLE_TAP_CENTER_X, T.TOGGLE_TAP_CENTER_Y, tapGrow.value, tapAlpha.value)
    }
}

/** 서브 글 두 장(안 함, 함)과 스위치. [on]은 0(꺼짐)~1(켜짐), 스프링이라 조금 넘칠 수 있어요. */
@Composable
private fun ToggleState(on: Float, alpha: Float) {
    val colors = MorningTheme.colors
    val p = on.coerceIn(0f, 1f)
    val offAlpha = (1f - p) * alpha
    val onAlpha = p * alpha
    val shift = T.TOGGLE_SUB_SHIFT * on
    if (offAlpha > 0f) {
        Box(Modifier.graphicsLayer { this.alpha = offAlpha }) {
            FigmaText(
                stringResource(R.string.onboarding_motion_overlay_off),
                FigmaRect(T.TOGGLE_SUB_X, T.TOGGLE_SUB_OFF_Y - shift, T.TOGGLE_SUB_W, T.TOGGLE_SUB_H),
                T.TOGGLE_SUB_SIZE,
                FontWeight.Medium,
                colors.textTertiary,
                Alignment.CenterStart,
            )
        }
    }
    if (onAlpha > 0f) {
        Box(Modifier.graphicsLayer { this.alpha = onAlpha }) {
            FigmaText(
                stringResource(R.string.onboarding_motion_overlay_on),
                FigmaRect(T.TOGGLE_SUB_X, T.TOGGLE_SUB_ON_Y - shift, T.TOGGLE_SUB_W, T.TOGGLE_SUB_H),
                T.TOGGLE_SUB_SIZE,
                FontWeight.Medium,
                colors.textTertiary,
                Alignment.CenterStart,
            )
        }
    }
    Box(Modifier.graphicsLayer { this.alpha = alpha }) {
        Box(
            Modifier
                .figma(T.ToggleTrack)
                .background(
                    lerp(colors.controlTrackOff, colors.primaryDefault, p),
                    RoundedCornerShape(T.TOGGLE_TRACK_RADIUS.dp),
                ),
        )
        Box(
            Modifier
                .figma(
                    FigmaRect(
                        x = lerp(T.TOGGLE_KNOB_OFF_X, T.TOGGLE_KNOB_ON_X, on),
                        y = T.TOGGLE_KNOB_Y,
                        w = T.TOGGLE_KNOB_SIZE,
                        h = T.TOGGLE_KNOB_SIZE,
                    ),
                )
                .morningShadow(MorningShadows.knob, CircleShape)
                .background(colors.controlKnob, CircleShape),
        )
    }
}
