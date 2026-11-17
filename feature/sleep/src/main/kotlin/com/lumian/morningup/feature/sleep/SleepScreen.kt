@file:Suppress("MagicNumber") // 잠들기 탭만의 분위기 색과 빛 덩어리 자리예요(design-system.md: 변수 없이 원색).

package com.lumian.morningup.feature.sleep

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.SleepGroup
import com.lumian.morningup.core.model.SleepTrack
import com.lumian.morningup.core.model.SleepTracks
import kotlinx.coroutines.launch

/** Z1 바탕 빛 덩어리(남색, 보라, 자두). */
private val LoungeGlows = listOf(Color(0xFF2B3F8C), Color(0xFF4A2F7A), Color(0xFF1F4C6E))
private const val DRIFT_MILLIS = 14_000

/**
 * Z1 수면 라운지(늘 다크 모드로 감싸서 불러요). 빛 덩어리가 천천히 떠다니고, 위 언더라인 탭을 누르면 그 묶음으로 옮겨가요.
 * 타일을 누르면 [onOpenTrack]으로 Z2 재생 화면을 열어요.
 */
@Composable
fun SleepScreen(onOpenTrack: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = MorningTheme.colors
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    val t by driftState(MorningTheme.reduceMotion)
    Box(
        modifier
            .fillMaxSize()
            .background(colors.backgroundBase)
            .drawBehind { drawLoungeGlows(t) },
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // 위 여백 24+8(11-18 사용자 요청: 홈 말고 모든 화면 위 여백을 늘림)
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 32.dp)) {
                Text(
                    stringResource(R.string.sleep_title),
                    style = MorningTheme.typography.headlineLarge,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.sleep_subtitle),
                    style = MorningTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
            Spacer(Modifier.height(24.dp))
            GroupTabs(selected = tab) { i ->
                tab = i
                scope.launch { listState.animateScrollToItem(i) }
            }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 24.dp, top = 24.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(40.dp),
            ) {
                itemsIndexed(SleepTracks.groups) { _, group -> GroupRow(group, onOpenTrack) }
            }
        }
        // 아래 하단 메뉴 뒤로 목록이 자연스럽게 사라져요.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(140.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, colors.backgroundBase))),
        )
    }
}

@Composable
private fun driftState(still: Boolean): State<Float> {
    if (still) return remember { mutableFloatStateOf(0.5f) }
    return rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(DRIFT_MILLIS), RepeatMode.Reverse),
        label = "drift",
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLoungeGlows(t: Float) {
    listOf(
        Triple(Offset(0.15f + 0.08f * t, 0.12f + 0.04f * t), 0.75f, LoungeGlows[0]),
        Triple(Offset(0.9f - 0.06f * t, 0.42f + 0.05f * t), 0.65f, LoungeGlows[1]),
        Triple(Offset(0.25f + 0.05f * t, 0.88f - 0.06f * t), 0.7f, LoungeGlows[2]),
    ).forEach { (p, r, c) ->
        val center = Offset(size.width * p.x, size.height * p.y)
        val radius = size.width * r
        drawCircle(
            Brush.radialGradient(listOf(c.copy(alpha = 0.45f), c.copy(alpha = 0f)), center, radius),
            radius,
            center,
        )
    }
}

/** 언더라인 탭 줄(왼쪽 24, 사이 24). 고른 탭만 글자가 진하고 아래 줄이 칠해져요. */
@Composable
private fun GroupTabs(selected: Int, onSelect: (Int) -> Unit) {
    val colors = MorningTheme.colors
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        SleepTracks.groups.forEachIndexed { i, group ->
            val on = i == selected
            val textColor by animateColorAsState(if (on) colors.textPrimary else colors.textSecondary, label = "tab")
            val lineColor by animateColorAsState(
                if (on) colors.primaryDefault else Color.Transparent,
                label = "tabLine",
            )
            Column(Modifier.width(IntrinsicSize.Max).morningClickable(onClick = { onSelect(i) })) {
                Text(
                    group.name,
                    style = if (on) MorningTheme.typography.bodyMediumStrong else MorningTheme.typography.bodyMedium,
                    color = textColor,
                    modifier = Modifier.padding(vertical = 10.dp),
                )
                Box(Modifier.fillMaxWidth().height(2.dp).background(lineColor, RoundedCornerShape(1.dp)))
            }
        }
    }
}

@Composable
private fun GroupRow(group: SleepGroup, onOpenTrack: (String) -> Unit) {
    Column {
        Text(group.name, style = MorningTheme.typography.bodySmallStrong, color = MorningTheme.colors.textSecondary)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            group.tracks.forEach { track -> SleepTile(track) { onOpenTrack(track.id) } }
            Spacer(Modifier.width(12.dp))
        }
    }
}

@Composable
private fun SleepTile(track: SleepTrack, onClick: () -> Unit) {
    Column(Modifier.width(112.dp).morningClickable(onClick = onClick)) {
        Box(
            Modifier
                .size(112.dp)
                .clip(RoundedCornerShape(16.dp))
                .drawBehind { drawPaletteBlobs(track.palette.map { Color(it) }) },
        )
        Spacer(Modifier.height(8.dp))
        Text(track.name, style = MorningTheme.typography.bodyMedium, color = MorningTheme.colors.textBody, maxLines = 2)
        Spacer(Modifier.height(2.dp))
        Text(
            stringResource(R.string.sleep_minutes, track.minutes),
            style = MorningTheme.typography.captionMedium,
            color = MorningTheme.colors.textTertiary,
        )
    }
}

/** 썸네일 B+: 바탕 2색 그라데이션 위에 소리 색 3개를 흐리게 섞어요. */
internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPaletteBlobs(palette: List<Color>) {
    val (c0, c1, c2) = palette
    drawRect(Brush.linearGradient(listOf(lerp(c0, Color.Black, 0.35f), lerp(c1, Color.Black, 0.45f))))
    listOf(
        Triple(c0, Offset(0.2f, 0.25f), 0.75f),
        Triple(c1, Offset(0.85f, 0.45f), 0.65f),
        Triple(c2, Offset(0.35f, 0.9f), 0.7f),
    ).forEach { (c, at, a) ->
        val center = Offset(size.width * at.x, size.height * at.y)
        val r = size.width * 0.62f
        drawCircle(Brush.radialGradient(listOf(c.copy(alpha = a), c.copy(alpha = 0f)), center, r), r, center)
    }
}
