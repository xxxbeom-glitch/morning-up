@file:Suppress("MagicNumber") // 숨쉬는 빛 크기와 소리 색 섞는 비율은 시안 값 그대로예요(design-system.md Z2).

package com.lumian.morningup.feature.sleep

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Timer
import com.composables.icons.lucide.X
import com.lumian.morningup.core.alarm.sleep.SleepPlayback
import com.lumian.morningup.core.alarm.sleep.SleepPlayer
import com.lumian.morningup.core.designsystem.component.MorningIconButton
import com.lumian.morningup.core.designsystem.component.MorningModalSheet
import com.lumian.morningup.core.designsystem.component.MorningRadioRow
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.SleepTrack
import com.lumian.morningup.core.model.SleepTracks
import kotlinx.coroutines.delay

private const val INHALE_MILLIS = 4_000
private const val EXHALE_MILLIS = 6_000
private const val IDLE_HIDE_MILLIS = 5_000L
private val PlayButton = 112.dp
private val PlayIcon = 36.dp

/** 숨쉬는 빛 3겹: 날숨 지름, 들숨 지름, 투명도. */
private val BreathRings = listOf(Triple(150f, 172f, 0.22f), Triple(216f, 250f, 0.16f), Triple(300f, 340f, 0.10f))

/** 들숨 때 버튼 뒤로 퍼지는 물결 2겹의 끝 지름. */
private val Ripples = listOf(380f, 460f)

/**
 * Z2 재생(전체 화면, 하단 메뉴 없음, 늘 다크). 열리면 그 소리를 틀어요. 닫아도 재생은 이어지고 알림으로 조절해요.
 * 소리를 찾지 못하면 [onBack]으로 바로 닫아요.
 */
@Composable
fun SleepPlayerPage(trackId: String, onBack: () -> Unit, viewModel: SleepViewModel = hiltViewModel()) {
    val track = remember(trackId) { SleepTracks.find(trackId) }
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    LaunchedEffect(trackId) {
        if (track == null) onBack() else viewModel.play(trackId)
    }
    if (track == null) return
    MorningTheme(darkTheme = true, reduceMotion = MorningTheme.reduceMotion) {
        SleepPlayerScreen(
            track = track,
            playback = playback?.takeIf { it.trackId == trackId },
            onBack = onBack,
            onToggle = { viewModel.toggle(trackId) },
            onTimer = viewModel::setTimer,
        )
    }
}

@Composable
internal fun SleepPlayerScreen(
    track: SleepTrack,
    playback: SleepPlayback?,
    onBack: () -> Unit,
    onToggle: () -> Unit,
    onTimer: (Int?) -> Unit,
) {
    val playing = playback?.playing == true
    val palette = remember(track.id) { track.palette.map { Color(it) } }
    val glow = remember(palette) { lerp(palette[0], Color.White, 0.5f) }
    var sheet by remember { mutableStateOf(false) }
    // 5초 동안 손대지 않으면(재생 중일 때만) 닫기, 버튼, 글자, 칩이 사라지고 숨쉬는 빛만 남아요.
    var touches by remember { mutableIntStateOf(0) }
    var chromeShown by remember { mutableStateOf(true) }
    LaunchedEffect(touches, playing, sheet) {
        chromeShown = true
        if (playing && !sheet) {
            delay(IDLE_HIDE_MILLIS)
            chromeShown = false
        }
    }
    val chrome by animateFloatAsState(if (chromeShown) 1f else 0f, label = "chrome")
    val breath by breathState(still = MorningTheme.reduceMotion || !playing)
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(lerp(palette[0], Color.Black, 0.3f), lerp(palette[1], Color.Black, 0.75f), NightBase),
                ),
            )
            .drawBehind { drawPlayerGlows(palette, breath) }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        touches++
                    }
                }
            },
    ) {
        MorningIconButton(
            icon = Lucide.X,
            contentDescription = stringResource(R.string.sleep_close),
            onClick = onBack,
            modifier = Modifier.statusBarsPadding().padding(start = 8.dp, top = 8.dp).alpha(chrome),
        )
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(340.dp).drawBehind { drawBreath(glow, breath) }, contentAlignment = Alignment.Center) {
                PlayPauseButton(
                    playing = playing,
                    enabled = chromeShown,
                    onClick = onToggle,
                    modifier = Modifier.alpha(chrome),
                )
            }
            Spacer(Modifier.height(24.dp))
            Column(
                Modifier.alpha(chrome).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    track.name,
                    style = MorningTheme.typography.headlineLarge,
                    color = MorningTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.sleep_meta, SleepTracks.groupOf(track.id)?.name.orEmpty(), track.minutes),
                    style = MorningTheme.typography.bodySmall,
                    color = MorningTheme.colors.textSecondary,
                )
            }
            Spacer(Modifier.weight(1.2f))
            TimerChip(playback, enabled = chromeShown, onClick = { sheet = true }, modifier = Modifier.alpha(chrome))
            Spacer(Modifier.height(48.dp))
        }
        TimerSheet(visible = sheet, current = playback?.timerMinutes, onDismiss = { sheet = false }) {
            onTimer(it)
            sheet = false
        }
    }
}

private val NightBase = Color(0xFF090D18)

@Composable
private fun breathState(still: Boolean): State<Float> {
    if (still) return remember { mutableFloatStateOf(0f) }
    // 들숨 4초 동안 커지고, 날숨 6초 동안 작아져요.
    return rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = INHALE_MILLIS + EXHALE_MILLIS
                0f at 0 using FastOutSlowInEasing
                1f at INHALE_MILLIS using FastOutSlowInEasing
            },
        ),
        label = "breathPhase",
    )
}

/** 배경 빛 덩어리: 위 오른쪽 소리 셋째 색, 아래 왼쪽 둘째 색, 가운데 첫째 색(숨에 맞춰 살짝 움직여요). */
private fun DrawScope.drawPlayerGlows(palette: List<Color>, breath: Float) {
    listOf(
        Triple(Offset(0.85f, 0.12f + 0.02f * breath), 0.7f, palette[2].copy(alpha = 0.40f)),
        Triple(Offset(0.12f, 0.82f - 0.02f * breath), 0.75f, palette[1].copy(alpha = 0.45f)),
        Triple(Offset(0.5f, 0.45f), 0.6f, palette[0].copy(alpha = 0.22f)),
    ).forEach { (p, r, c) ->
        val center = Offset(size.width * p.x, size.height * p.y)
        val radius = size.width * r
        drawCircle(Brush.radialGradient(listOf(c, c.copy(alpha = 0f)), center, radius), radius, center)
    }
}

private fun DrawScope.drawBreath(glow: Color, breath: Float) {
    Ripples.forEach { end ->
        if (breath > 0f) {
            val d = PlayButton.toPx() + (end.dp.toPx() - PlayButton.toPx()) * breath
            drawCircle(glow.copy(alpha = 0.18f * (1f - breath)), d / 2, style = Stroke(1.dp.toPx()))
        }
    }
    BreathRings.asReversed().forEach { (small, large, a) ->
        val d = (small + (large - small) * breath).dp.toPx()
        drawCircle(glow.copy(alpha = a * (1f + 0.4f * breath)), d / 2)
    }
}

@Composable
private fun PlayPauseButton(playing: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val play = remember { Lucide.Play.filled() }
    val pause = remember { Lucide.Pause.filled() }
    Box(
        modifier
            .size(PlayButton)
            .background(MorningTheme.colors.textPrimary, CircleShape)
            .morningClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (playing) pause else play,
            contentDescription = stringResource(if (playing) R.string.sleep_pause else R.string.sleep_play),
            tint = MorningTheme.colors.textInverse,
            modifier = Modifier.size(PlayIcon),
        )
    }
}

/** 종료 타이머 칩: 꺼져 있으면 '종료 타이머', 돌고 있으면 남은 분('28분 남음', 올림). */
@Composable
private fun TimerChip(playback: SleepPlayback?, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val left = playback?.remainingMillis?.takeIf { playback.timerMinutes != null }
    val label = if (left == null) {
        stringResource(R.string.sleep_timer_chip_off)
    } else {
        val minutes = ((left + SleepPlayer.MILLIS_PER_MINUTE - 1) / SleepPlayer.MILLIS_PER_MINUTE).toInt()
        stringResource(R.string.sleep_timer_left, minutes)
    }
    Row(
        modifier
            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(percent = 50))
            .morningClickable(enabled = enabled && playback != null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            Lucide.Timer,
            contentDescription = null,
            tint = MorningTheme.colors.textPrimary,
            modifier = Modifier.size(20.dp),
        )
        Text(label, style = MorningTheme.typography.bodyMediumStrong, color = MorningTheme.colors.textPrimary)
        Spacer(Modifier.width(2.dp))
    }
}

/** Z2-1 종료 타이머 고르기. 완료 버튼 없이 줄을 누르면 바로 정해지고 닫혀요. */
@Composable
private fun TimerSheet(visible: Boolean, current: Int?, onDismiss: () -> Unit, onPick: (Int?) -> Unit) {
    MorningModalSheet(visible = visible, onDismiss = onDismiss, tag = "sleepTimerSheet") {
        Text(
            stringResource(R.string.sleep_timer_sheet_title),
            style = MorningTheme.typography.titleMedium,
            color = MorningTheme.colors.textPrimary,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
        )
        SleepTracks.TIMER_MINUTES.forEach { minutes ->
            MorningRadioRow(
                label = if (minutes == null) {
                    stringResource(R.string.sleep_timer_option_off)
                } else {
                    stringResource(R.string.sleep_timer_option, minutes)
                },
                selected = current == minutes,
                onClick = { onPick(minutes) },
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}
