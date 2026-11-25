@file:Suppress("MaxLineLength")

package com.lumian.morningup.feature.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import com.lumian.morningup.core.designsystem.theme.MorningTheme

/**
 * 스플래시 손글씨(결정 170, TSK-206). 앱 아이콘 앞 그림(`app/src/main/res/drawable/ic_launcher_app_foreground.xml`)의
 * 흰 선 8획을 그대로 써서, 잠에 취해 쓰는 것처럼 한 획씩 느릿느릿 그려요. 새로 그리지 않아요.
 * 아이콘을 바꾸면 [STROKES]도 같이 바꿔요(시험이 둘이 같은지 봐요).
 */
internal object SplashHandwriting {
    /** 아이콘 벡터의 획 순서 그대로예요. 좌표는 viewport 540 안에서 [OFFSET]만큼 옮긴 자리예요. */
    val STROKES = listOf(
        "M40.2523 173.06C36.9413 179.803 34.4518 186.951 34.0224 191.151C33.3769 197.465 46.8495 185.805 52.8865 183.013C59.6822 179.869 69.2549 176.258 74.0533 171.885C75.2321 170.811 76.325 169.504 76.9598 168.108C77.5946 166.712 77.5946 165.136 76.9785 164.128C75.7871 162.178 71.8066 162.32 68.284 162.911C66.715 163.174 65.9936 164.683 65.5704 166.079C63.8892 171.626 68.8565 178.979 74.8749 187.357C75.9735 188.886 77.5697 188.574 80.053 185.62C82.5363 182.667 86.2331 176.76 89.7807 169.976C93.3282 163.191 96.6143 155.709 100 148",
        "M105.114 107C105.114 109.859 105.114 112.719 105.114 127.263C105.114 141.808 105.114 167.951 104.908 182.236C104.617 202.413 100.124 208.49 101.153 210.96C105.839 222.203 124.228 222.527 127.341 224.167C134.502 227.94 132.138 237.765 133.797 245C137.502 244.424 148.282 233.358 160.758 217.074C165 210.594 165 207.735 165 204.789",
        "M161.196 160.756C160.423 164.45 158.853 175.981 161.378 182.849C161.881 184.218 163.516 184.709 164.694 184.926C169.506 185.813 174.836 178.56 179.927 172.348C181.021 171.013 182.264 170.278 183.055 168.833C183.846 167.389 184.233 165.343 183.852 163.471C182.355 156.11 174.882 153.342 169.809 147.347C166.668 142.171 164.723 138.042 162.772 134.949C161.981 133.492 161.595 132.265 161.196 131",
        "M161 131C157.808 127.33 153.824 123.244 150.212 117.727C149.408 116.285 148.616 115.07 147.416 113.229C146.216 111.388 144.632 108.958 143 104",
        "M149 168.767C162.337 168.767 176.137 167.968 184.869 166.35C190.881 165.236 197.239 162.322 203.737 160.292C209.867 158.378 216.296 155.851 222.198 154.034C228.365 152.135 231.739 150.193 232.962 148.388C237.532 141.641 225.69 136.877 223.038 132.437C222.339 131.268 221.626 130.02 221.011 130.001C214.567 129.811 224.839 149.333 226.676 160.625C226.883 175.843 226.481 186.93 225.471 189.347C224.863 190.565 224.06 191.764 223.232 193",
        "M250 136C250 149.56 253.274 173.566 257.217 182.703C261.11 185.6 264.868 187.275 268.774 187.895C271.452 188.106 275.544 188.106 281 186.865",
        "M265.087 102C266.765 106.903 268.889 112.697 271.636 119.08C274.304 125.28 282.827 142.709 292.575 166.016C298.324 179.762 299.423 187.379 296.256 200.175C294.116 208.821 289.275 217.093 286.292 223.315C283.702 228.718 281.212 234.03 276.341 240.233C274.026 243.182 269.347 243.143 265.538 244.369C263.828 245.594 262.569 248.046 261.71 250.126C260.852 252.206 260.432 253.84 260 258",
        "M283 184C286.239 187.129 292.753 190.259 299.677 196.135C304.204 199.977 309.864 202.024 314.961 203.018C316.391 203.212 318.004 203.6 320.046 203.8C322.087 204 324.507 204 327 204",
    )

    const val VIEWPORT = 540f
    const val OFFSET = 90f
    const val STROKE_WIDTH = 5.6f

    /** 8획을 다 쓰는 시간(획 사이 쉼 포함). 결정 170 가안 약 3초. 빠르기를 바꿀 때는 이 값과 아래 표만 고쳐요. */
    const val DRAW_MILLIS = 3_000

    /** 다 쓴 뒤 멈춰 보여 주는 시간. */
    const val HOLD_MILLIS = 500

    /** 스플래시 전체 시간. */
    const val TOTAL_MILLIS = DRAW_MILLIS + HOLD_MILLIS

    /** 획마다 느림 배수(클수록 같은 길이를 더 천천히). 고르지 않게 머뭇거리다 느려지다 해요. */
    private val PACE = listOf(1.15f, 0.85f, 1.35f, 0.75f, 1.0f, 1.3f, 0.8f, 1.1f)

    /** 획 사이 쉼(밀리초). 이어지는 획(3→4)은 짧게, 글자 사이(1→2, 4→5, 5→6)는 길게 쉬어요. */
    private val PAUSES = listOf(170, 90, 50, 210, 160, 110, 130)

    /** 획 안에서의 빠르기. 머뭇거리다 서두르기, 서두르다 늘어지기를 섞어요. */
    private val EASINGS: List<Easing> = listOf(
        CubicBezierEasing(0.55f, 0f, 0.35f, 1f),
        CubicBezierEasing(0.2f, 0.5f, 0.45f, 1f),
        CubicBezierEasing(0.65f, 0.05f, 0.4f, 0.95f),
        CubicBezierEasing(0.3f, 0.3f, 0.6f, 1f),
        CubicBezierEasing(0.5f, 0f, 0.25f, 1f),
        CubicBezierEasing(0.7f, 0.1f, 0.3f, 1f),
        CubicBezierEasing(0.25f, 0.45f, 0.55f, 1f),
        CubicBezierEasing(0.6f, 0f, 0.4f, 1f),
    )

    /** 한 획이 그려지는 시간 구간(밀리초). */
    data class Slot(val start: Float, val end: Float)

    /** 획 길이([lengths])로 시간표를 만들어요. 쉼을 뺀 시간을 길이 x 느림 배수 비율로 나눠요. */
    fun timeline(lengths: List<Float>): List<Slot> {
        val weights = lengths.mapIndexed { i, length -> length * PACE[i % PACE.size] }
        val pauses = (0 until lengths.size - 1).map { PAUSES[it % PAUSES.size].toFloat() }
        val drawing = DRAW_MILLIS - pauses.sum()
        val total = weights.sum().takeIf { it > 0f } ?: 1f
        var t = 0f
        return weights.mapIndexed { i, w ->
            val start = t
            val end = start + drawing * w / total
            t = end + (pauses.getOrNull(i) ?: 0f)
            Slot(start, end)
        }
    }

    /** [elapsed] 밀리초에 [index]번 획이 얼마나 그려졌는지(0~1). */
    fun progress(slots: List<Slot>, index: Int, elapsed: Float): Float {
        val slot = slots[index]
        val raw = ((elapsed - slot.start) / (slot.end - slot.start)).coerceIn(0f, 1f)
        return EASINGS[index % EASINGS.size].transform(raw)
    }
}

/** 아이콘 배경색(`ic_launcher_app_background`)과 같아요. */
internal val SplashBackground = Color(0xFF171A1F)

/**
 * 손글씨 로고. 움직임 줄이기를 켜면 다 쓴 글자를 바로 보여 줘요.
 * 크기는 [modifier]가 정하고, 아이콘 viewport 540을 그 칸에 맞춰 그려요.
 */
@Composable
internal fun SplashHandwritingLogo(modifier: Modifier = Modifier) {
    val reduceMotion = MorningTheme.reduceMotion
    val strokes = remember {
        SplashHandwriting.STROKES.map { data ->
            val path = PathParser().parsePathString(data).toPath()
            val measure = PathMeasure().apply { setPath(path, false) }
            path to measure
        }
    }
    val slots = remember(strokes) { SplashHandwriting.timeline(strokes.map { it.second.length }) }
    val elapsed = remember { Animatable(if (reduceMotion) SplashHandwriting.DRAW_MILLIS.toFloat() else 0f) }
    LaunchedEffect(reduceMotion) {
        if (!reduceMotion) {
            elapsed.animateTo(
                SplashHandwriting.DRAW_MILLIS.toFloat(),
                tween(SplashHandwriting.DRAW_MILLIS, easing = LinearEasing),
            )
        }
    }
    val segment = remember { Path() }
    Canvas(modifier) {
        val k = size.minDimension / SplashHandwriting.VIEWPORT
        val pen = Stroke(
            width = SplashHandwriting.STROKE_WIDTH,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        scale(k, pivot = Offset.Zero) {
            translate(SplashHandwriting.OFFSET, SplashHandwriting.OFFSET) {
                strokes.forEachIndexed { i, (path, measure) ->
                    val p = SplashHandwriting.progress(slots, i, elapsed.value)
                    when {
                        p <= 0f -> Unit
                        p >= 1f -> drawPath(path, Color.White, style = pen)
                        else -> {
                            segment.reset()
                            measure.getSegment(0f, measure.length * p, segment, true)
                            drawPath(segment, Color.White, style = pen)
                        }
                    }
                }
            }
        }
    }
}
