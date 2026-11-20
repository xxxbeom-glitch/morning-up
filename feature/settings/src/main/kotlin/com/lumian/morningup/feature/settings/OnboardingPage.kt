package com.lumian.morningup.feature.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumian.morningup.core.alarm.PermissionSettings
import com.lumian.morningup.core.designsystem.component.MorningButton
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.AlarmPermission
import kotlinx.coroutines.delay

/** 스플래시를 보여 주는 시간. */
private const val SPLASH_MILLIS = 1_200L

/** 상태 바 아래에서 제목 묶음까지(피그마 384 - 상태 바 52). 위 280 그림 칸(20~300)에는 [OnboardingPicture]가 들어가요. */
private val HeadlineTop = 332.dp

/** 상태 바 아래에서 진행 점까지(피그마 512 - 52). */
private val DotsTop = 460.dp
private val DotSize = 8.dp
private val DotActiveWidth = 24.dp
private val DotRadius = 4.dp

/** 배경 위쪽 푸른 기운. 피그마는 라이트, 다크 모두 `#0B6CF0` 16%에서 60% 높이까지 투명으로 칠해요. */
private val GlowBlue = Color(0xFF0B6CF0)
private const val GLOW_ALPHA = 0.16f
private const val GLOW_END = 0.6f
private const val DARK_LUMINANCE = 0.5f

/** 한 장의 문구. */
private data class StepCopy(
    @StringRes val title: Int,
    @StringRes val sub: Int,
    @StringRes val button: Int,
    @StringRes val skip: Int,
)

private fun copyOf(step: OnboardingStep): StepCopy = when (step) {
    OnboardingStep.Notifications -> StepCopy(
        R.string.onboarding_notifications_title,
        R.string.onboarding_notifications_sub,
        R.string.onboarding_notifications_button,
        R.string.onboarding_later,
    )
    OnboardingStep.FullScreen -> StepCopy(
        R.string.onboarding_full_screen_title,
        R.string.onboarding_full_screen_sub,
        R.string.onboarding_full_screen_button,
        R.string.onboarding_later,
    )
    OnboardingStep.Battery -> StepCopy(
        R.string.onboarding_battery_title,
        R.string.onboarding_battery_sub,
        R.string.onboarding_battery_button,
        R.string.onboarding_later,
    )
    OnboardingStep.Overlay -> StepCopy(
        R.string.onboarding_overlay_title,
        R.string.onboarding_overlay_sub,
        R.string.onboarding_overlay_button,
        R.string.onboarding_later,
    )
    OnboardingStep.Location -> StepCopy(
        R.string.onboarding_location_title,
        R.string.onboarding_location_sub,
        R.string.onboarding_location_button,
        R.string.onboarding_skip,
    )
}

/**
 * 첫 실행 온보딩(결정 129, 131). 스플래시 뒤에 꺼진 권한 장만 차례로 보여 줘요.
 * 버튼을 누르면 그 권한의 시스템 창이나 폰 설정으로 가고, 돌아와서 켜졌으면 다음 장으로 넘어가요.
 * '나중에 하기'는 다음 장으로 넘겨요(건너뛴 필수 권한은 홈 배너가 다시 알려요). 마지막 장을 넘기면 [onFinished].
 */
@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    showAll: Boolean = false,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val report by viewModel.report.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var splashDone by rememberSaveable { mutableStateOf(false) }
    var stepName by rememberSaveable { mutableStateOf<String?>(null) }
    val step = stepName?.let(OnboardingStep::valueOf)
    // [showAll](온보딩 전용 확인 앱)일 때, 이 장에 들어올 때 권한이 이미 켜져 있었는지. 켜져 있던 장도 보여 줘요.
    val grantedOnEnter by rememberSaveable(stepName) {
        mutableStateOf(step?.let { viewModel.report.value.isGranted(it.permission) } ?: false)
    }

    fun goTo(next: OnboardingStep?) {
        if (next == null) onFinished() else stepName = next.name
    }

    LaunchedEffect(Unit) {
        if (!splashDone) {
            delay(SPLASH_MILLIS)
            viewModel.refresh()
            goTo(OnboardingStep.after(null, viewModel.report.value, showAll))
            splashDone = true
        }
    }
    // 폰 설정이나 시스템 창에서 돌아와 권한 상태가 바뀌면, 켜진 장은 넘겨요.
    // [showAll]이면 이 장에 있는 동안 새로 켜졌을 때만 다음 장으로 넘겨요.
    LaunchedEffect(report, step) {
        val current = step ?: return@LaunchedEffect
        val next = OnboardingStep.settle(current, report, showAll, grantedOnEnter)
        if (next != current) goTo(next)
    }

    val notificationAsk = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refresh()
    }
    val locationAsk = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refresh()
    }
    val allow: (OnboardingStep) -> Unit = { target ->
        when (target) {
            OnboardingStep.Notifications ->
                if (needsRuntimeNotificationAsk(context)) {
                    askOrOpen(context, viewModel, Manifest.permission.POST_NOTIFICATIONS, target.permission) {
                        notificationAsk.launch(it)
                    }
                } else {
                    openSettings(context, target.permission)
                }
            OnboardingStep.Battery -> PermissionSettings.open(context, PermissionSettings.batteryRequestIntent(context))
            OnboardingStep.Location ->
                askOrOpen(context, viewModel, Manifest.permission.ACCESS_COARSE_LOCATION, AlarmPermission.Location) {
                    locationAsk.launch(it)
                }
            OnboardingStep.FullScreen, OnboardingStep.Overlay -> openSettings(context, target.permission)
        }
    }

    if (!splashDone || step == null) {
        OnboardingSplash(modifier)
    } else {
        OnboardingStepPage(
            step = step,
            onAllow = { allow(step) },
            onLater = { goTo(OnboardingStep.after(step, report, showAll)) },
            modifier = modifier,
        )
    }
}

/** 스플래시(결정 131, O1 자리). 낮엔 한낮 하늘, 밤(다크)엔 밤하늘 위에 워드마크만 있어요. 진행 점은 없어요. */
@Composable
fun OnboardingSplash(modifier: Modifier = Modifier) {
    val colors = MorningTheme.colors
    val night = colors.backgroundBase.luminance() < DARK_LUMINANCE
    val sky = if (night) {
        listOf(colors.skyNightTop, colors.skyNightBottom)
    } else {
        listOf(colors.skyNoonTop, colors.skyNoonBottom)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(sky)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.onboarding_wordmark),
            style = MorningTheme.typography.displayMedium,
            color = colors.textOnSky,
        )
    }
}

/** 온보딩 권한 한 장(O2-1~O2-4, O3). 위 280 그림 칸에 피그마 그림과 움직임을 그려요(O2-4, O3는 피그마도 비어 있어요). */
@Composable
fun OnboardingStepPage(
    step: OnboardingStep,
    onAllow: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    val copy = copyOf(step)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundBase)
            .background(
                Brush.verticalGradient(
                    0f to GlowBlue.copy(alpha = GLOW_ALPHA),
                    GLOW_END to GlowBlue.copy(alpha = 0f),
                ),
            ),
    ) {
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            OnboardingPicture(
                step = step,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = OnboardingMotionTokens.SLOT_TOP.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = HeadlineTop)
                    .padding(horizontal = MorningDimens.space24),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MorningDimens.space12),
            ) {
                Text(
                    text = stringResource(copy.title),
                    style = typography.headlineLarge,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(copy.sub),
                    style = typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            ProgressDots(
                active = step.dotIndex,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = DotsTop),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = MorningDimens.space16,
                    end = MorningDimens.space16,
                    top = MorningDimens.space16,
                    bottom = MorningDimens.space8,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MorningDimens.space4),
        ) {
            MorningButton(text = stringResource(copy.button), onClick = onAllow)
            Text(
                text = stringResource(copy.skip),
                style = typography.bodyMediumStrong,
                color = colors.textTertiary,
                modifier = Modifier
                    .clip(RoundedCornerShape(MorningDimens.space8))
                    .morningClickable(onClick = onLater)
                    .padding(horizontal = MorningDimens.space16, vertical = MorningDimens.space12),
            )
        }
    }
}

/** 진행 점 5개. 지금 장은 24x8 막대(primary/default), 나머지는 8x8 점(control/track-off). */
@Composable
private fun ProgressDots(active: Int, modifier: Modifier = Modifier) {
    val colors = MorningTheme.colors
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(MorningDimens.space8)) {
        repeat(OnboardingStep.DotCount) { index ->
            val on = index == active
            Box(
                modifier = Modifier
                    .size(width = if (on) DotActiveWidth else DotSize, height = DotSize)
                    .background(
                        if (on) colors.primaryDefault else colors.controlTrackOff,
                        RoundedCornerShape(DotRadius),
                    ),
            )
        }
    }
}
