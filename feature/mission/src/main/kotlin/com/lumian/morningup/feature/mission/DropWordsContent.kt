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
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Delete
import com.composables.icons.lucide.Lucide
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.motion.perform
import com.lumian.morningup.core.designsystem.theme.MissionColors
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.model.mission.DropWordsSpeed
import com.lumian.morningup.core.model.mission.FallingWord
import com.lumian.morningup.core.model.mission.SyllableKeypad
import com.lumian.morningup.feature.mission.play.FeedbackKind
import com.lumian.morningup.feature.mission.play.MissionPlay
import kotlin.math.roundToInt

/** 이름표(테스트용). 음절 키는 `"$DROP_KEY_TAG$index"`. */
const val DROP_KEY_TAG = "dropKey"
const val DROP_DELETE_TAG = "dropDelete"
const val DROP_INPUT_TAG = "dropInput"
const val DROP_FIELD_TAG = "dropField"

/** 피그마 M1: 떨어지는 칸 모서리 20, 키패드 5열. */
private val FieldRadius = 20.dp
private const val KEY_COLUMNS = 5

/** 단어는 칸 가운데에 놓아요. */
private const val LANE_CENTER = 0.5f
private val CursorWidth = 2.dp
private val CursorHeight = 28.dp
private val CursorGap = 4.dp

/** 바닥 가까이(마지막 15%)에서 페이드로 사라져요. */
private const val FLOOR_FADE_FROM = 0.85f

/** 맞힌 단어는 1.2배로 커지며 사라져요. */
private const val POP_SCALE = 1.2f

/**
 * M1 Drop Words 내용. 위에서 단어가 떨어지는 칸(흰색 5%, 모서리 20), 입력칸, 음절 키패드 5x4(19칸 + 지우기).
 * 확인 키는 없어요. 단어를 다 치면 바로 맞힌 것으로 쳐요.
 */
@Composable
internal fun ColumnScope.DropWordsContent(
    play: MissionPlay.Drop,
    nowMillis: Long,
    onKey: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val game = play.game
    val view = LocalView.current
    val density = LocalDensity.current
    val shake = remember { Animatable(0f) }
    val feedback = play.feedback
    LaunchedEffect(feedback?.id) {
        when (feedback?.kind) {
            FeedbackKind.Correct, FeedbackKind.Finished -> view.perform(MorningHaptic.Confirm)
            FeedbackKind.Wrong -> {
                view.perform(MorningHaptic.Reject)
                shake.shakeSideways(with(density) { ShakeDistance.toPx() })
            }
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
            .testTag(DROP_FIELD_TAG),
    ) {
        val laneWidth = maxWidth / DropWordsSpeed.LANES
        val travel = maxHeight
        game.words.forEach { word ->
            key(word.id) {
                FallingWordText(
                    word = word,
                    progress = word.progress(nowMillis, game.fallMillis),
                    laneWidth = laneWidth.value,
                    travel = travel.value,
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
                )
            }
        }
    }
    Spacer(Modifier.height(MorningDimens.space16))
    InputBox(input = game.input, modifier = Modifier.graphicsLayer { translationX = shake.value })
    Spacer(Modifier.height(MorningDimens.space16))
    SyllableKeys(keypad = game.keypad, onKey = onKey, onDelete = onDelete)
    Spacer(Modifier.height(MorningDimens.space24))
}

/** 단어 하나. 가로는 칸 3개 중 자기 칸 가운데, 세로는 떨어진 만큼. 박스 없이 글자만 내려와요. */
@Composable
private fun FallingWordText(word: FallingWord, progress: Float, laneWidth: Float, travel: Float) {
    Text(
        text = word.text,
        style = MissionText.word,
        color = MissionColors.Text,
        modifier = Modifier
            .placeWord(word.lane, progress, laneWidth, travel)
            .graphicsLayer {
                alpha = if (progress > FLOOR_FADE_FROM) (1f - progress) / (1f - FLOOR_FADE_FROM) else 1f
            },
    )
}

@Composable
private fun PoppedWord(word: FallingWord, progress: Float, laneWidth: Float, travel: Float) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, MorningMotion.effects(MorningMotion.EffectsDefault)) }
    Text(
        text = word.text,
        style = MissionText.word,
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
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.place((centerX - placeable.width / 2f).roundToInt(), (maxY * progress).roundToInt().coerceAtLeast(0))
    }
}

@Composable
private fun InputBox(input: String, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.mission_math_input_description, input)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MissionRowHeight)
            .background(MissionColors.Fill, RoundedCornerShape(MissionRowRadius))
            .testTag(DROP_INPUT_TAG)
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = input, style = MissionText.input, color = MissionColors.Text)
        Spacer(Modifier.width(CursorGap))
        Box(Modifier.width(CursorWidth).height(CursorHeight).background(MissionColors.Text))
    }
}

/** 5x4 음절 키. 19칸은 음절, 오른쪽 아래는 지우기(늘 그 자리). 쓴 음절도 흐리게 바꾸지 않아요. */
@Composable
private fun SyllableKeys(keypad: List<String>, onKey: (String) -> Unit, onDelete: () -> Unit) {
    val cells: List<String?> = keypad.take(SyllableKeypad.SIZE) + listOf<String?>(null)
    val deleteLabel = stringResource(R.string.mission_math_delete)
    Column(verticalArrangement = Arrangement.spacedBy(MissionGap)) {
        cells.chunked(KEY_COLUMNS).forEachIndexed { row, rowCells ->
            Row(horizontalArrangement = Arrangement.spacedBy(MissionGap)) {
                rowCells.forEachIndexed { column, syllable ->
                    val index = row * KEY_COLUMNS + column
                    if (syllable == null) {
                        MissionKeyBox(
                            description = deleteLabel,
                            onClick = onDelete,
                            rest = MissionColors.FillWeak,
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
                            modifier = Modifier.weight(1f).testTag("$DROP_KEY_TAG$index"),
                        ) {
                            Text(text = syllable, style = MissionText.key, color = MissionColors.Text)
                        }
                    }
                }
            }
        }
    }
}
