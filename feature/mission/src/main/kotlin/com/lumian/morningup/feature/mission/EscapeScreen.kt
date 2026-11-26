package com.lumian.morningup.feature.mission

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Timer
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.perform
import com.lumian.morningup.core.designsystem.theme.MissionColors
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.engine.MissionEscape
import kotlinx.coroutines.delay

const val ESCAPE_INPUT_TAG = "escapeInput"
const val ESCAPE_BACK_TAG = "escapeBack"
const val ESCAPE_TIME_TAG = "escapeTime"

private val TraceRadius = 20.dp
private val TracePadding = 24.dp
private val TraceMinHeight = 100.dp
private val TraceSize = 24.sp
private val TraceLineHeight = 34.sp
private val SmallIcon = 14.dp
private val PillHeight = 28.dp
private const val SHAKE_OFFSET = 6f
private const val GHOST_ALPHA = 0.35f
private const val LOW_FILL_ALPHA = 0.16f
private const val LOW_SECONDS = 10
private const val FRAME_MILLIS = 200L
private const val MILLIS_PER_SECOND = 1_000L
private const val SHAKE_MILLIS = 240
private const val SHAKE_STEPS = 6

/** 맞음 글자색(`status/success` #00C281). 미션 화면은 모드와 상관없이 어두워서 값으로 둬요. */
private val SuccessText = Color(0xFF00C281)

/** 울림 화면(:app)이 M5 자리에 꽂는 진짜 화면. */
@Composable
fun RingEscapeRoute(viewModel: EscapeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var remaining by remember { mutableLongStateOf(MissionEscape.IDLE_MILLIS) }
    LaunchedEffect(state.lastTypedAt, state.result) {
        if (state.result == EscapeResult.Correct) return@LaunchedEffect
        while (true) {
            remaining = (state.lastTypedAt + MissionEscape.IDLE_MILLIS - viewModel.nowMillis()).coerceAtLeast(0)
            if (remaining == 0L) {
                viewModel.onIdle()
                break
            }
            delay(FRAME_MILLIS)
        }
    }
    EscapeScreen(
        state = state,
        remainingMillis = remaining,
        onType = viewModel::onType,
        onBack = viewModel::onBack,
    )
}

/**
 * M5 미션 포기, 따라 쓰기 모양(TSK-231 시안 `1165:17736`, 결정 171). 미션 화면처럼 늘 어두운 바탕이에요.
 * 왼쪽 위 '미션으로 돌아가기', 오른쪽 위 남은 시간 알약(마지막으로 친 때부터 1분, 10초 아래면 빨강),
 * 안내 한 줄, 흐린 예문 위에 바로 겹쳐 쓰는 칸, 그리고 폰 키보드. 확인 버튼은 없어요.
 * 끝까지 맞게 쓰면 칸이 초록이 되고 위 알약이 사라져요. 뒤로 가기는 미션으로 돌아가기예요.
 */
@Composable
fun EscapeScreen(
    state: EscapeUiState,
    remainingMillis: Long,
    onType: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val correct = state.result == EscapeResult.Correct
    val seconds = ((remainingMillis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
    val low = !correct && seconds <= LOW_SECONDS
    BackHandler(enabled = !correct) { onBack() }
    MissionFrame(backdrop = MissionColors.DropWords, status = null, remainingMillis = null, modifier = modifier) {
        Column(Modifier.fillMaxSize().imePadding()) {
            Spacer(Modifier.height(MorningDimens.space12))
            Row(Modifier.fillMaxWidth().height(PillHeight), verticalAlignment = Alignment.CenterVertically) {
                if (!correct) BackPill(onBack)
                Spacer(Modifier.weight(1f))
                if (!correct) TimePill(seconds, low)
            }
            Spacer(Modifier.height(MorningDimens.space24))
            Text(
                text = if (low) {
                    stringResource(R.string.escape_screen_idle, seconds)
                } else {
                    stringResource(R.string.escape_screen_guide)
                },
                style = MorningTheme.typography.captionMedium,
                color = if (low) MorningTheme.colors.statusError else MissionColors.TextStatus,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(MorningDimens.space16))
            TraceField(state = state, onType = onType)
        }
    }
}

@Composable
private fun BackPill(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .height(PillHeight)
            .background(MissionColors.Fill, CircleShape)
            .clickable(role = Role.Button, onClick = onBack)
            .testTag(ESCAPE_BACK_TAG)
            .padding(horizontal = MorningDimens.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Lucide.ChevronLeft,
            contentDescription = null,
            tint = MissionColors.Text,
            modifier = Modifier.size(SmallIcon),
        )
        Spacer(Modifier.width(MorningDimens.space4))
        Text(
            text = stringResource(R.string.escape_screen_back),
            style = MorningTheme.typography.captionStrong,
            color = MissionColors.Text,
        )
    }
}

@Composable
private fun TimePill(seconds: Int, low: Boolean) {
    val error = MorningTheme.colors.statusError
    val color = if (low) error else MissionColors.Text
    Row(
        modifier = Modifier
            .height(PillHeight)
            .background(if (low) error.copy(alpha = LOW_FILL_ALPHA) else MissionColors.Fill, CircleShape)
            .testTag(ESCAPE_TIME_TAG)
            .padding(horizontal = MorningDimens.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Lucide.Timer, contentDescription = null, tint = color, modifier = Modifier.size(SmallIcon))
        Spacer(Modifier.width(MorningDimens.space4))
        Text(
            text = stringResource(R.string.escape_screen_seconds, seconds),
            style = MorningTheme.typography.captionStrong,
            color = color,
        )
    }
}

/**
 * 흐린 예문 위에 바로 겹쳐 쓰는 칸. 쓴 글자는 예문 같은 자리를 덮어요([TraceOverlay]).
 * 맞게 친 글자는 진하게, 조합 중인 한글은 밑줄(키보드 조합 표시), 틀린 글자는 빨강과 밑줄이고 칸이 살짝 흔들려요.
 * 틀린 글자를 지우기 전에는 이어 쓰지 못해요. 예문이 길면 칸 안에서 줄을 바꿔요. 줄 바꿈 글자는 넣지 않아요.
 */
@Composable
private fun TraceField(state: EscapeUiState, onType: (String) -> Unit) {
    val correct = state.result == EscapeResult.Correct
    var field by remember { mutableStateOf(TextFieldValue(state.text, TextRange(state.text.length))) }
    val wrongAt = if (correct) null else TraceTyping.firstWrong(field.text, state.sentence, field.composition)
    val shake = rememberShake(wrongAt, state.rejected)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val colors = TraceColors(
        done = if (correct) SuccessText else MissionColors.Text,
        ghost = MissionColors.Text.copy(alpha = GHOST_ALPHA),
        wrong = MorningTheme.colors.statusError,
    )
    val overlay = remember(state.sentence, wrongAt, colors) { TraceOverlay(state.sentence, wrongAt, colors) }
    val description = stringResource(R.string.escape_screen_input)
    Box(
        Modifier
            .fillMaxWidth()
            .offset(x = shake.value.dp)
            .heightIn(min = TraceMinHeight)
            .background(if (correct) MissionColors.Success else MissionColors.Fill, RoundedCornerShape(TraceRadius))
            .padding(TracePadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = field,
            onValueChange = { next ->
                val clean = next.text.replace("\n", "")
                val value = if (clean == next.text) next else TextFieldValue(clean, TextRange(clean.length))
                val old = field.text
                // 틀린 글자 뒤로 이어 쓰면 칸은 그대로 두고, 화면 모델이 막힌 횟수를 세어 흔들어요.
                if (TraceTyping.accepts(old, clean, state.sentence)) field = value
                if (clean != old) onType(clean)
            },
            enabled = !correct,
            textStyle = MorningTheme.typography.headlineLarge.copy(
                fontSize = TraceSize,
                lineHeight = TraceLineHeight,
                fontWeight = FontWeight.Bold,
                color = colors.done,
            ),
            visualTransformation = overlay,
            cursorBrush = SolidColor(MissionColors.Text),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.None,
                autoCorrectEnabled = false,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus)
                .testTag(ESCAPE_INPUT_TAG)
                .semantics { contentDescription = description },
        )
    }
}

/** 틀린 글자가 새로 나오거나 막힐 때마다 칸을 좌우로 살짝 흔들고 짧게 떨어요. 움직임 줄이기면 떨기만 해요. */
@Composable
private fun rememberShake(wrongAt: Int?, rejected: Int): Animatable<Float, *> {
    val shake = remember { Animatable(0f) }
    val view = LocalView.current
    val reduceMotion = MorningTheme.reduceMotion
    LaunchedEffect(wrongAt, rejected) {
        if (wrongAt == null) return@LaunchedEffect
        view.perform(MorningHaptic.Reject)
        if (reduceMotion) return@LaunchedEffect
        shake.animateTo(
            0f,
            keyframes {
                durationMillis = SHAKE_MILLIS
                SHAKE_OFFSET at SHAKE_MILLIS / SHAKE_STEPS
                -SHAKE_OFFSET at SHAKE_MILLIS / 2
                SHAKE_OFFSET / 2 at SHAKE_MILLIS - SHAKE_MILLIS / SHAKE_STEPS
            },
        )
    }
    return shake
}

internal data class TraceColors(val done: Color, val ghost: Color, val wrong: Color)

/**
 * 쓴 글 뒤에 아직 안 쓴 예문을 흐리게 이어 붙여, 쓴 글자가 예문 같은 자리를 덮어 보이게 해요.
 * 커서는 쓴 글 끝을 넘지 않아요. [wrongAt]부터 쓴 글자는 빨강과 밑줄이에요.
 */
internal class TraceOverlay(
    private val sentence: String,
    private val wrongAt: Int?,
    private val colors: TraceColors,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val typed = text.text
        val shown = buildAnnotatedString {
            typed.forEachIndexed { i, c ->
                val style = if (wrongAt != null && i >= wrongAt) {
                    SpanStyle(color = colors.wrong, textDecoration = TextDecoration.Underline)
                } else {
                    SpanStyle(color = colors.done)
                }
                withStyle(style) { append(c) }
            }
            if (typed.length < sentence.length) {
                withStyle(SpanStyle(color = colors.ghost)) { append(sentence.substring(typed.length)) }
            }
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = offset
            override fun transformedToOriginal(offset: Int) = offset.coerceAtMost(typed.length)
        }
        return TransformedText(shown, mapping)
    }

    override fun equals(other: Any?): Boolean =
        other is TraceOverlay && other.sentence == sentence && other.wrongAt == wrongAt && other.colors == colors

    override fun hashCode(): Int = (sentence.hashCode() * HASH + (wrongAt ?: -1)) * HASH + colors.hashCode()

    private companion object {
        const val HASH = 31
    }
}
