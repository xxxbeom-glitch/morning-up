package com.lumian.morningup.feature.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
/** 움직임 줄이기를 켠 폰의 스플래시 시간(다 쓴 글자만 보여 줘요). */
private const val SPLASH_REDUCED_MILLIS = 1_200L

/**
 * 그림 칸(280)과 글 묶음을 한 덩어리로 상태 바와 아래 버튼 묶음 사이 세로 가운데에 놓아요(11-20 사용자 지시).
 * 덩어리 높이는 글 묶음이 96인 장 기준 408(그림 280 + 사이 32 + 글 96)이라, 글이 더 긴 장도 그림 자리가 같아요.
 */
private val BlockHeight = 408.dp
private val TextTop = 312.dp

/**
 * 배경 위쪽 푸른 기운. 피그마는 `#0B6CF0`에서 60% 높이까지 투명으로 칠해요.
 * 시작 진하기는 라이트 24%(11-20 사용자 의견 "파란색이 너무 연하다"), 다크 16%예요.
 */
private val GlowBlue = Color(0xFF0B6CF0)
private const val GLOW_ALPHA_LIGHT = 0.24f
private const val GLOW_ALPHA_DARK = 0.16f
private const val GLOW_END = 0.6f

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
 * [onLightRoundFinished]가 있으면(온보딩 전용 확인 앱) 첫 바퀴를 마칠 때 그것을 부르고, 스플래시 없이
 * 첫 장부터 한 바퀴 더 보여 줘요. 부르는 쪽이 그때 테마를 다크로 바꿔요(11-20 사용자 요청: 라이트 5장 뒤 다크 5장).
 */
@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    showAll: Boolean = false,
    onLightRoundFinished: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val report by viewModel.report.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var splashDone by rememberSaveable { mutableStateOf(false) }
    var stepName by rememberSaveable { mutableStateOf<String?>(null) }
    var lightRoundDone by rememberSaveable { mutableStateOf(false) }
    val step = stepName?.let(OnboardingStep::valueOf)
    // [showAll](온보딩 전용 확인 앱)일 때, 이 장에 들어올 때 권한이 이미 켜져 있었는지. 켜져 있던 장도 보여 줘요.
    val grantedOnEnter by rememberSaveable(stepName) {
        mutableStateOf(step?.let { viewModel.report.value.isGranted(it.permission) } ?: false)
    }

    fun goTo(next: OnboardingStep?) {
        when {
            next != null -> stepName = next.name
            onLightRoundFinished != null && !lightRoundDone -> {
                lightRoundDone = true
                onLightRoundFinished()
                stepName = OnboardingStep.after(null, viewModel.report.value, showAll)?.name
                if (stepName == null) onFinished()
            }
            else -> onFinished()
        }
    }

    // 손글씨를 다 쓰고 0.5초 멈춘 뒤 넘어가요(결정 170). 움직임 줄이기면 전처럼 짧게요.
    val splashMillis = if (MorningTheme.reduceMotion) {
        SPLASH_REDUCED_MILLIS
    } else {
        SplashHandwriting.TOTAL_MILLIS.toLong()
    }
    LaunchedEffect(Unit) {
        if (!splashDone) {
            delay(splashMillis)
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

/**
 * 스플래시(결정 131, O1 자리). 아이콘과 같은 어두운 바탕에 아이콘 손글씨 '일어나'를 한 획씩 졸린 속도로 써요(결정 170).
 * 라이트, 다크 모두 아이콘 색 그대로예요.
 */
@Composable
fun OnboardingSplash(modifier: Modifier = Modifier) {
    val name = stringResource(R.string.onboarding_splash_desc)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashBackground),
        contentAlignment = Alignment.Center,
    ) {
        SplashHandwritingLogo(
            Modifier
                .size(SplashLogoSize)
                .semantics { contentDescription = name },
        )
    }
}

/** 손글씨 칸. 아이콘 viewport 540을 이 크기에 맞춰서 글자 폭은 약 170dp예요. */
private val SplashLogoSize = 320.dp

/**
 * 온보딩 권한 한 장(O2-1~O2-4, O3). 그림 칸(328x280)에 피그마 그림과 움직임을 그리고, 그 아래 제목과 설명을 둬요.
 * 진행 점은 없어요(11-20 사용자 지시).
 */
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
    val glowAlpha = if (colors.isDark) GLOW_ALPHA_DARK else GLOW_ALPHA_LIGHT
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundBase)
            .background(
                Brush.verticalGradient(
                    0f to GlowBlue.copy(alpha = glowAlpha),
                    GLOW_END to GlowBlue.copy(alpha = 0f),
                ),
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val top = ((maxHeight - BlockHeight) / 2).coerceAtLeast(0.dp)
            OnboardingPicture(
                step = step,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = top),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = top + TextTop)
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
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
