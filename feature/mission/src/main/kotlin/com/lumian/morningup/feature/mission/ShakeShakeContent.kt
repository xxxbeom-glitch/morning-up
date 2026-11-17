package com.lumian.morningup.feature.mission

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumian.morningup.core.designsystem.component.RollDirection
import com.lumian.morningup.core.designsystem.component.RollingText
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.motion.perform
import com.lumian.morningup.core.designsystem.theme.MissionColors
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.mission.ShakeCounter
import com.lumian.morningup.feature.mission.play.MissionPlay
import kotlinx.coroutines.launch

/** 피그마 M4 Mission Ring: 지름 280, 두께 6. */
private val RingSize = 280.dp
private val RingStroke = 6.dp
private val CountSize = 120.sp
private const val COUNT_POP_SCALE = 1.12f
private val CountJiggle = 6.dp
private const val NANOS_PER_MILLI = 1_000_000L

/** 남은 횟수 이름표(테스트용). */
const val SHAKE_COUNT_TAG = "shakeCount"

/**
 * M4 Shake Shake 내용. 위 줄 없이 가운데 큰 시간 고리 안에 남은 횟수와 안내만 둬요.
 * 흔들 때마다 숫자가 아래로 굴러 하나 줄고, 순간 커졌다 돌아오며 좌우로 같이 흔들려요(틱 진동).
 */
@Composable
internal fun ColumnScope.ShakeShakeContent(play: MissionPlay.Shake, remainingMillis: Long) {
    val view = LocalView.current
    val density = LocalDensity.current
    val reduceMotion = MorningTheme.reduceMotion
    val pop = remember { Animatable(1f) }
    val jiggle = remember { Animatable(0f) }
    LaunchedEffect(play.feedback?.id) {
        if (play.feedback == null) return@LaunchedEffect
        view.perform(MorningHaptic.Tick)
        if (reduceMotion) return@LaunchedEffect
        launch {
            pop.snapTo(COUNT_POP_SCALE)
            pop.animateTo(1f, MorningMotion.spatial(MorningMotion.SpatialFast, reduceMotion = false))
        }
        jiggle.shakeSideways(with(density) { CountJiggle.toPx() })
    }
    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        MissionTimer(
            remainingMillis = remainingMillis,
            diameter = RingSize,
            strokeWidth = RingStroke,
            track = MissionColors.RingTrack,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RollingText(
                text = play.game.remaining.toString(),
                style = MorningTheme.typography.displayLarge.copy(
                    fontSize = CountSize,
                    lineHeight = CountSize,
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = MissionColors.Text,
                direction = RollDirection.ByValue,
                modifier = Modifier.graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                    translationX = jiggle.value
                },
            )
            Spacer(Modifier.height(MorningDimens.space4))
            Text(
                text = stringResource(R.string.mission_shake_guide),
                style = MissionText.guide,
                color = MissionColors.TextGuide,
            )
        }
    }
}

/**
 * 가속도 센서를 듣다가 한 번 흔들었다고 보면 [onShake]를 불러요(세기 기준과 0.25초 묶기는 [ShakeCounter]).
 * 화면에 있는 동안만 들어요.
 */
@Composable
internal fun ShakeSensor(onShake: () -> Unit) {
    val context = LocalContext.current
    val currentOnShake by rememberUpdatedState(onShake)
    DisposableEffect(context) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val counter = ShakeCounter()
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = Triple(event.values[0], event.values[1], event.values[2])
                if (counter.onSample(x, y, z, event.timestamp / NANOS_PER_MILLI)) currentOnShake()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (manager != null && sensor != null) {
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose { manager?.unregisterListener(listener) }
    }
}
