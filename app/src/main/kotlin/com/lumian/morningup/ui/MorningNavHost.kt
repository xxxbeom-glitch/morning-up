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
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.theme.LocalReduceMotion
import com.lumian.morningup.feature.edit.AlarmEditRoute
import com.lumian.morningup.feature.edit.EditRoute
import com.lumian.morningup.feature.home.HomeRoute
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/** 탭 세 개를 담은 첫 화면. */
@Serializable
data object MainRoute

/** 편집 화면이 저장한 알람 id를 첫 화면에 넘기는 열쇠. */
private const val SAVED_ALARM_KEY = "savedAlarmId"

/**
 * 앱 화면 길. 첫 화면(탭) 위로 알람 편집이 옆에서 밀려 들어와요(spatialSlow + 페이드).
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
            MorningUpApp(
                alarmTab = { tabModifier ->
                    HomeRoute(
                        onOpenEdit = { id ->
                            navController.navigate(EditRoute(id ?: EditRoute.NEW_ALARM)) { launchSingleTop = true }
                        },
                        modifier = tabModifier,
                        savedAlarmId = savedId,
                        onSavedAlarmShown = { entry.savedStateHandle[SAVED_ALARM_KEY] = null },
                    )
                },
                debugTools = debugTools,
            )
        }
        composable<EditRoute> {
            AlarmEditRoute(
                onSaved = { id ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(SAVED_ALARM_KEY, id)
                    navController.popBackStack()
                },
                onClose = { navController.popBackStack() },
            )
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
