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
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Delete
import com.composables.icons.lucide.Lucide
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.motion.perform
import com.lumian.morningup.core.designsystem.theme.MissionColors
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.mission.MathKey
import com.lumian.morningup.core.model.mission.QuickMathKeypad
import com.lumian.morningup.core.model.mission.QuickMathState
import com.lumian.morningup.core.model.mission.SubmitResult
import kotlinx.coroutines.delay

/** 피그마 M3 치수. */
private val InputMaxWidth = 320.dp
private val InputHeight = 64.dp
private val InputRadius = 16.dp
private val CursorWidth = 2.dp
private val CursorHeight = 28.dp
private val KeyHeight = 56.dp
private val KeyRadius = 14.dp
private val KeyGap = 10.dp
private const val KEY_COLUMNS = 3

/** 키 누름 0.94배(dev-plan 미션 키패드). */
private const val KEY_PRESS_SCALE = 0.94f

/** 정답 칠이 남는 시간, 오답 흔들기 폭. */
private const val SUCCESS_HOLD_MILLIS = 800L
private val ShakeDistance = 8.dp
private const val SHAKE_STEPS = 4

/** 남은 시간 다시 그리는 간격. */
private const val TIMER_FRAME_MILLIS = 100L

/** 화면 이름표(테스트에서 키 자리를 찾을 때). */
const val MATH_KEY_TAG = "mathKey"
const val MATH_INPUT_TAG = "mathInput"

/** :app이 미션 자리에 꽂는 연결 함수. */
@Composable
fun QuickMathRoute(viewModel: QuickMathViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var remaining by remember { mutableLongStateOf(remainingOf(state.timerEndsAt, viewModel.nowMillis())) }
    LaunchedEffect(state.timerEndsAt) {
        while (true) {
            remaining = remainingOf(state.timerEndsAt, viewModel.nowMillis())
            delay(TIMER_FRAME_MILLIS)
        }
    }
    QuickMathScreen(state = state, remainingMillis = remaining, onKey = viewModel::onKey)
}

/** M3 Quick Math 화면(상태만 받아 그려요). */
@Composable
fun QuickMathScreen(state: QuickMathUiState, remainingMillis: Long, onKey: (MathKey) -> Unit) {
    val game = state.game
    MissionFrame(
        backdrop = MissionColors.QuickMath,
        status = game?.let { stringResource(R.string.mission_math_streak, it.streak, it.target) }.orEmpty(),
        remainingMillis = remainingMillis,
    ) {
        if (game == null) return@MissionFrame
        Spacer(Modifier.weight(1f))
        Problem(game = game, feedback = state.feedback)
        Spacer(Modifier.weight(1f))
        Keypad(keypad = game.keypad, onKey = onKey)
        Spacer(Modifier.height(MorningDimens.space24))
    }
}

@Composable
private fun Problem(game: QuickMathState, feedback: MathFeedback?) {
    val view = LocalView.current
    val density = LocalDensity.current
    val shake = remember { Animatable(0f) }
    var success by remember { mutableStateOf(false) }
    LaunchedEffect(feedback?.id) {
        when (feedback?.result) {
            SubmitResult.Correct, SubmitResult.Finished -> {
                view.perform(MorningHaptic.Confirm)
                success = true
                delay(SUCCESS_HOLD_MILLIS)
                success = false
            }
            SubmitResult.Wrong -> {
                view.perform(MorningHaptic.Reject)
                val distance = with(density) { ShakeDistance.toPx() }
                repeat(SHAKE_STEPS) { step ->
                    val target = if (step % 2 == 0) distance else -distance
                    shake.animateTo(target, MorningMotion.effects(MorningMotion.EffectsFast))
                }
                shake.animateTo(0f, MorningMotion.effects(MorningMotion.EffectsFast))
            }
            SubmitResult.Empty, null -> Unit
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
        Spacer(Modifier.height(MorningDimens.space16))
        val inputDescription = stringResource(R.string.mission_math_input_description, game.input)
        Row(
            modifier = Modifier
                .widthIn(max = InputMaxWidth)
                .fillMaxWidth()
                .height(InputHeight)
                .graphicsLayer { translationX = shake.value }
                .background(fill, RoundedCornerShape(InputRadius))
                .testTag(MATH_INPUT_TAG)
                .semantics { contentDescription = inputDescription },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = game.input,
                style = typography.headlineNumber.copy(fontWeight = FontWeight.Bold),
                color = MissionColors.Text,
            )
            Box(Modifier.width(CursorWidth).height(CursorHeight).background(MissionColors.Text))
        }
        Spacer(Modifier.height(MorningDimens.space12))
        Text(
            text = stringResource(R.string.mission_math_hint),
            style = typography.bodySmall,
            color = MissionColors.TextHint,
        )
    }
}

/** 3x4 키패드. 숫자 자리는 정답마다 섞이고, 섞일 때 글자만 바뀌며 페이드돼요(칸은 그대로). */
@Composable
private fun Keypad(keypad: List<Int>, onKey: (MathKey) -> Unit) {
    val keys = QuickMathKeypad.keys(keypad)
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
        is MathKey.Digit -> MissionColors.Fill
        MathKey.Delete -> MissionColors.FillWeak
        MathKey.Confirm -> MissionColors.TimerFill
    }
    val fill = if (pressed && key !is MathKey.Confirm) MissionColors.FillPressed else rest
    val description = when (key) {
        is MathKey.Digit -> key.value.toString()
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
