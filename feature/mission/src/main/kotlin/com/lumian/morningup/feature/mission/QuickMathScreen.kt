package com.lumian.morningup.feature.mission

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Delete
import com.composables.icons.lucide.Lucide
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.motion.perform
import com.lumian.morningup.core.designsystem.theme.FixedFontScale
import com.lumian.morningup.core.designsystem.theme.MissionColors
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.mission.MathKey
import com.lumian.morningup.core.model.mission.QuickMathKeypad
import com.lumian.morningup.core.model.mission.QuickMathState
import com.lumian.morningup.feature.mission.play.FeedbackKind
import com.lumian.morningup.feature.mission.play.MissionFeedback
import com.lumian.morningup.feature.mission.play.MissionPlay
import kotlinx.coroutines.delay

/** 피그마 M3 치수. */
private val InputMaxWidth = 320.dp
private val KeyHeight = 56.dp
private val KeyRadius = 14.dp
private val KeyGap = 10.dp
private const val KEY_COLUMNS = 3

/** 키 누름 0.94배(dev-plan 미션 키패드). */
private const val KEY_PRESS_SCALE = 0.94f

/** 정답 칠이 남는 시간, 오답 흔들기 폭. */
private const val SUCCESS_HOLD_MILLIS = 800L

/** 화면 이름표(테스트에서 키 자리를 찾을 때). */
const val MATH_KEY_TAG = "mathKey"
const val MATH_INPUT_TAG = "mathInput"

/** M3 Quick Math 내용(미션 틀 안). */
@Composable
internal fun ColumnScope.QuickMathContent(play: MissionPlay.Math, onKey: (MathKey) -> Unit) {
    val game = play.shown
    Spacer(Modifier.weight(1f))
    Problem(game = game, feedback = play.feedback, wrong = play.holding, onEnter = { onKey(MathKey.Confirm) })
    Spacer(Modifier.weight(1f))
    Keypad(keypad = game.keypad, onKey = onKey)
    Spacer(Modifier.height(MorningDimens.space24))
}

/**
 * 문제와 입력 칸(사이 24, 결정 90). 틀리면(결정 93) 큰 표시 없이 입력 칸 테두리와 글자만 빨개지고,
 * 폰이 짧게 떨린 뒤 약 1초 뒤 새 문제로 바뀌어요. 흔들기는 하지 않아요.
 */
@Composable
private fun Problem(game: QuickMathState, feedback: MissionFeedback?, wrong: Boolean, onEnter: () -> Unit) {
    val view = LocalView.current
    var success by remember { mutableStateOf(false) }
    LaunchedEffect(feedback?.id) {
        when (feedback?.kind) {
            FeedbackKind.Correct, FeedbackKind.Finished -> {
                view.perform(MorningHaptic.Confirm)
                success = true
                delay(SUCCESS_HOLD_MILLIS)
                success = false
            }
            FeedbackKind.Wrong -> view.perform(MorningHaptic.Reject)
            null -> Unit
        }
    }
    val fill by animateColorAsState(
        targetValue = if (success) MissionColors.Success else MissionColors.Fill,
        animationSpec = MorningMotion.effects(MorningMotion.EffectsDefault),
        label = "inputFill",
    )
    val typography = MorningTheme.typography
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = game.problem.text,
            style = typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MissionColors.Text,
        )
        Spacer(Modifier.height(MorningDimens.space24))
        MissionInputField(
            value = game.input,
            wrong = wrong,
            onEnter = onEnter,
            tag = MATH_INPUT_TAG,
            fill = fill,
            modifier = Modifier.widthIn(max = InputMaxWidth),
        )
    }
}

/** 3x4 키패드. 숫자 자리는 정답마다 섞이고, 섞일 때 글자만 바뀌며 페이드돼요(칸은 그대로). */
@Composable
private fun Keypad(keypad: List<Int>, onKey: (MathKey) -> Unit) {
    val keys = QuickMathKeypad.keys(keypad)
    FixedFontScale { KeyRows(keys, onKey) }
}

@Composable
private fun KeyRows(keys: List<MathKey>, onKey: (MathKey) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(KeyGap)) {
        keys.chunked(KEY_COLUMNS).forEachIndexed { row, rowKeys ->
            Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
                rowKeys.forEachIndexed { column, key ->
                    KeyCell(
                        key = key,
                        onClick = { onKey(key) },
                        modifier = Modifier.weight(1f).testTag("$MATH_KEY_TAG${row * KEY_COLUMNS + column}"),
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyCell(key: MathKey, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val reduceMotion = MorningTheme.reduceMotion
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) KEY_PRESS_SCALE else 1f,
        animationSpec = MorningMotion.spatial(MorningMotion.SpatialFast, reduceMotion = false),
        label = "keyScale",
    )
    val rest = when (key) {
        is MathKey.Digit, MathKey.Dot -> MissionColors.Fill
        MathKey.Delete -> MissionColors.FillWeak
        MathKey.Confirm -> MissionColors.TimerFill
    }
    val fill = if (pressed && key !is MathKey.Confirm) MissionColors.FillPressed else rest
    val description = when (key) {
        is MathKey.Digit -> key.value.toString()
        MathKey.Dot -> stringResource(R.string.mission_math_dot)
        MathKey.Delete -> stringResource(R.string.mission_math_delete)
        MathKey.Confirm -> stringResource(R.string.mission_math_confirm)
    }
    Box(
        modifier = modifier
            .height(KeyHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(fill, RoundedCornerShape(KeyRadius))
            .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                view.perform(MorningHaptic.KeyTap)
                onClick()
            }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        when (key) {
            is MathKey.Digit -> Crossfade(
                targetState = key.value,
                animationSpec = MorningMotion.effects(MorningMotion.EffectsDefault),
                label = "keyDigit",
            ) { digit ->
                Text(
                    text = digit.toString(),
                    style = MorningTheme.typography.headlineNumber,
                    color = MissionColors.Text,
                )
            }
            MathKey.Dot -> Text(
                text = ".",
                style = MorningTheme.typography.headlineNumber,
                color = MissionColors.Text,
            )
            MathKey.Delete -> Icon(
                imageVector = Lucide.Delete,
                contentDescription = null,
                tint = MissionColors.Text,
                modifier = Modifier.size(MorningDimens.iconMd),
            )
            MathKey.Confirm -> Icon(
                imageVector = Lucide.Check,
                contentDescription = null,
                tint = MissionColors.OnConfirm,
                modifier = Modifier.size(MorningDimens.iconMd),
            )
        }
    }
}
