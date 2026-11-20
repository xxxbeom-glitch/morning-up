package com.lumian.morningup.feature.mission

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Delete
import com.composables.icons.lucide.Lucide
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.motion.perform
import com.lumian.morningup.core.designsystem.theme.FixedFontScale
import com.lumian.morningup.core.designsystem.theme.MissionColors
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.mission.DropKeypadLayout
import com.lumian.morningup.core.model.mission.DropWordsSpeed
import com.lumian.morningup.core.model.mission.FallingWord
import com.lumian.morningup.feature.mission.play.FeedbackKind
import com.lumian.morningup.feature.mission.play.MissionPlay
import kotlin.math.roundToInt

/** 이름표(테스트용). 음절 키는 `"$DROP_KEY_TAG$index"`. */
const val DROP_KEY_TAG = "dropKey"
const val DROP_DELETE_TAG = "dropDelete"
const val DROP_INPUT_TAG = "dropInput"
const val DROP_FIELD_TAG = "dropField"

/** 피그마 M1: 떨어지는 칸 모서리 20. */
private val FieldRadius = 20.dp

/** 떨어지는 칸 좌우 안쪽 여백(화면 좌우 여백과 같음, 11-20 사용자 지시). 긴 단어가 칸 밖으로 잘리지 않게 해요. */
private val FieldSidePadding = 16.dp

/** 키패드 영역 높이는 5x4(키 56, 간격 10) 그대로예요. 칸이 늘면 키가 작아져요(11-20 사용자 지시). */
private val KeypadAreaHeight = MissionKeyHeight * 4 + MissionGap * 3

/** 열 수에 따른 키 간격, 글자 크기(sp), 모서리. 8열(360dp 폭에서 키 폭 약 36dp)까지 눌러 틀리지 않게 줄여요. */
private data class KeyLook(val gap: Dp, val fontSp: Int, val radius: Dp)

/** 5열, 6열, 7열, 8열 차례. */
private val KeyLooks = listOf(
    KeyLook(MissionGap, 24, MissionKeyRadius),
    KeyLook(8.dp, 22, MissionKeyRadius),
    KeyLook(6.dp, 20, 12.dp),
    KeyLook(6.dp, 18, 12.dp),
)
private const val FIRST_LOOK_COLUMNS = 5

private fun keyLook(columns: Int): KeyLook = KeyLooks[(columns - FIRST_LOOK_COLUMNS).coerceIn(0, KeyLooks.lastIndex)]

/** 단어는 칸 가운데에 놓아요. */
private const val LANE_CENTER = 0.5f

/** 바닥 가까이(마지막 15%)에서 페이드로 사라져요. */
private const val FLOOR_FADE_FROM = 0.85f

/** 맞힌 단어는 1.2배로 커지며 사라져요. */
private const val POP_SCALE = 1.2f

/**
 * M1 Drop Words 내용. 위에서 단어가 떨어지는 칸(흰색 5%, 모서리 20, 좌우 여백 16), 입력칸, 음절 키패드(단계별 5x4~8x5, 지우기 포함).
 * 확인 키는 없어요. 단어를 다 치면 바로 맞힌 것으로 쳐요.
 */
@Composable
internal fun ColumnScope.DropWordsContent(
    play: MissionPlay.Drop,
    nowMillis: Long,
    onKey: (String) -> Unit,
    onDelete: () -> Unit,
    onSubmit: () -> Unit = {},
) {
    val game = play.game
    val view = LocalView.current
    val feedback = play.feedback
    LaunchedEffect(feedback?.id) {
        when (feedback?.kind) {
            FeedbackKind.Correct, FeedbackKind.Finished -> view.perform(MorningHaptic.Confirm)
            // 결정 93: 큰 표시 없이 입력 칸 테두리와 글자만 빨개지고, 약 1초 뒤 칸이 비워져요.
            FeedbackKind.Wrong -> view.perform(MorningHaptic.Reject)
            null -> Unit
        }
    }
    Spacer(Modifier.height(MorningDimens.space16))
    BoxWithConstraints(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .background(MissionColors.FillFaint, RoundedCornerShape(FieldRadius))
            .clipToBounds()
            .padding(horizontal = FieldSidePadding)
            .testTag(DROP_FIELD_TAG),
    ) {
        val laneWidth = maxWidth / DropWordsSpeed.LANES
        val travel = maxHeight
        val fit = rememberWordFit(maxWidth)
        game.words.forEach { word ->
            key(word.id) {
                FallingWordText(
                    word = word,
                    progress = word.progress(nowMillis, game.fallMillis),
                    laneWidth = laneWidth.value,
                    travel = travel.value,
                    style = fit(word.text),
                )
            }
        }
        val popped = feedback?.word?.takeIf { feedback.kind != FeedbackKind.Wrong }
        if (popped != null) {
            key(feedback.id) {
                PoppedWord(
                    word = popped,
                    progress = popped.progress(nowMillis, game.fallMillis),
                    laneWidth.value,
                    travel.value,
                    style = fit(popped.text),
                )
            }
        }
    }
    Spacer(Modifier.height(MorningDimens.space16))
    MissionInputField(
        value = play.wrongInput ?: game.input,
        wrong = play.holding,
        onEnter = onSubmit,
        tag = DROP_INPUT_TAG,
        placeholder = stringResource(R.string.mission_drop_placeholder),
    )
    Spacer(Modifier.height(MorningDimens.space16))
    SyllableKeys(
        keypad = game.keypad,
        layout = DropKeypadLayout.of(game.level),
        onKey = onKey,
        onDelete = onDelete,
    )
    Spacer(Modifier.height(MorningDimens.space24))
}

/** 단어 글자 모양. 여백 안 칸 폭([width])에 한 줄로 안 들어가는 긴 단어는 글자 크기를 줄여 맞춰요(11-20 사용자 지시). */
@Composable
private fun rememberWordFit(width: Dp): (String) -> TextStyle {
    val base = MissionText.word
    val measurer = rememberTextMeasurer()
    val widthPx = with(LocalDensity.current) { width.roundToPx() }
    return remember(base, measurer, widthPx) {
        val cache = HashMap<String, TextStyle>()
        val fit: (String) -> TextStyle = { text ->
            cache.getOrPut(text) {
                val measured = measurer.measure(text, base, maxLines = 1, softWrap = false).size.width
                if (measured <= widthPx || measured == 0) {
                    base
                } else {
                    base.copy(fontSize = base.fontSize * (widthPx.toFloat() / measured * FIT_MARGIN))
                }
            }
        }
        fit
    }
}

/** 줄인 글자가 칸 끝에 딱 붙지 않게 조금 더 줄여요. */
private const val FIT_MARGIN = 0.97f

/** 단어 하나. 가로는 칸 3개 중 자기 칸 가운데(칸 밖으로 나가지 않게 맞춤), 세로는 떨어진 만큼. 박스 없이 글자만 내려와요. */
@Composable
private fun FallingWordText(word: FallingWord, progress: Float, laneWidth: Float, travel: Float, style: TextStyle) {
    Text(
        text = word.text,
        style = style,
        maxLines = 1,
        softWrap = false,
        color = MissionColors.Text,
        modifier = Modifier
            .placeWord(word.lane, progress, laneWidth, travel)
            .graphicsLayer {
                alpha = if (progress > FLOOR_FADE_FROM) (1f - progress) / (1f - FLOOR_FADE_FROM) else 1f
            },
    )
}

@Composable
private fun PoppedWord(word: FallingWord, progress: Float, laneWidth: Float, travel: Float, style: TextStyle) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, MorningMotion.effects(MorningMotion.EffectsDefault)) }
    Text(
        text = word.text,
        style = style,
        maxLines = 1,
        softWrap = false,
        color = MissionColors.Text,
        modifier = Modifier
            .placeWord(word.lane, progress, laneWidth, travel)
            .graphicsLayer {
                val scale = 1f + (POP_SCALE - 1f) * anim.value
                scaleX = scale
                scaleY = scale
                alpha = 1f - anim.value
            },
    )
}

/** 칸 안 자리: [laneWidth], [travel]은 dp 값이에요. 바닥에 닿을 때 글자 아래끝이 칸 아래끝과 맞아요. */
private fun Modifier.placeWord(
    lane: Int,
    progress: Float,
    laneWidth: Float,
    travel: Float,
): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    val centerX = (lane + LANE_CENTER) * laneWidth.dp.toPx()
    val maxY = travel.dp.toPx() - placeable.height
    // 가장자리 칸의 긴 단어도 여백 안에 머물게 왼쪽, 오른쪽 끝에 맞춰요.
    val maxX = (constraints.maxWidth - placeable.width).coerceAtLeast(0)
    val x = (centerX - placeable.width / 2f).roundToInt().coerceIn(0, maxX)
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.place(x, (maxY * progress).roundToInt().coerceAtLeast(0))
    }
}

/**
 * 음절 키(단계별 5x4~8x5). 오른쪽 아래는 지우기(늘 그 자리). 쓴 음절도 흐리게 바꾸지 않아요.
 * 영역 높이는 5x4와 같고, 칸이 늘면 키와 간격, 글자가 작아져요.
 */
@Composable
private fun SyllableKeys(
    keypad: List<String>,
    layout: DropKeypadLayout,
    onKey: (String) -> Unit,
    onDelete: () -> Unit,
) {
    FixedFontScale { SyllableRows(keypad, layout, onKey, onDelete) }
}

@Composable
private fun SyllableRows(
    keypad: List<String>,
    layout: DropKeypadLayout,
    onKey: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val cells: List<String?> = keypad.take(layout.size) + listOf<String?>(null)
    val deleteLabel = stringResource(R.string.mission_math_delete)
    val look = keyLook(layout.columns)
    val keyHeight = (KeypadAreaHeight - look.gap * (layout.rows - 1)) / layout.rows
    val keyText = MissionText.key.copy(fontSize = look.fontSp.sp)
    Column(verticalArrangement = Arrangement.spacedBy(look.gap)) {
        cells.chunked(layout.columns).forEachIndexed { row, rowCells ->
            Row(horizontalArrangement = Arrangement.spacedBy(look.gap)) {
                rowCells.forEachIndexed { column, syllable ->
                    val index = row * layout.columns + column
                    if (syllable == null) {
                        MissionKeyBox(
                            description = deleteLabel,
                            onClick = onDelete,
                            rest = MissionColors.FillWeak,
                            height = keyHeight,
                            radius = look.radius,
                            modifier = Modifier.weight(1f).testTag(DROP_DELETE_TAG),
                        ) {
                            Icon(
                                imageVector = Lucide.Delete,
                                contentDescription = null,
                                tint = MissionColors.Text,
                                modifier = Modifier.size(MorningDimens.iconMd),
                            )
                        }
                    } else {
                        MissionKeyBox(
                            description = syllable,
                            onClick = { onKey(syllable) },
                            height = keyHeight,
                            radius = look.radius,
                            modifier = Modifier.weight(1f).testTag("$DROP_KEY_TAG$index"),
                        ) {
                            Text(text = syllable, style = keyText, color = MissionColors.Text)
                        }
                    }
                }
            }
        }
    }
}
