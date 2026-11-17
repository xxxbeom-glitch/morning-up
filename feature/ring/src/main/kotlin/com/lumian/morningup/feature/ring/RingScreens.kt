package com.lumian.morningup.feature.ring

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.lumian.morningup.core.designsystem.component.MorningButton
import com.lumian.morningup.core.designsystem.component.MorningButtonSize
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningFixedColors
import com.lumian.morningup.core.designsystem.theme.MorningShadows
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.designsystem.theme.morningShadow
import com.lumian.morningup.core.model.DayPart
import com.lumian.morningup.core.ui.AlarmText
import java.time.LocalDate
import java.time.LocalTime

/** 피그마 W1 Wake Dialog 폭과 모서리, 위 여백, 버튼 줄 높이. */
private val WakeCardWidth = 328.dp
private val WakeCardRadius = 28.dp
private val WakeCardTop = 36.dp
private val WakeTitleGap = 10.dp
private val WakeMessageGap = 28.dp
private val WakeButtonHeight = 56.dp
private val DividerHeight = 1.dp

/**
 * 울림 화면 하늘(D0, W1 뒤, W2의 Ring Plain). 아침은 `sky/2 → sky/5 → 바탕`, 밤은 `sky/night-top → night-bottom`.
 * 해, 달, 날씨 장면은 7단계예요.
 */
@Composable
fun RingSky(dayPart: DayPart, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    val colors = MorningTheme.colors
    val brush = when (dayPart) {
        DayPart.Night -> Brush.verticalGradient(listOf(colors.skyNightTop, colors.skyNightBottom))
        DayPart.Morning -> Brush.verticalGradient(listOf(colors.sky2, colors.sky5, colors.backgroundBase))
    }
    Box(modifier = modifier.fillMaxSize().background(brush), content = content)
}

/**
 * D0 알람 울림. 하늘, 날짜, 시각, 원형 버튼만 있어요(스누즈 없음).
 * 미션이 없으면 버튼 글만 '알람 끄기'예요. [onButtonCenter]는 미션 화면이 퍼져 나올 자리예요.
 */
@Composable
fun RingingScreen(
    date: LocalDate,
    time: LocalTime,
    dayPart: DayPart,
    hasMissions: Boolean,
    onMainButton: () -> Unit,
    modifier: Modifier = Modifier,
    onButtonCenter: (Offset) -> Unit = {},
) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    val resources = LocalContext.current.resources
    RingSky(dayPart, modifier) {
        Column(
            modifier = Modifier.align(Alignment.Center).systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = AlarmText.dateLong(resources, date), style = typography.bodySmall, color = colors.textBody)
            Spacer(Modifier.height(MorningDimens.space4))
            Text(
                text = stringResource(R.string.ring_clock, AlarmText.hour12(time.hour), time.minute),
                style = typography.displayLarge12h,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(MorningDimens.space40))
            MorningButton(
                text = stringResource(if (hasMissions) R.string.ring_start_mission else R.string.ring_dismiss),
                onClick = onMainButton,
                size = MorningButtonSize.Circle,
                haptic = MorningHaptic.Confirm,
                modifier = Modifier.onGloballyPositioned { onButtonCenter(it.boundsInRoot().center) },
            )
        }
    }
}

/**
 * W1 해제 완료. 하늘 위에 딤을 깔고 가운데 카드(인사, 한마디, '확인').
 * 피그마는 홈 위에 딤이지만, 잠금 화면 위 액티비티라 홈 대신 같은 하늘을 깔아요.
 */
@Composable
fun WakeUpScreen(dayPart: DayPart, onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    val morning = dayPart == DayPart.Morning
    val shape = RoundedCornerShape(WakeCardRadius)
    RingSky(dayPart, modifier) {
        Box(
            Modifier
                .fillMaxSize()
                .background(if (morning) MorningFixedColors.WakeDimMorning else MorningFixedColors.WakeDimNight),
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = MorningDimens.layoutScreenMargin)
                .widthIn(max = WakeCardWidth)
                .fillMaxWidth()
                .morningShadow(MorningShadows.float, shape)
                .background(colors.backgroundElevated, shape),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(WakeCardTop))
            Text(
                text = stringResource(
                    if (morning) R.string.ring_wake_up_title_morning else R.string.ring_wake_up_title_night,
                ),
                style = typography.headlineLarge,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(WakeTitleGap))
            Text(
                text = stringResource(
                    if (morning) R.string.ring_wake_up_message_morning else R.string.ring_wake_up_message_night,
                ),
                style = typography.bodyMedium,
                color = colors.textBody,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = MorningDimens.space24),
            )
            Spacer(Modifier.height(WakeMessageGap))
            Box(Modifier.fillMaxWidth().height(DividerHeight).background(colors.borderDivider))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WakeButtonHeight)
                    .morningClickable(haptic = MorningHaptic.Press, onClick = onConfirm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.ring_wake_up_ok),
                    style = typography.titleMedium,
                    color = colors.primaryText,
                )
            }
        }
    }
}

/**
 * W2 깨어 있나요. 남은 시간을 크게 보여 주고, 원형 '깼어요'를 누르면 체크로 바뀐 뒤 닫혀요.
 */
@Composable
fun WakeCheckScreen(
    dayPart: DayPart,
    answerSeconds: Int,
    remainingMillis: Long,
    confirmed: Boolean,
    onAwake: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    val resources = LocalContext.current.resources
    RingSky(dayPart, modifier) {
        Column(
            modifier = Modifier.align(Alignment.Center).systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.ring_wake_check_title),
                style = typography.headlineLarge,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(MorningDimens.space8))
            Text(
                text = stringResource(R.string.ring_wake_check_message, AlarmText.duration(resources, answerSeconds)),
                style = typography.bodyMedium,
                color = colors.textBody,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(MorningDimens.space32))
            Text(
                text = AlarmText.countdown(resources, remainingMillis),
                style = typography.displayLarge12h,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(MorningDimens.space40))
            AwakeButton(confirmed = confirmed, onClick = onAwake)
        }
    }
}

@Composable
private fun AwakeButton(confirmed: Boolean, onClick: () -> Unit) {
    val colors = MorningTheme.colors
    val reduceMotion = MorningTheme.reduceMotion
    val doneLabel = stringResource(R.string.ring_wake_check_done)
    Box(
        modifier = Modifier
            .size(MorningDimens.sizeButtonCircle)
            .background(colors.primaryDefault, CircleShape)
            .morningClickable(haptic = MorningHaptic.Confirm, enabled = !confirmed, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = confirmed,
            transitionSpec = {
                fadeIn(MorningMotion.spatial(MorningMotion.SpatialFast, reduceMotion)) togetherWith
                    fadeOut(MorningMotion.spatial(MorningMotion.SpatialFast, reduceMotion))
            },
            label = "awake",
        ) { done ->
            if (done) {
                Icon(
                    imageVector = Lucide.Check,
                    contentDescription = null,
                    tint = colors.primaryOn,
                    modifier = Modifier.size(MorningDimens.iconLg).semantics { contentDescription = doneLabel },
                )
            } else {
                Text(
                    text = stringResource(R.string.ring_wake_check_awake),
                    style = MorningTheme.typography.titleMedium,
                    color = colors.primaryOn,
                )
            }
        }
    }
}
