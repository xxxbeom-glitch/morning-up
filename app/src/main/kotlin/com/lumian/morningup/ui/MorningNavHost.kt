package com.lumian.morningup.ui

import androidx.compose.ui.platform.LocalContext
import com.lumian.morningup.core.designsystem.theme.ThemeModeStore
import com.lumian.morningup.core.designsystem.theme.rememberThemeMode
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.lumian.morningup.core.model.PermissionStep
import com.lumian.morningup.core.model.banner
import com.lumian.morningup.core.model.bannerMore
import com.lumian.morningup.feature.edit.AlarmEditRoute
import com.lumian.morningup.feature.edit.EditRoute
import com.lumian.morningup.feature.edit.MissionTryRoute
import com.lumian.morningup.feature.home.HomeRoute
import com.lumian.morningup.feature.mission.MissionPreviewRouteScreen
import com.lumian.morningup.feature.mission.play.MissionPreviewRoute
import com.lumian.morningup.feature.settings.FaqPage
import com.lumian.morningup.feature.settings.LicensesPage
import com.lumian.morningup.feature.ring.TrickPreviewRoute
import com.lumian.morningup.feature.settings.LabRoute
import com.lumian.morningup.feature.settings.OnboardingRoute
import com.lumian.morningup.feature.settings.PermissionBanner
import com.lumian.morningup.feature.settings.PermissionRoute
import com.lumian.morningup.feature.settings.PermissionStepSheet
import com.lumian.morningup.feature.settings.openBannerTarget
import com.lumian.morningup.feature.settings.SettingsRoute
import com.lumian.morningup.feature.settings.SettingsViewModel
import com.lumian.morningup.feature.sleep.SleepPlayerPage
import com.lumian.morningup.feature.sleep.SleepPlayerRoute
import com.lumian.morningup.feature.sleep.SleepScreen
import kotlinx.coroutines.flow.MutableStateFlow
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

/** 미션 확인용 앱 첫 화면(11-20 사용자 지시). */
@Serializable
data object MissionTryDestination

/** 설정 '권한' 한 화면(결정 130 보충). */
@Serializable
data object PermissionsRoute

/** T3 '확실히 잠깨기' 실험실(결정 146). */
@Serializable
data object LabDestination

/** T3-3 버튼 속이기 미리 해보기(소리 없음). */
@Serializable
data object TrickPreviewDestination

/** 편집 화면이 저장한 알람 id를 첫 화면에 넘기는 열쇠. */
private const val SAVED_ALARM_KEY = "savedAlarmId"

/** 저장하면서 그 알람의 남은 확인(W2, W3)을 멈췄는지(결정 145). 홈 토스트가 달라져요. */
private const val CHECKS_STOPPED_KEY = "checksStopped"
private const val REST_RELEASED_KEY = "restReleased"

/**
 * 앱 화면 길. 첫 화면(탭) 위로 알람 편집이, 그 위로 미션 미리보기가 옆에서 밀려 들어와요(spatialSlow + 페이드).
 * [editRequests]에 알람 id가 오면(상태 표시줄 알람 아이콘) 그 알람의 편집 화면을 열고 [onEditRequestHandled]를 불러요.
 * [showOnboarding]이면 온보딩부터 시작하고, 끝나면 [onOnboardingFinished]를 부른 뒤 홈으로 가요(뒤로 가도 안 돌아와요).
 * [onboardingOnly](온보딩 전용 확인 앱)면 켜진 권한 장도 다 보여 주고, 끝나면 홈 대신 스플래시부터 다시 보여요.
 * 그때 [onOnboardingLightRoundFinished]가 있으면 라이트 한 바퀴 뒤에 불러 다크 한 바퀴를 이어 보여요.
 * [missionOnly](미션 확인용 앱)면 미션 목록부터 시작하고, 미션을 끝내거나 닫으면 목록으로 돌아와요.
 */
@Composable
fun MorningNavHost(
    editRequests: StateFlow<Long?>,
    onEditRequestHandled: () -> Unit,
    modifier: Modifier = Modifier,
    permissionRequests: StateFlow<PermissionStep?> = MutableStateFlow(null),
    onPermissionRequestHandled: () -> Unit = {},
    showOnboarding: Boolean = false,
    onOnboardingFinished: () -> Unit = {},
    onboardingOnly: Boolean = false,
    onOnboardingLightRoundFinished: (() -> Unit)? = null,
    missionOnly: Boolean = false,
    navController: NavHostController = rememberNavController(),
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
    // 권한 시트는 홈 위에 떠요. 다른 화면이 위에 있으면 홈까지 닫고, 시트는 홈 쪽에서 열어요.
    val pendingPermission by permissionRequests.collectAsStateWithLifecycle()
    LaunchedEffect(pendingPermission) {
        if (pendingPermission != null) navController.popBackStack(MainRoute, inclusive = false)
    }
    val reduceMotion = LocalReduceMotion.current
    NavHost(
        navController = navController,
        startDestination = when {
            missionOnly -> MissionTryDestination
            showOnboarding -> OnboardingDestination
            else -> MainRoute
        },
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
            // H2 배너를 누르면 나중용 권한 시트 PL2가 홈 위에 떠요(결정 132).
            var permissionSheet by rememberSaveable { mutableStateOf<PermissionStep?>(null) }
            // 배터리 전날 밤 알림을 누르면 PL2 2단계로 열어요(필수가 꺼져 있으면 시트가 1단계부터 보여요).
            var showAlarmTab by remember { mutableStateOf(false) }
            val permissionRequest by permissionRequests.collectAsStateWithLifecycle()
            LaunchedEffect(permissionRequest) {
                val step = permissionRequest ?: return@LaunchedEffect
                permissionSheet = step
                showAlarmTab = true
                onPermissionRequestHandled()
            }
            // 첫 알람을 저장해도 권한 창은 띄우지 않아요(결정 128). 꺼진 필수 권한은 H2 배너로 알려요.
            MorningUpApp(
                alarmTab = { tabModifier ->
                    HomeRoute(
                        onOpenEdit = { id ->
                            navController.navigate(EditRoute(id ?: EditRoute.NEW_ALARM)) { launchSingleTop = true }
                        },
                        modifier = tabModifier,
                        savedAlarmId = savedId,
                        onSavedAlarmShown = {
                            entry.savedStateHandle[SAVED_ALARM_KEY] = null
                            entry.savedStateHandle[CHECKS_STOPPED_KEY] = false
                            entry.savedStateHandle[REST_RELEASED_KEY] = false
                        },
                        savedChecksStopped = entry.savedStateHandle[CHECKS_STOPPED_KEY] ?: false,
                        savedRestReleased = entry.savedStateHandle[REST_RELEASED_KEY] ?: false,
                        onSelectingChange = { homeSelecting = it },
                        banner = report.banner?.let { kind ->
                            {
                                val context = LocalContext.current
                                PermissionBanner(
                                    kind,
                                    onClick = {
                                        val step = kind.startStep
                                        if (step != null) permissionSheet = step else openBannerTarget(context, kind)
                                    },
                                    more = report.bannerMore,
                                )
                            }
                        },
                    )
                },
                settingsTab = { tabModifier ->
                    val context = LocalContext.current
                    SettingsRoute(
                        versionName = BuildConfig.VERSION_NAME,
                        onOpenPermissions = {
                            navController.navigate(PermissionsRoute) { launchSingleTop = true }
                        },
                        modifier = tabModifier,
                        onOpenFaq = { navController.navigate(FaqRoute) { launchSingleTop = true } },
                        onOpenLicenses = { navController.navigate(LicensesRoute) { launchSingleTop = true } },
                        onOpenLab = { navController.navigate(LabDestination) { launchSingleTop = true } },
                        themeMode = rememberThemeMode(enabled = BuildConfig.DEBUG).takeIf { BuildConfig.DEBUG },
                        onThemeMode = { mode -> ThemeModeStore.set(context, mode) },
                    )
                },
                sleepTab = { tabModifier ->
                    SleepScreen(
                        onOpenTrack = { id -> navController.navigate(SleepPlayerRoute(id)) { launchSingleTop = true } },
                        modifier = tabModifier,
                    )
                },
                settingsAlert = report.needsAttention,
                sleepLounge = BuildConfig.SLEEP_LOUNGE,
                hideNav = homeSelecting,
                alarmTabRequested = showAlarmTab,
                onAlarmTabShown = { showAlarmTab = false },
            )
            permissionSheet?.let { step ->
                PermissionStepSheet(requested = step, onClose = { permissionSheet = null }, viewModel = settings)
            }
        }
        composable<OnboardingDestination>(
            enterTransition = { EnterTransition.None },
            exitTransition = { fadeOut(MorningMotion.effects(MorningMotion.EffectsDefault)) },
        ) {
            OnboardingRoute(
                onFinished = {
                    onOnboardingFinished()
                    // 온보딩 전용 확인 앱은 홈으로 가지 않고 스플래시부터 다시 시작해요.
                    navController.navigate(if (onboardingOnly) OnboardingDestination else MainRoute) {
                        popUpTo(OnboardingDestination) { inclusive = true }
                        launchSingleTop = !onboardingOnly
                    }
                },
                showAll = onboardingOnly,
                onLightRoundFinished = onOnboardingLightRoundFinished,
            )
        }
        composable<MissionTryDestination> {
            MissionTryRoute(
                onStart = { setting ->
                    navController.navigate(MissionPreviewRoute.of(listOf(setting))) { launchSingleTop = true }
                },
            )
        }
        composable<PermissionsRoute> {
            PermissionRoute(onBack = { navController.popBackStack() })
        }
        composable<LabDestination> {
            LabRoute(
                onBack = { navController.popBackStack() },
                onPreview = { navController.navigate(TrickPreviewDestination) { launchSingleTop = true } },
            )
        }
        composable<TrickPreviewDestination> {
            TrickPreviewRoute(onClose = { navController.popBackStack<TrickPreviewDestination>(inclusive = true) })
        }
        composable<SleepPlayerRoute> { entry ->
            SleepPlayerPage(
                trackId = entry.toRoute<SleepPlayerRoute>().trackId,
                onBack = { navController.popBackStack() },
            )
        }
        composable<FaqRoute> { FaqPage(onBack = { navController.popBackStack() }) }
        composable<LicensesRoute> { LicensesPage(onBack = { navController.popBackStack() }) }
        composable<EditRoute> {
            AlarmEditRoute(
                onSaved = { id, checksStopped, restReleased ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(CHECKS_STOPPED_KEY, checksStopped)
                    navController.previousBackStackEntry?.savedStateHandle?.set(REST_RELEASED_KEY, restReleased)
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
