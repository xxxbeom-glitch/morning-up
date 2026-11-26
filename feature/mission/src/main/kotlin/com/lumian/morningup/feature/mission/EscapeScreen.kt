package com.lumian.morningup.feature.mission

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.perform
import com.lumian.morningup.core.designsystem.motion.pressScale
import com.lumian.morningup.core.designsystem.theme.MissionColors
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.engine.MissionEscape
import java.util.UUID
import kotlinx.coroutines.delay

const val ESCAPE_INPUT_TAG = "escapeInput"
const val ESCAPE_BACK_TAG = "escapeBack"

private val TraceRadius = 20.dp
private val TracePadding = 24.dp
private val TraceMinHeight = 100.dp
private val TraceSize = 24.sp
private val TraceLineHeight = 34.sp
private val RetryHeight = 40.dp
private const val SHAKE_OFFSET = 6f
private const val GHOST_ALPHA = 0.35f
private const val LOW_SECONDS = 10
private const val FRAME_MILLIS = 200L
private const val MILLIS_PER_SECOND = 1_000L
private const val SHAKE_MILLIS = 240
private const val SHAKE_STEPS = 6

/** 맞음 글자색(`status/success` #00C281). 미션 화면은 모드와 상관없이 어두워서 값으로 둬요. */
private val SuccessText = Color(0xFF00C281)

/**
 * M5를 열 때마다 바뀌는 열기 번호. 울림 화면(Activity)이 살아 있는 동안 뷰모델이 남아서, 한 울림에서 두 번째로 열면
 * 첫 열기 때 1분이 이어져 바로 미션으로 돌아갔어요(ISS-056). 화면을 떠나면 잊고, 접기나 회전 같은 다시 그리기에는 남아요.
 */
@Composable
internal fun rememberEscapeOpenKey(): String = rememberSaveable { "escape_${UUID.randomUUID()}" }

/** 울림 화면(:app)이 M5 자리에 꽂는 진짜 화면. 열 때마다 새 뷰모델이라 1분과 쓴 글이 처음부터예요. */
@Composable
fun RingEscapeRoute(viewModel: EscapeViewModel = hiltViewModel(key = rememberEscapeOpenKey())) {
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
 * 위에 다른 미션 화면과 같은 남은 시간 바(마지막으로 친 때부터 1분, 칠 때마다 다시 차요), 안내 한 줄
 * (10초 아래면 빨강), 흐린 예문 위에 바로 겹쳐 쓰는 칸, 그 아래 '다시 미션 수행하기', 그리고 폰 키보드.
 * 확인 버튼은 없어요. 끝까지 맞게 쓰면 칸이 초록이 되고 아래 버튼이 사라져요. 뒤로 가기는 미션으로 돌아가기예요.
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
            // 다른 미션 화면 위쪽 줄과 같은 남은 시간 바(MissionHeader와 같은 자리, 같은 부품).
            MissionTimeline(
                remainingMillis = if (correct) MissionEscape.IDLE_MILLIS else remainingMillis,
                totalMillis = MissionEscape.IDLE_MILLIS,
                modifier = Modifier.fillMaxWidth().padding(top = MorningDimens.space12),
            )
            // 시간 바 아래 남은 자리의 세로 가운데에 안내, 칸, 버튼 묶음을 둬요(TSK-242). 키보드가 올라오면
            // imePadding으로 남은 자리가 줄어 키보드 위에서 다시 가운데에 오고, 그래도 넘치면 스크롤돼요.
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = MorningDimens.space16),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
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
                    if (!correct) {
                        Spacer(Modifier.height(MorningDimens.space16))
                        RetryMissionButton(onBack)
                    }
                }
            }
        }
    }
}

/**
 * 칸 아래 가운데 '다시 미션 수행하기'. 옛 M5 '미션으로 돌아가기'와 같은 미션 화면 알약(흰 10% 칠, 흰 글자)이에요.
 * 누르면 하던 미션으로 돌아가요.
 */
@Composable
private fun RetryMissionButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(RetryHeight)
            .pressScale(interaction)
            .background(MissionColors.Fill, CircleShape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                view.perform(MorningHaptic.Press)
                onBack()
            }
            .testTag(ESCAPE_BACK_TAG)
            .padding(horizontal = MorningDimens.space16),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.escape_screen_retry),
            style = MorningTheme.typography.captionStrong,
            color = MissionColors.Text,
        )
    }
}

/**
 * 흐린 예문 위에 바로 겹쳐 쓰는 칸. 같은 글꼴, 크기, 줄 높이, 시작 자리의 세 겹을 겹쳐요(TSK-246).
 * 1. 맨 아래 흐린 예문([traceGhost]). 늘 예문 전체로 줄을 짜서, 조합 중 글자(예: 'ㅌ')의 폭이 달라도 뒤 글자가 밀리지 않아요.
 * 2. 가운데 보이는 쓴 글자([TraceOverlay]). 맞은 글자는 진하게, 틀린 글자는 빨강이에요(밑줄 없음, 칸 흔들림).
 * 3. 맨 위 진짜 입력 칸. 글자는 모두 투명이라 키보드 조합 밑줄도 투명이 되어 안 보여요. 커서만 보여요.
 * 2와 3은 같은 글(쓴 글 + 투명한 남은 예문)이라 줄과 자리가 같아서 커서가 쓴 글자 끝에 정확히 붙어요.
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
    val shown = remember(state.sentence, wrongAt, colors) { TraceOverlay(state.sentence, wrongAt, colors) }
    val input = remember(state.sentence) { TraceOverlay(state.sentence, wrongAt = null, colors = null) }
    val composing = if (correct) null else field.composition
    val ghost = remember(state.sentence, field.text.length, composing, colors.ghost) {
        traceGhost(state.sentence, field.text.length, composing, colors.ghost)
    }
    val textStyle = MorningTheme.typography.headlineLarge.copy(
        fontSize = TraceSize,
        lineHeight = TraceLineHeight,
        fontWeight = FontWeight.Bold,
        color = colors.done,
    )
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
        // 세 겹은 모두 왼쪽 위에서 시작해야 줄 수가 달라도 같은 자리에 겹쳐요. 묶음째로 칸 세로 가운데에 둬요.
        Box(Modifier.fillMaxWidth()) {
            BasicText(text = ghost, style = textStyle, modifier = Modifier.fillMaxWidth())
            BasicText(
                text = shown.filter(AnnotatedString(field.text)).text,
                style = textStyle,
                modifier = Modifier.fillMaxWidth(),
            )
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
                textStyle = textStyle.copy(color = Color.Transparent),
                visualTransformation = input,
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
}

/**
 * 맨 아래 흐린 예문 겹. 쓴 글자 아래는 투명으로 비워 두고(자리는 그대로), 조합 중인 글자 아래는 예문을 흐리게 보여요.
 * 늘 예문 전체 글이라 줄 바꿈과 글자 자리가 쓰는 동안 바뀌지 않아요.
 */
internal fun traceGhost(sentence: String, typedLength: Int, composition: TextRange?, ghost: Color): AnnotatedString =
    buildAnnotatedString {
        sentence.forEachIndexed { i, c ->
            val composing = composition != null && i >= composition.min && i < composition.max
            val color = if (i < typedLength && !composing) Color.Transparent else ghost
            withStyle(SpanStyle(color = color)) { append(c) }
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
 * 쓴 글 뒤에 아직 안 쓴 예문을 투명하게 이어 붙여요. 그래서 줄 짜기가 예문 전체와 같아 쓴 글자가 예문 같은 자리에 와요.
 * 남은 예문은 맨 아래 겹([traceGhost])이 흐리게 보여 줘요. 커서는 쓴 글 끝을 넘지 않아요.
 * [wrongAt]부터 쓴 글자는 빨강이에요(밑줄 없음, TSK-246). [colors]가 null이면 쓴 글자도 투명(진짜 입력 칸 겹)이에요.
 */
internal class TraceOverlay(
    private val sentence: String,
    private val wrongAt: Int?,
    private val colors: TraceColors?,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val typed = text.text
        val shown = buildAnnotatedString {
            typed.forEachIndexed { i, c ->
                val color = when {
                    colors == null -> Color.Transparent
                    wrongAt != null && i >= wrongAt -> colors.wrong
                    else -> colors.done
                }
                withStyle(SpanStyle(color = color)) { append(c) }
            }
            if (typed.length < sentence.length) {
                withStyle(SpanStyle(color = Color.Transparent)) { append(sentence.substring(typed.length)) }
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
