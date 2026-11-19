@file:Suppress("MagicNumber") // 예제 그림의 자리와 움직임 비율이라 이름 붙인 상수로 빼면 더 읽기 어려워요.

package com.lumian.morningup.feature.edit

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Smartphone
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.MissionType
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 미션 설정 시트 예제 영역(결정 87, 11-18 사용자 지시). 실제 미션 화면을 단순하게 줄인 그림으로,
 * 무슨 게임인지 알 수 있게 진짜 문제, 단어, 보기를 넣고 푸는 모습을 짧게 되풀이해요.
 * 문제와 단어는 그 단계의 예시(edit_mission_examples_*)라 단계를 바꾸면 예제도 바뀌어요.
 * '애니메이션 줄이기'가 켜져 있으면 답을 넣은 뒤 모습으로 멈춰요.
 */
@Composable
internal fun MissionExample(type: MissionType, level: Int, speed: Int, count: Int, modifier: Modifier = Modifier) {
    val still = MorningTheme.reduceMotion
    val cycle = when (type) {
        MissionType.QuickMath -> 4_000
        MissionType.PopQuiz -> 3_200
        // 속도 1~4: 한 바퀴 7초, 5.2초, 3.8초, 2.8초. 차이가 눈에 띄게 벌렸어요.
        MissionType.DropWords -> listOf(7_000, 5_200, 3_800, 2_800)[(speed - 1).coerceIn(0, 3)]
        MissionType.ShakeShake -> 3_600
    }
    // 단계나 속도가 바뀌면 처음부터 새 빠르기로 다시 돌려요.
    val t by key(type, cycle) { loop(cycle, still, rest = 0.85f) }
    Box(modifier.padding(16.dp)) {
        when (type) {
            MissionType.QuickMath -> QuickMathExample(t, level)
            MissionType.PopQuiz -> PopQuizExample(t, level)
            MissionType.DropWords -> DropWordsExample(t, level)
            MissionType.ShakeShake -> ShakeExample(t, count)
        }
    }
}

/** 0에서 1로 되풀이하는 시간. 멈춤이면 [rest]에 머물러요. */
@Composable
private fun loop(millis: Int, still: Boolean, rest: Float): State<Float> {
    if (still) return remember(rest) { mutableFloatStateOf(rest) }
    val transition = rememberInfiniteTransition(label = "missionExample")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(millis, easing = LinearEasing), RepeatMode.Restart),
        label = "t",
    )
}

private fun example(list: Array<String>, level: Int): String =
    list.getOrElse(level - 1) { list.first() }.removePrefix("예: ")

/** Quick Math 예시 문제의 답(edit_mission_examples_quick_math 순서). */
private val MathAnswers = listOf("12", "85", "168", "226", "988", "1445", "8051")


@Composable
private fun QuickMathExample(t: Float, level: Int) {
    val colors = MorningTheme.colors
    val problem = example(stringArrayResource(R.array.edit_mission_examples_quick_math), level)
    val answer = MathAnswers.getOrElse(level - 1) { MathAnswers.first() }
    // 키패드 없이(11-18 사용자 지시): 답이 한 글자씩 들어가고(0.12~0.5), 입력 칸 안 확인을 눌러 맞혀요(0.6).
    val typed = ((t - 0.12f) / 0.38f * answer.length).toInt().coerceIn(0, answer.length)
    val solved = t >= 0.6f
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "$problem = ?",
            style = MorningTheme.typography.headlineLarge,
            color = colors.textPrimary,
            maxLines = 1,
        )
        Spacer(Modifier.height(24.dp))
        InputBox(
            text = answer.take(typed),
            solved = solved,
            pressingEnter = t >= 0.54f && !solved,
        )
    }
}

/** 미션 입력 칸을 줄인 모양: 글자, 오른쪽 안 확인 버튼. 맞히면 연한 파랑이에요. */
@Composable
private fun InputBox(text: String, solved: Boolean, pressingEnter: Boolean, modifier: Modifier = Modifier) {
    val colors = MorningTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (solved) colors.primarySubtle else colors.surfaceCard)
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MorningTheme.typography.bodyMediumStrong,
            color = if (solved) colors.primaryText else colors.textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .graphicsLayer { if (pressingEnter) scaleX = 0.88f.also { scaleY = it } }
                .clip(CircleShape)
                .background(if (solved || pressingEnter) colors.primaryDefault else colors.controlTrackOff),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Lucide.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun PopQuizExample(t: Float, level: Int) {
    val colors = MorningTheme.colors
    val question = example(stringArrayResource(R.array.edit_mission_examples_pop_quiz), level)
    val options = example(stringArrayResource(R.array.edit_mission_example_quiz_options), level).split("|")
    // 정답 자리는 단계마다 달라요. 0.4에 정답을 누르고 0.5부터 맞힌 표시예요.
    val answerAt = (level - 1) % options.size
    val shown = options.drop(1).toMutableList().apply { add(answerAt, options.first()) }
    val tapped = t >= 0.4f
    val solved = t >= 0.5f
    Column(Modifier.fillMaxSize()) {
        Text(
            text = question,
            style = MorningTheme.typography.titleMedium,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        Spacer(Modifier.height(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            shown.chunked(2).forEachIndexed { row, pair ->
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEachIndexed { column, option ->
                        val right = row * 2 + column == answerAt
                        val pressed = right && tapped && !solved
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .graphicsLayer { if (pressed) scaleX = 0.95f.also { scaleY = it } }
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (right && tapped) colors.primarySubtle else colors.surfaceCard)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = option,
                                style = MorningTheme.typography.bodyMediumStrong,
                                color = if (right && solved) colors.primaryText else colors.textBody,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                            if (right && solved) {
                                Icon(
                                    Lucide.Check,
                                    contentDescription = null,
                                    tint = colors.primaryDefault,
                                    modifier = Modifier.align(Alignment.CenterEnd).size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DropWordsExample(t: Float, level: Int) {
    val colors = MorningTheme.colors
    val words = example(stringArrayResource(R.array.edit_mission_example_drop_words), level).split("|")
    val lanes = listOf(0.05f, 0.55f, 0.25f)
    val starts = listOf(0f, 0.34f, 0.67f)
    // 0~0.8 동안 떨어지고, 0.6부터 한 글자씩 쳐서 0.8에 없어져요. null이면 지금 안 보여요.
    val falls = lanes.indices.map { i ->
        val local = ((t - starts[i]) % 1f + 1f) % 1f
        if (local < 0.8f) local / 0.8f else null
    }
    val typing = lanes.indices.mapNotNull { i ->
        val fall = falls[i]?.takeIf { it > 0.6f } ?: return@mapNotNull null
        val word = words[i % words.size]
        word.take(((fall - 0.6f) / 0.4f * (word.length + 1)).toInt().coerceIn(1, word.length))
    }.lastOrNull().orEmpty()
    Column(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val maxX = constraints.maxWidth
            val maxY = constraints.maxHeight
            lanes.indices.forEach { i ->
                val word = words[i % words.size]
                val fall = falls[i]
                if (fall != null) {
                    val chip = @Composable {
                        Text(
                            text = word,
                            style = MorningTheme.typography.bodyMediumStrong,
                            color = colors.textPrimary,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceCard)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                    Box(
                        Modifier
                            .offset {
                                IntOffset((maxX * lanes[i]).roundToInt(), ((maxY - 36.dp.toPx()) * fall).roundToInt())
                            }
                            .graphicsLayer { alpha = (fall / 0.1f).coerceAtMost(1f) },
                    ) { chip() }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        InputBox(text = typing, solved = false, pressingEnter = false)
    }
}

@Composable
private fun ShakeExample(t: Float, count: Int) {
    val colors = MorningTheme.colors
    val shaking = t < 0.8f
    val done = ((t / 0.8f).coerceAtMost(1f) * count).toInt()
    val angle = if (shaking) sin(t * 2 * PI * 9).toFloat() * 16f else 0f
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Lucide.Smartphone,
                contentDescription = null,
                tint = colors.textBody,
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer {
                        rotationZ = angle
                        translationX = angle * 1.5f
                    },
            )
        }
        Text(
            text = "$done / $count",
            style = MorningTheme.typography.titleMedium,
            color = if (done >= count) colors.primaryText else colors.textPrimary,
        )
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(colors.surfaceCard),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(done / count.toFloat().coerceAtLeast(1f))
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(colors.primaryDefault),
            )
        }
    }
}
