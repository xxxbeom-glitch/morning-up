package com.lumian.morningup.feature.ring

import androidx.compose.foundation.Image
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lumian.morningup.core.designsystem.component.MorningButton
import com.lumian.morningup.core.designsystem.component.MorningButtonSize
import com.lumian.morningup.core.designsystem.component.MorningIconButton
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.FixedFontScale
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningFixedColors
import com.lumian.morningup.core.designsystem.theme.MorningShadows
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.designsystem.theme.morningShadow
import com.lumian.morningup.core.model.ButtonTrick
import com.lumian.morningup.core.model.DayPart
import com.lumian.morningup.core.model.WeatherKind
import com.lumian.morningup.core.ui.AlarmBackgrounds
import com.lumian.morningup.core.ui.AlarmText
import com.lumian.morningup.core.ui.rememberBackgroundPhoto
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
 * D0 알람 울림. 하늘, 날짜, 시각, 원형 버튼. 미루기를 켰고 횟수가 남았으면([snoozeMinutes]) 버튼 아래 작은 글자 버튼
 * 'n분 뒤 다시'가 있어요(결정 195, 시안 TSK-281). 미션이 없으면 버튼 글만 '알람 끄기'예요.
 * [onButtonCenter]는 미션 화면이 퍼져 나올 자리예요.
 */
@Composable
fun RingingScreen(
    date: LocalDate,
    time: LocalTime,
    dayPart: DayPart,
    hasMissions: Boolean,
    onMainButton: () -> Unit,
    modifier: Modifier = Modifier,
    background: String? = null,
    onButtonCenter: (Offset) -> Unit = {},
    trick: ButtonTrick? = null,
    trickKey: Any? = null,
    trickListener: TrickListener = TrickListener.None,
    trickRun: TrickRun? = null,
    snoozeMinutes: Int? = null,
    onSnooze: () -> Unit = {},
) {
    // 울림 화면은 뷰모델이 울림 한 번 동안 들고 있는 [trickRun]을 줘요(화면을 껐다 켜도 이어져요, ISS-048).
    // 미리 해보기는 [trick]만 주고, [trickKey]가 바뀌면 처음부터 다시 걸려요.
    val local = remember(trickKey, trick) { trick?.let { TrickRun(it) } }
    val run = trickRun ?: local
    ChosenBackground(background, dayPart, modifier) {
        RingingContent(
            date, time, hasMissions, onMainButton, onButtonCenter, run, trickListener, snoozeMinutes, onSnooze,
        )
    }
}

/**
 * S8에서 고른 배경. 이름 있는 배경은 위아래 그라데이션, 내 사진은 꽉 채우고 글자가 읽히게 살짝 어둡게 덮어요.
 * 어두운 배경 위에서는 글자를 다크 모드 색으로 바꿔요. 고르지 않았거나 사진을 못 읽으면 기본 하늘이에요.
 */
@Composable
private fun ChosenBackground(
    background: String?,
    dayPart: DayPart,
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val named = AlarmBackgrounds.find(background)
    val photo = if (AlarmBackgrounds.isPhoto(background)) rememberBackgroundPhoto(background) else null
    when {
        photo != null -> MorningTheme(darkTheme = true, reduceMotion = MorningTheme.reduceMotion) {
            Box(modifier.fillMaxSize()) {
                Image(photo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = PHOTO_SCRIM)))
                content()
            }
        }
        named != null -> MorningTheme(darkTheme = !named.darkText, reduceMotion = MorningTheme.reduceMotion) {
            Box(
                modifier = modifier.fillMaxSize().background(Brush.verticalGradient(listOf(named.top, named.bottom))),
                content = content,
            )
        }
        else -> RingSky(dayPart, modifier, content)
    }
}

private const val PHOTO_SCRIM = 0.35f

@Composable
private fun BoxScope.RingingContent(
    date: LocalDate,
    time: LocalTime,
    hasMissions: Boolean,
    onMainButton: () -> Unit,
    onButtonCenter: (Offset) -> Unit,
    trickRun: TrickRun?,
    trickListener: TrickListener,
    snoozeMinutes: Int?,
    onSnooze: () -> Unit,
) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    val resources = LocalContext.current.resources
    val label = stringResource(if (hasMissions) R.string.ring_start_mission else R.string.ring_dismiss)
    // 버튼 속이기 중에는 원래 자리를 비워 두고 위 층([TrickLayer])에서 그려요.
    val tricking = trickRun != null
    Column(
        modifier = Modifier.align(Alignment.Center).systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = AlarmText.dateLong(resources, date), style = typography.bodySmall, color = colors.textBody)
            Spacer(Modifier.height(MorningDimens.space4))
            FixedFontScale {
                Text(
                    // 12시간제는 '오후 12:16'처럼 오전/오후를 시각과 같은 글자로 붙여요(design-system 12시간제 규칙, `254:8591`).
                    text = AlarmText.time(resources, time),
                    style = typography.displayLarge12h,
                    color = colors.textPrimary,
                )
            }
        }
        Spacer(Modifier.height(MorningDimens.space40))
        if (tricking) {
            Spacer(Modifier.size(MorningDimens.sizeButtonCircle))
        } else {
            MorningButton(
                text = label,
                onClick = {
                    trickRun?.finish(trickListener)
                    onMainButton()
                },
                size = MorningButtonSize.Circle,
                haptic = MorningHaptic.Confirm,
                modifier = Modifier.onGloballyPositioned { onButtonCenter(it.boundsInRoot().center) },
            )
        }
        // 미루기 버튼은 '움직이는 버튼' 중에도 제자리예요(`docs/plan-11-snooze.md` 2절).
        if (snoozeMinutes != null) SnoozeButton(snoozeMinutes, onSnooze)
    }
    if (tricking && trickRun != null) {
        TrickLayer(trickRun, label, trickListener) { center ->
            onButtonCenter(center)
            onMainButton()
        }
    }
}

/** 테스트에서 D0 미루기 버튼을 찾는 이름표. */
const val SNOOZE_BUTTON_TAG = "ringSnooze"

/** '미션 시작' 아래 16dp의 작은 글자 버튼 'n분 뒤 다시'(시안 TSK-281). 누르는 칸은 글자보다 넓게 잡아요. */
@Composable
private fun SnoozeButton(minutes: Int, onClick: () -> Unit) {
    Spacer(Modifier.height(MorningDimens.space4))
    Text(
        text = stringResource(R.string.ring_snooze, minutes),
        style = MorningTheme.typography.bodySmall,
        color = MorningTheme.colors.textBody,
        modifier = Modifier
            .testTag(SNOOZE_BUTTON_TAG)
            .morningClickable(haptic = MorningHaptic.Press, onClick = onClick)
            .padding(horizontal = MorningDimens.space16, vertical = SnoozeTouchPadding),
    )
}

private val SnoozeTouchPadding = 12.dp

/**
 * W1 해제 완료. 하늘 위에 딤을 깔고 가운데 카드(인사, 한마디, '확인').
 * 피그마는 홈 위에 딤이지만, 잠금 화면 위 액티비티라 홈 대신 같은 하늘을 깔아요.
 */
@Composable
fun WakeUpScreen(
    dayPart: DayPart,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    weather: WeatherKind? = null,
) {
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
                text = stringResource(wakeUpMessage(morning, weather)),
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

/** W1 한마디. 날씨를 알면 날씨 한마디, 모르면(위치 동의 없음, 못 불러옴, 꺼 둠) 짧은 인사예요. */
private fun wakeUpMessage(morning: Boolean, weather: WeatherKind?): Int = when (weather) {
    WeatherKind.Rain -> R.string.ring_wake_up_weather_rain
    WeatherKind.Snow -> R.string.ring_wake_up_weather_snow
    WeatherKind.Clear -> R.string.ring_wake_up_weather_clear
    null -> if (morning) R.string.ring_wake_up_message_morning else R.string.ring_wake_up_message_night
}

/**
 * W2 '깨어 있나요?'와 W3 '정말 깼어요?'(결정 145 9-1). 남은 시간을 크게 보여 주고, 원형 '깼어요'를 10초 동안
 * 꾹 누르면([HoldButton]) 끝나요. 일찍 떼면 안내 줄이 '끝까지 누르고 있어야 해요'로 바뀌어요.
 * - [remainingMillis]가 null이면(W3를 기다리는 중에 미리 연 경우) 시계와 그 아래 안내를 숨겨요.
 * - [confirmed]면 버튼 없이 '확인했어요'만 보여요. W2는 한 번 더 확인이 켜져 있으면 [nextAskMinutes]분 뒤 다시
 *   묻는다고 한 줄 더 적어요.
 */
@Composable
fun WakeCheckScreen(
    dayPart: DayPart,
    remainingMillis: Long?,
    confirmed: Boolean,
    onAwake: () -> Unit,
    modifier: Modifier = Modifier,
    recheck: Boolean = false,
    nextAskMinutes: Int? = null,
    ask: AskCount? = null,
    multiDone: MultiDone? = null,
    release: Boolean = false,
    onClose: () -> Unit = {},
) {
    RingSky(dayPart, modifier) {
        Column(
            modifier = Modifier.align(Alignment.Center).systemBarsPadding().padding(horizontal = MorningDimens.space24),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (confirmed) {
                CheckDone(nextAskMinutes, multiDone)
            } else {
                CheckAsking(recheck && !release, remainingMillis, onAwake, ask.takeIf { recheck && !release }, release)
            }
        }
        // W4만 위 왼쪽에 X 닫기가 있어요(TSK-266). 누르면 기다림이 그대로 이어져요.
        if (release && !confirmed) {
            MorningIconButton(
                icon = Lucide.X,
                contentDescription = stringResource(R.string.ring_release_close),
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .systemBarsPadding()
                    .padding(MorningDimens.space8)
                    .testTag(RELEASE_CLOSE_TAG),
            )
        }
    }
}

@Composable
private fun CheckAsking(
    recheck: Boolean,
    remainingMillis: Long?,
    onAwake: () -> Unit,
    ask: AskCount?,
    release: Boolean = false,
) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    val resources = LocalContext.current.resources
    var releasedEarly by remember { mutableStateOf(false) }
    if (ask != null) {
        // 여러 번 묻기 W3: 제목 위 '3번 중 2번째' 또는 '마지막이에요'(시간 안내 글과 같은 색).
        Text(
            text = if (ask.isLast) {
                stringResource(R.string.ring_multi_last)
            } else {
                stringResource(R.string.ring_multi_count, ask.total, ask.number)
            },
            style = typography.bodySmallStrong,
            color = colors.textBody,
        )
        Spacer(Modifier.height(MorningDimens.space8))
    }
    Text(
        text = stringResource(if (recheck) R.string.ring_recheck_title else R.string.ring_wake_check_title),
        style = typography.headlineLarge,
        color = colors.textPrimary,
    )
    Spacer(Modifier.height(MorningDimens.space8))
    Text(
        text = stringResource(if (releasedEarly) R.string.ring_hold_released_early else R.string.ring_hold_guide),
        style = typography.bodyMedium,
        color = if (releasedEarly) colors.primaryText else colors.textBody,
        textAlign = TextAlign.Center,
    )
    if (remainingMillis != null) {
        Spacer(Modifier.height(MorningDimens.space16))
        FixedFontScale {
            Text(
                text = AlarmText.countdown(resources, remainingMillis),
                style = typography.displayLarge12h,
                color = colors.textPrimary,
            )
        }
        Spacer(Modifier.height(MorningDimens.space16))
        Text(
            text = stringResource(R.string.ring_check_ring_again),
            style = typography.bodySmall,
            color = colors.textBody,
            textAlign = TextAlign.Center,
        )
    }
    if (release) {
        // W4: 남은 시간 숫자 없이 해제 뒤 안내 한 줄(TSK-266).
        Spacer(Modifier.height(MorningDimens.space16))
        Text(
            text = stringResource(R.string.ring_release_note),
            style = typography.bodySmall,
            color = colors.textBody,
            textAlign = TextAlign.Center,
        )
    }
    Spacer(Modifier.height(MorningDimens.space40))
    HoldButton(
        label = stringResource(R.string.ring_wake_check_awake),
        onComplete = onAwake,
        onReleasedEarly = { releasedEarly = true },
        onPressed = { releasedEarly = false },
    )
}

@Composable
private fun CheckDone(nextAskMinutes: Int?, multiDone: MultiDone?) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    Text(
        text = stringResource(R.string.ring_wake_check_done),
        style = typography.headlineLarge,
        color = colors.textPrimary,
    )
    val next = when (multiDone) {
        MultiDone.More -> stringResource(R.string.ring_multi_next)
        MultiDone.Last -> stringResource(R.string.ring_multi_all_done)
        null -> nextAskMinutes?.let { stringResource(R.string.ring_wake_check_next, it) }
    }
    if (next != null) {
        Spacer(Modifier.height(MorningDimens.space8))
        Text(
            text = next,
            style = typography.bodyMedium,
            color = colors.textBody,
            textAlign = TextAlign.Center,
        )
    }
}

const val RELEASE_CLOSE_TAG = "ringReleaseClose"

/** 여러 번 묻기에서 지금 묻는 확인이 몇 번째인지([number]는 1부터). */
data class AskCount(val number: Int, val total: Int) {
    val isLast: Boolean get() = number >= total
}

/** 여러 번 묻기에서 '깼어요'를 마친 뒤 글. [More]는 '조금 뒤 한 번 더 물어볼게요', [Last]는 '오늘 확인을 다 마쳤어요'. */
enum class MultiDone { More, Last }
