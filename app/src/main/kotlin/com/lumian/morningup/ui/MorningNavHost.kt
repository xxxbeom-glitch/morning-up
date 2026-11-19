package com.lumian.morningup.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.lumian.morningup.BuildConfig
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.theme.LocalReduceMotion
import com.lumian.morningup.core.model.AlarmPermission
import com.lumian.morningup.feature.edit.AlarmEditRoute
import com.lumian.morningup.feature.edit.EditRoute
import com.lumian.morningup.feature.home.HomeRoute
import com.lumian.morningup.feature.mission.MissionPreviewRouteScreen
import com.lumian.morningup.feature.mission.play.MissionPreviewRoute
import com.lumian.morningup.feature.settings.FaqPage
import com.lumian.morningup.feature.settings.LicensesPage
import com.lumian.morningup.feature.settings.OnboardingRoute
import com.lumian.morningup.feature.settings.PermissionBanner
import com.lumian.morningup.feature.settings.PermissionRoute
import com.lumian.morningup.feature.settings.PowerGuardConsentDialog
import com.lumian.morningup.feature.settings.PowerGuardGuidePage
import com.lumian.morningup.feature.settings.SettingsRoute
import com.lumian.morningup.feature.settings.SettingsViewModel
import com.lumian.morningup.feature.sleep.SleepPlayerPage
import com.lumian.morningup.feature.sleep.SleepPlayerRoute
import com.lumian.morningup.feature.sleep.SleepScreen
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/** 탭 세 개를 담은 첫 화면. */
@Serializable
data object MainRoute

/** 첫 실행 온보딩(스플래시와 권한 장, 결정 129, 131). */
@Serializable
data object OnboardingDestination

/** 자주 묻는 질문. */
@Serializable
data object FaqRoute

/** T5 오픈소스 라이선스. */
@Serializable
data object LicensesRoute

/** P7 접근성 켜기 안내(전원 종료 방지). */
@Serializable
data object PowerGuideRoute

/** 설정 '권한' 한 화면(결정 130 보충). */
@Serializable
data object PermissionsRoute

/** 편집 화면이 저장한 알람 id를 첫 화면에 넘기는 열쇠. */
private const val SAVED_ALARM_KEY = "savedAlarmId"

/**
 * 앱 화면 길. 첫 화면(탭) 위로 알람 편집이, 그 위로 미션 미리보기가 옆에서 밀려 들어와요(spatialSlow + 페이드).
 * [editRequests]에 알람 id가 오면(상태 표시줄 알람 아이콘) 그 알람의 편집 화면을 열고 [onEditRequestHandled]를 불러요.
 * [showOnboarding]이면 온보딩부터 시작하고, 끝나면 [onOnboardingFinished]를 부른 뒤 홈으로 가요(뒤로 가도 안 돌아와요).
 */
@Composable
fun MorningNavHost(
    editRequests: StateFlow<Long?>,
    onEditRequestHandled: () -> Unit,
    modifier: Modifier = Modifier,
    showOnboarding: Boolean = false,
    onOnboardingFinished: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
    debugTools: @Composable () -> Unit = {},
) {
    val request by editRequests.collectAsStateWithLifecycle()
    LaunchedEffect(request) {
        val id = request ?: return@LaunchedEffect
        navController.navigate(EditRoute(id)) {
            popUpTo(MainRoute)
            launchSingleTop = true
        }
        onEditRequestHandled()
    }
    val reduceMotion = LocalReduceMotion.current
    NavHost(
        navController = navController,
        startDestination = if (showOnboarding) OnboardingDestination else MainRoute,
        modifier = modifier,
        enterTransition = { pageEnter(reduceMotion) },
        exitTransition = { pageExit(reduceMotion) },
        popEnterTransition = { pagePopEnter(reduceMotion) },
        popExitTransition = { pagePopExit(reduceMotion) },
    ) {
        composable<MainRoute> { entry ->
            val savedId by entry.savedStateHandle.getStateFlow<Long?>(SAVED_ALARM_KEY, null)
                .collectAsStateWithLifecycle()
            val settings: SettingsViewModel = hiltViewModel()
            val report by settings.report.collectAsStateWithLifecycle()
            var homeSelecting by remember { mutableStateOf(false) }
            // 첫 알람을 저장해도 권한 창은 띄우지 않아요(결정 128). 꺼진 필수 권한은 H2 배너로 알려요.
            MorningUpApp(
                alarmTab = { tabModifier ->
                    HomeRoute(
                        onOpenEdit = { id ->
                            navController.navigate(EditRoute(id ?: EditRoute.NEW_ALARM)) { launchSingleTop = true }
                        },
                        modifier = tabModifier,
                        savedAlarmId = savedId,
                        onSavedAlarmShown = { entry.savedStateHandle[SAVED_ALARM_KEY] = null },
                        onSelectingChange = { homeSelecting = it },
                        banner = if (report.needsAttention) {
                            { PermissionBanner(report) }
                        } else {
                            null
                        },
                    )
                },
                settingsTab = { tabModifier ->
                    SettingsRoute(
                        versionName = BuildConfig.VERSION_NAME,
                        onOpenPermissions = {
                            navController.navigate(PermissionsRoute) { launchSingleTop = true }
                        },
                        modifier = tabModifier,
                        onOpenFaq = { navController.navigate(FaqRoute) { launchSingleTop = true } },
                        onOpenLicenses = { navController.navigate(LicensesRoute) { launchSingleTop = true } },
                        onOpenPowerGuide = { navController.navigate(PowerGuideRoute) { launchSingleTop = true } },
                        extra = debugTools,
                    )
                },
                sleepTab = { tabModifier ->
                    SleepScreen(
                        onOpenTrack = { id -> navController.navigate(SleepPlayerRoute(id)) { launchSingleTop = true } },
                        modifier = tabModifier,
                    )
                },
                settingsAlert = report.needsAttention,
                hideNav = homeSelecting,
            )
        }
        composable<OnboardingDestination>(
            enterTransition = { EnterTransition.None },
            exitTransition = { fadeOut(MorningMotion.effects(MorningMotion.EffectsDefault)) },
        ) {
            OnboardingRoute(
                onFinished = {
                    onOnboardingFinished()
                    navController.navigate(MainRoute) {
                        popUpTo(OnboardingDestination) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<PermissionsRoute> {
            PermissionRoute(onBack = { navController.popBackStack() })
        }
        composable<SleepPlayerRoute> { entry ->
            SleepPlayerPage(
                trackId = entry.toRoute<SleepPlayerRoute>().trackId,
                onBack = { navController.popBackStack() },
            )
        }
        composable<FaqRoute> { FaqPage(onBack = { navController.popBackStack() }) }
        composable<LicensesRoute> { LicensesPage(onBack = { navController.popBackStack() }) }
        composable<PowerGuideRoute> {
            PowerGuardGuidePage(onBack = { navController.popBackStack() })
        }
        composable<EditRoute> {
            AlarmEditRoute(
                onSaved = { id ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(SAVED_ALARM_KEY, id)
                    navController.popBackStack()
                },
                onClose = { navController.popBackStack() },
                onPreview = { missions ->
                    if (missions.isNotEmpty()) {
                        navController.navigate(MissionPreviewRoute.of(missions)) { launchSingleTop = true }
                    }
                },
            )
        }
        // 미리보기: 편집 화면 위에 미션 화면을 띄우고, 닫히면 편집 화면(시트)으로 돌아가요.
        composable<MissionPreviewRoute> {
            MissionPreviewRouteScreen(onClosed = { navController.popBackStack<MissionPreviewRoute>(inclusive = true) })
        }
    }
}

/** 새 화면은 오른쪽에서 들어오고, 아래 화면은 조금만 왼쪽으로 비켜요. '애니메이션 줄이기'면 페이드만 해요. */
private fun pageEnter(reduceMotion: Boolean): EnterTransition =
    if (reduceMotion) {
        fadeIn(MorningMotion.effects(MorningMotion.EffectsDefault))
    } else {
        slideInHorizontally(MorningMotion.spatial(MorningMotion.SpatialSlow, false)) { it } +
            fadeIn(MorningMotion.effects(MorningMotion.EffectsDefault))
    }

private fun pageExit(reduceMotion: Boolean): ExitTransition =
    if (reduceMotion) {
        fadeOut(MorningMotion.effects(MorningMotion.EffectsDefault))
    } else {
        slideOutHorizontally(MorningMotion.spatial(MorningMotion.SpatialSlow, false)) { -it / PARALLAX } +
            fadeOut(MorningMotion.effects(MorningMotion.EffectsDefault))
    }

private fun pagePopEnter(reduceMotion: Boolean): EnterTransition =
    if (reduceMotion) {
        fadeIn(MorningMotion.effects(MorningMotion.EffectsDefault))
    } else {
        slideInHorizontally(MorningMotion.spatial(MorningMotion.SpatialSlow, false)) { -it / PARALLAX } +
            fadeIn(MorningMotion.effects(MorningMotion.EffectsDefault))
    }

private fun pagePopExit(reduceMotion: Boolean): ExitTransition =
    if (reduceMotion) {
        fadeOut(MorningMotion.effects(MorningMotion.EffectsDefault))
    } else {
        slideOutHorizontally(MorningMotion.spatial(MorningMotion.SpatialSlow, false)) { it } +
            fadeOut(MorningMotion.effects(MorningMotion.EffectsDefault))
    }

/** 아래 화면이 비켜나는 비율(폭의 1/4). */
private const val PARALLAX = 4
