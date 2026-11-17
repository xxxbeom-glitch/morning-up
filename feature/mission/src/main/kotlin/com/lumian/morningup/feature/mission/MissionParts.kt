package com.lumian.morningup.feature.mission

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.motion.perform
import com.lumian.morningup.core.designsystem.theme.MissionColors
import com.lumian.morningup.core.designsystem.theme.MorningTheme

/** 흔들기 왕복 수(세 번, 약 0.3초). */
private const val SHAKE_STEPS = 4

/** 미션 키 공통: 높이 56, 모서리 14, 누르면 0.94배. */
internal val MissionKeyHeight = 56.dp
internal val MissionKeyRadius = 14.dp
internal val MissionGap = 10.dp
private const val KEY_PRESS_SCALE = 0.94f

/** 피그마 Mission Answer, Mission Input: 높이 64, 모서리 16. */
internal val MissionRowHeight = 64.dp
internal val MissionRowRadius = 16.dp

/** 좌우로 짧게 흔들고 제자리로 와요(dev-plan 오답). */
internal suspend fun Animatable<Float, AnimationVector1D>.shakeSideways(distance: Float) {
    repeat(SHAKE_STEPS) { step ->
        animateTo(if (step % 2 == 0) distance else -distance, MorningMotion.effects(MorningMotion.EffectsFast))
    }
    animateTo(0f, MorningMotion.effects(MorningMotion.EffectsFast))
}

/** 미션 화면 글자(피그마 11-16 통일 규칙). 토큰 글꼴에서 크기와 굵기만 바꿔 써요. */
internal object MissionText {
    /** 분야, 동작 안내: SemiBold 15. */
    val guide: TextStyle
        @Composable get() = MorningTheme.typography.bodyMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

    /** 보조 안내: SemiBold 14. */
    val hint: TextStyle
        @Composable get() = MorningTheme.typography.bodySmall.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

    /** 퀴즈 문제: ExtraBold 24, 줄 간격 135%. */
    val question: TextStyle
        @Composable get() = MorningTheme.typography.headlineLarge.copy(
            fontSize = 24.sp,
            lineHeight = 32.4.sp,
            fontWeight = FontWeight.ExtraBold,
        )

    /** 답 줄, 키 글자: SemiBold 18 / 24. */
    val answer: TextStyle
        @Composable get() = MorningTheme.typography.bodyMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

    val key: TextStyle
        @Composable get() = MorningTheme.typography.headlineNumber.copy(
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
        )

    /** 떨어지는 단어: Bold 18. */
    val word: TextStyle
        @Composable get() = MorningTheme.typography.bodyMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold)

    /** 입력칸 값: Bold 28. */
    val input: TextStyle
        @Composable get() = MorningTheme.typography.headlineNumber.copy(fontWeight = FontWeight.Bold)
}

/** 입력 칸 안 엔터 버튼(결정 90): 48x48, 모서리 12, 오른쪽 8, 흰 12% 칠에 흰 80% 체크. */
private val EnterSize = 48.dp
private val EnterRadius = 12.dp
private val EnterInset = 8.dp
private val WrongBorder = 2.dp
private val InputCursorWidth = 2.dp
private val InputCursorHeight = 28.dp
private val InputCursorGap = 4.dp
private const val ENTER_FILL_ALPHA = 0.12f
private const val ENTER_ICON_ALPHA = 0.8f

/** 엔터 버튼 이름표(테스트용). 입력 칸 이름표 뒤에 붙여요. */
const val ENTER_SUFFIX = "Enter"

/**
 * 피그마 Mission Input(엔터 켬): 높이 64, 모서리 16, 흰 10% 칠. 가운데 값(Bold 28)과 흰 커서, 안 오른쪽 엔터 버튼.
 * [wrong]이면(결정 93) 칠은 그대로 두고 안쪽 테두리 2와 값 글자만 `status/error`로 바꿔요.
 * 값이 비면 [placeholder]를 흐리게 보여 줘요(없으면 커서만).
 */
@Composable
internal fun MissionInputField(
    value: String,
    wrong: Boolean,
    onEnter: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    fill: Color = MissionColors.Fill,
    placeholder: String? = null,
) {
    val error = MorningTheme.colors.statusError
    val shape = RoundedCornerShape(MissionRowRadius)
    val description = stringResource(R.string.mission_math_input_description, value)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MissionRowHeight)
            .background(fill, shape)
            .then(if (wrong) Modifier.border(WrongBorder, error, shape) else Modifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = EnterSize + EnterInset)
                .testTag(tag)
                .semantics { contentDescription = description },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (value.isEmpty() && placeholder != null) {
                Text(text = placeholder, style = MorningTheme.typography.bodyMedium, color = MissionColors.TextHint)
            } else {
                Text(
                    text = value,
                    style = MissionText.input,
                    color = if (wrong) error else MissionColors.Text,
                    maxLines = 1,
                )
                if (!wrong) {
                    if (value.isNotEmpty()) Spacer(Modifier.width(InputCursorGap))
                    Box(Modifier.width(InputCursorWidth).height(InputCursorHeight).background(MissionColors.Text))
                }
            }
        }
        EnterButton(
            onClick = onEnter,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = EnterInset)
                .testTag(tag + ENTER_SUFFIX),
        )
    }
}

@Composable
private fun EnterButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val view = LocalView.current
    val description = stringResource(R.string.mission_math_confirm)
    Box(
        modifier = modifier
            .size(EnterSize)
            .background(Color.White.copy(alpha = ENTER_FILL_ALPHA), RoundedCornerShape(EnterRadius))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
            ) {
                view.perform(MorningHaptic.KeyTap)
                onClick()
            }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Lucide.Check,
            contentDescription = null,
            tint = Color.White.copy(alpha = ENTER_ICON_ALPHA),
            modifier = Modifier.size(MorningDimens.iconMd),
        )
    }
}

/** 미션 키 한 칸(흰색 10% 칠, 눌림 18%, 0.94배, `KEYBOARD_TAP`). */
@Composable
internal fun MissionKeyBox(
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rest: Color = MissionColors.Fill,
    content: @Composable () -> Unit,
) {
    val reduceMotion = MorningTheme.reduceMotion
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) KEY_PRESS_SCALE else 1f,
        animationSpec = MorningMotion.spatial(MorningMotion.SpatialFast, reduceMotion = false),
        label = "missionKeyScale",
    )
    Box(
        modifier = modifier
            .height(MissionKeyHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(if (pressed) MissionColors.FillPressed else rest, RoundedCornerShape(MissionKeyRadius))
            .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                view.perform(MorningHaptic.KeyTap)
                onClick()
            }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
