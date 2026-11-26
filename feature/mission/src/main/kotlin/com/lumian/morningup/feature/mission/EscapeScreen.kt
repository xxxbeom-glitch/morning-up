package com.lumian.morningup.feature.mission

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Timer
import com.composables.icons.lucide.VolumeX
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.perform
import com.lumian.morningup.core.designsystem.theme.MissionColors
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.engine.MissionEscape
import kotlinx.coroutines.delay

const val ESCAPE_INPUT_TAG = "escapeInput"
const val ESCAPE_SUBMIT_TAG = "escapeSubmit"
const val ESCAPE_BACK_TAG = "escapeBack"

private val SentenceRadius = 20.dp
private val SentencePadding = 24.dp
private val SentenceSize = 22.sp
private val InputSize = 20.sp
private val InputEndPadding = 56.dp
private val SubmitSize = 48.dp
private val SubmitRadius = 12.dp
private val SubmitInset = 8.dp
private val WrongBorder = 2.dp
private val SmallIcon = 16.dp
private val BackPillHeight = 32.dp
private const val SENTENCE_FILL_ALPHA = 0.05f
private const val SUBMIT_FILL_ALPHA = 0.12f
private const val SUBMIT_ICON_ALPHA = 0.8f
private const val LOW_SECONDS = 10
private const val FRAME_MILLIS = 200L
private const val MILLIS_PER_SECOND = 1_000L

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
        onSubmit = viewModel::onSubmit,
        onBack = viewModel::onBack,
    )
}

/**
 * M5 미션 포기(결정 171, 172, 피그마 다크 `1149:15007`). 미션 화면처럼 늘 어두운 바탕이에요.
 * 위 타임라인 바는 마지막으로 글자를 친 때부터 1분을 보여 주고, 10초 아래면 글이 빨개져요.
 * 예문 카드, 폰 키보드로 쓰는 칸(엔터 켬), 아래 '소리를 잠시 껐어요'와 '미션으로 돌아가기'.
 * 맞으면 칸이 초록이 되고 키보드와 아래 줄이 사라져요. 뒤로 가기는 미션으로 돌아가기예요.
 */
@Composable
fun EscapeScreen(
    state: EscapeUiState,
    remainingMillis: Long,
    onType: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val correct = state.result == EscapeResult.Correct
    BackHandler(enabled = !correct) { onBack() }
    MissionFrame(backdrop = MissionColors.DropWords, status = null, remainingMillis = null, modifier = modifier) {
        Column(Modifier.fillMaxSize().imePadding()) {
            Spacer(Modifier.height(MorningDimens.space12))
            MissionTimeline(
                remainingMillis = if (correct) MissionEscape.IDLE_MILLIS else remainingMillis,
                totalMillis = MissionEscape.IDLE_MILLIS,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(MorningDimens.space20))
            Text(
                text = stringResource(R.string.escape_screen_title),
                style = MorningTheme.typography.headlineLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                color = MissionColors.Text,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (!correct) IdleLine(remainingMillis)
            Spacer(Modifier.height(MorningDimens.space24))
            Text(
                text = stringResource(R.string.escape_screen_guide),
                style = MorningTheme.typography.bodyMedium,
                color = MissionColors.TextStatus,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(MorningDimens.space16))
            SentenceCard(state.sentence)
            Spacer(Modifier.height(MorningDimens.space16))
            EscapeInput(state = state, onType = onType, onSubmit = onSubmit)
            Spacer(Modifier.height(MorningDimens.space12))
            ResultLine(state.result)
            Spacer(Modifier.weight(1f))
            if (!correct) BottomRow(onBack)
            Spacer(Modifier.height(MorningDimens.space12))
        }
    }
}

@Composable
private fun IdleLine(remainingMillis: Long) {
    val seconds = ((remainingMillis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
    val color = if (seconds <= LOW_SECONDS) MorningTheme.colors.statusError else MissionColors.TextGuide
    Row(
        Modifier.fillMaxWidth().padding(top = MorningDimens.space8),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Lucide.Timer, contentDescription = null, tint = color, modifier = Modifier.size(SmallIcon))
        Spacer(Modifier.width(MorningDimens.space4))
        Text(
            text = stringResource(R.string.escape_screen_idle, seconds),
            style = MorningTheme.typography.captionMedium,
            color = color,
        )
    }
}

@Composable
private fun SentenceCard(sentence: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = SENTENCE_FILL_ALPHA), RoundedCornerShape(SentenceRadius))
            .padding(SentencePadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = sentence,
            style = MorningTheme.typography.headlineLarge.copy(fontSize = SentenceSize, fontWeight = FontWeight.Bold),
            color = MissionColors.Text,
            textAlign = TextAlign.Center,
        )
    }
}

/** 피그마 Mission Input(엔터 켬, 글자 20, 오른쪽 여백 56)을 폰 키보드로 쓰는 칸. 틀리면 빨간 테두리 2, 맞으면 초록. */
@Composable
private fun EscapeInput(state: EscapeUiState, onType: (String) -> Unit, onSubmit: () -> Unit) {
    val error = MorningTheme.colors.statusError
    val shape = RoundedCornerShape(MissionRowRadius)
    val wrong = state.result == EscapeResult.Wrong
    val correct = state.result == EscapeResult.Correct
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val textColor = when {
        correct -> SuccessText
        wrong -> error
        else -> MissionColors.Text
    }
    val description = stringResource(R.string.escape_screen_input)
    Box(
        Modifier
            .fillMaxWidth()
            .height(MissionRowHeight)
            .background(if (correct) MissionColors.Success else MissionColors.Fill, shape)
            .then(if (wrong) Modifier.border(WrongBorder, error, shape) else Modifier),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = state.text,
            onValueChange = onType,
            enabled = !correct,
            singleLine = true,
            textStyle = MorningTheme.typography.bodyMedium.copy(
                fontSize = InputSize,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center,
            ),
            cursorBrush = SolidColor(MissionColors.Text),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                autoCorrectEnabled = false,
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = InputEndPadding, end = InputEndPadding)
                .focusRequester(focus)
                .testTag(ESCAPE_INPUT_TAG)
                .semantics { contentDescription = description },
        )
        if (!correct) {
            SubmitButton(onSubmit, Modifier.align(Alignment.CenterEnd).padding(end = SubmitInset))
        }
    }
}

@Composable
private fun SubmitButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val view = LocalView.current
    val description = stringResource(R.string.mission_math_confirm)
    Box(
        modifier = modifier
            .size(SubmitSize)
            .background(Color.White.copy(alpha = SUBMIT_FILL_ALPHA), RoundedCornerShape(SubmitRadius))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
            ) {
                view.perform(MorningHaptic.KeyTap)
                onClick()
            }
            .testTag(ESCAPE_SUBMIT_TAG)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Lucide.Check,
            contentDescription = null,
            tint = Color.White.copy(alpha = SUBMIT_ICON_ALPHA),
            modifier = Modifier.size(MorningDimens.iconMd),
        )
    }
}

@Composable
private fun ResultLine(result: EscapeResult?) {
    val (text, color) = when (result) {
        EscapeResult.Wrong -> stringResource(R.string.escape_screen_wrong) to MorningTheme.colors.statusError
        EscapeResult.Correct -> stringResource(R.string.escape_screen_right) to SuccessText
        null -> stringResource(R.string.escape_screen_help) to MissionColors.TextHint
    }
    Text(
        text = text,
        style = MorningTheme.typography.captionMedium,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun BottomRow(onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Lucide.VolumeX,
            contentDescription = null,
            tint = MissionColors.TextHint,
            modifier = Modifier.size(SmallIcon),
        )
        Spacer(Modifier.width(MorningDimens.space4))
        Text(
            text = stringResource(R.string.escape_screen_muted),
            style = MorningTheme.typography.captionMedium,
            color = MissionColors.TextHint,
        )
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .height(BackPillHeight)
                .background(MissionColors.Fill, CircleShape)
                .clickable(role = Role.Button, onClick = onBack)
                .testTag(ESCAPE_BACK_TAG)
                .padding(horizontal = MorningDimens.space12),
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
}
