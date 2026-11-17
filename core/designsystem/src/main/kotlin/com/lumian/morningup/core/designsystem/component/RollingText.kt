package com.lumian.morningup.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.theme.MorningTheme

/** 숫자가 바뀌면 굴러 바뀌는 방향. */
enum class RollDirection { Up, ByValue }

/**
 * 숫자가 바뀔 때 한 글자씩 굴러 바뀌는 글자(홈 상단 '몇 시간 몇 분 후', − + 버튼 값).
 * 숫자 칸만 움직이고 나머지 글자는 그대로 둬요. 줄바꿈(\n)으로 여러 줄을 가운데 정렬해 그려요.
 * [RollDirection.Up]은 늘 위로, [RollDirection.ByValue]는 늘면 위로 줄면 아래로 굴러요.
 * '애니메이션 줄이기'면 움직이지 않고 페이드만 해요.
 */
@Composable
fun RollingText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    direction: RollDirection = RollDirection.Up,
) {
    Column(
        modifier = modifier
            .clearAndSetSemantics { contentDescription = text.replace('\n', ' ') },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        text.split('\n').forEach { line ->
            Row {
                splitRuns(line).forEach { run ->
                    if (run.isDigit) {
                        RollingDigit(run.text[0], style, color, direction)
                    } else {
                        Text(text = run.text, style = style, color = color)
                    }
                }
            }
        }
    }
}

@Composable
private fun RollingDigit(digit: Char, style: TextStyle, color: Color, direction: RollDirection) {
    val reduceMotion = MorningTheme.reduceMotion
    AnimatedContent(
        targetState = digit,
        transitionSpec = {
            val up = direction == RollDirection.Up || targetState > initialState
            rollTransform(up, reduceMotion)
        },
        label = "rollingDigit",
    ) { value ->
        Text(text = value.toString(), style = style, color = color)
    }
}

private fun rollTransform(up: Boolean, reduceMotion: Boolean): ContentTransform {
    val fade = fadeIn(MorningMotion.effects(MorningMotion.EffectsDefault)) togetherWith
        fadeOut(MorningMotion.effects(MorningMotion.EffectsFast))
    if (reduceMotion) return fade
    val spec = MorningMotion.spatial<IntOffset>(MorningMotion.SpatialDefault, false)
    val enter = slideInVertically(spec) { if (up) it else -it } +
        fadeIn(MorningMotion.effects(MorningMotion.EffectsDefault))
    val exit = slideOutVertically(spec) { if (up) -it else it } +
        fadeOut(MorningMotion.effects(MorningMotion.EffectsFast))
    return ContentTransform(enter, exit, sizeTransform = SizeTransform(clip = true))
}

/** 글자 한 덩어리. 숫자는 한 글자씩, 나머지는 이어진 덩어리로 나눠요. */
internal data class TextRun(val text: String, val isDigit: Boolean)

internal fun splitRuns(line: String): List<TextRun> {
    val runs = mutableListOf<TextRun>()
    val plain = StringBuilder()
    line.forEach { c ->
        if (c.isDigit()) {
            if (plain.isNotEmpty()) runs += TextRun(plain.toString(), false).also { plain.clear() }
            runs += TextRun(c.toString(), true)
        } else {
            plain.append(c)
        }
    }
    if (plain.isNotEmpty()) runs += TextRun(plain.toString(), false)
    return runs
}
