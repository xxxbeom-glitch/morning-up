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
import com.lumian.morningup.core.model.PermissionScreen
import com.lumian.morningup.feature.edit.AlarmEditRoute
import com.lumian.morningup.feature.edit.EditRoute
import com.lumian.morningup.feature.home.HomeRoute
import com.lumian.morningup.feature.mission.MissionPreviewRouteScreen
import com.lumian.morningup.feature.mission.play.MissionPreviewRoute
import com.lumian.morningup.feature.settings.FaqPage
import com.lumian.morningup.feature.settings.FirstSavePermissionAsk
import com.lumian.morningup.feature.settings.LicensesPage
import com.lumian.morningup.feature.settings.PermissionBanner
import com.lumian.morningup.feature.settings.PermissionRoute
import com.lumian.morningup.feature.settings.PowerGuardConsentDialog
import com.lumian.morningup.feature.settings.PowerGuardGuidePage
import com.lumian.morningup.feature.settings.SettingsRoute
import com.lumian.morningup.feature.settings.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/** 탭 세 개를 담은 첫 화면. */
@Serializable
data object MainRoute

/** 자주 묻는 질문. */
@Serializable
data object FaqRoute

/** T5 오픈소스 라이선스. */
@Serializable
data object LicensesRoute

/** P7 접근성 켜기 안내(전원 종료 방지). */
@Serializable
data object PowerGuideRoute

/** T2-A 알람 울림, T2-B 알람 화면 표시. [screen]은 [PermissionScreen] 이름이에요. */
@Serializable
data class PermissionsRoute(val screen: String)

/** 편집 화면이 저장한 알람 id를 첫 화면에 넘기는 열쇠. */
private const val SAVED_ALARM_KEY = "savedAlarmId"
private const val FIRST_SAVE_ASK_DELAY_MILLIS = 700L

/**
 * 앱 화면 길. 첫 화면(탭) 위로 알람 편집이, 그 위로 미션 미리보기가 옆에서 밀려 들어와요(spatialSlow + 페이드).
 * [editRequests]에 알람 id가 오면(상태 표시줄 알람 아이콘) 그 알람의 편집 화면을 열고 [onEditRequestHandled]를 불러요.
 */
@Composable
fun MorningNavHost(
    editRequests: StateFlow<Long?>,
    onEditRequestHandled: () -> Unit,
    modifier: Modifier = Modifier,
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
        startDestination = MainRoute,
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
            val firstSaveAsked by settings.firstSaveAsked.collectAsStateWithLifecycle()
            var askingFirstSave by rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(savedId) {
                // 첫 알람을 저장하고 홈에 돌아오면 저장된 카드를 잠깐 보여 준 뒤 권한을 한 번만 물어요(F21).
                if (savedId != null && !firstSaveAsked) {
                    delay(FIRST_SAVE_ASK_DELAY_MILLIS)
                    askingFirstSave = true
                }
            }
            MorningUpApp(
                alarmTab = { tabModifier ->
                    HomeRoute(
                        onOpenEdit = { id ->
                            navController.navigate(EditRoute(id ?: EditRoute.NEW_ALARM)) { launchSingleTop = true }
                        },
                        modifier = tabModifier,
                        savedAlarmId = savedId,
                        onSavedAlarmShown = { entry.savedStateHandle[SAVED_ALARM_KEY] = null },
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
                        onOpenPermissions = { screen ->
                            navController.navigate(PermissionsRoute(screen.name)) { launchSingleTop = true }
                        },
                        modifier = tabModifier,
                        onOpenFaq = { navController.navigate(FaqRoute) { launchSingleTop = true } },
                        onOpenLicenses = { navController.navigate(LicensesRoute) { launchSingleTop = true } },
                        extra = debugTools,
                    )
                },
                settingsAlert = report.needsAttention,
            )
            if (askingFirstSave) {
                FirstSavePermissionAsk(report) {
                    settings.markFirstSaveAsked()
                    askingFirstSave = false
                }
            }
        }
        composable<PermissionsRoute> { entry ->
            PermissionRoute(
                screen = PermissionScreen.valueOf(entry.toRoute<PermissionsRoute>().screen),
                onBack = { navController.popBackStack() },
                onOpenPowerGuide = { navController.navigate(PowerGuideRoute) { launchSingleTop = true } },
            )
        }
        composable<FaqRoute> { FaqPage(onBack = { navController.popBackStack() }) }
        composable<LicensesRoute> { LicensesPage(onBack = { navController.popBackStack() }) }
        composable<PowerGuideRoute> {
            PowerGuardGuidePage(onBack = { navController.popBackStack() })
        }
        composable<EditRoute> {
            val settings: SettingsViewModel = hiltViewModel()
            val report by settings.report.collectAsStateWithLifecycle()
            val powerAgreed by settings.powerGuardAgreed.collectAsStateWithLifecycle()
            var askPowerConsent by rememberSaveable { mutableStateOf(false) }
            val openGuide = { navController.navigate(PowerGuideRoute) { launchSingleTop = true } }
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
                onPowerGuardOn = {
                    // 접근성 서비스가 꺼져 있을 때만: 처음이면 P6 동의, 동의했으면 P7 안내로 바로 가요.
                    if (!report.isGranted(AlarmPermission.PowerGuard)) {
                        if (powerAgreed) openGuide() else askPowerConsent = true
                    }
                },
            )
            if (askPowerConsent) {
                PowerGuardConsentDialog(
                    onDismiss = { askPowerConsent = false },
                    onAgree = {
                        settings.agreePowerGuard()
                        askPowerConsent = false
                        openGuide()
                    },
                )
            }
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
