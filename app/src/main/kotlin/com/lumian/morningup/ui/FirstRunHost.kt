package com.lumian.morningup.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.lumian.morningup.feature.edit.FirstRunAlarmRoute
import com.lumian.morningup.feature.edit.FirstRunExit
import com.lumian.morningup.feature.edit.FirstRunViewModel
import com.lumian.morningup.feature.settings.FirstRunNotificationCard
import com.lumian.morningup.feature.settings.FirstRunPermissionRoute
import com.lumian.morningup.feature.settings.OnboardingSplash
import com.lumian.morningup.feature.settings.firstRunNeedsNotificationCard
import com.lumian.morningup.feature.settings.splashMillis
import kotlinx.coroutines.delay

/** 첫 실행 단계(결정 191). */
private enum class FirstRunStage { Splash, Alarm, Permissions }

/**
 * 첫 실행 흐름(결정 191, `docs/plan-11-first-run.md`): O1 스플래시 → O4-1~O4-4 첫 알람(알림 권한 카드 포함)
 * → O5 필수 권한 목록(꺼진 줄이 없으면 건너뜀) → [onFinished]. 저장한 알람이 있으면 그 id를 넘겨 홈이 토스트를 띄워요.
 * 첫 알람은 편집 모듈, 권한은 설정 모듈이라 둘을 잇는 이 틀만 앱에 둬요.
 * [demo](첫 실행 확인용 앱)면 알람을 저장하지 않고 O5에 네 줄을 다 보여요. [onLightRoundFinished]가 있으면
 * 첫 바퀴를 마칠 때 불러(부르는 쪽이 다크로 바꿔요) O4-1부터 한 바퀴 더 보여요.
 */
@Composable
internal fun FirstRunHost(
    onFinished: (savedAlarmId: Long?) -> Unit,
    demo: Boolean = false,
    onLightRoundFinished: (() -> Unit)? = null,
    alarmViewModel: FirstRunViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var stage by rememberSaveable { mutableStateOf(FirstRunStage.Splash) }
    var savedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var lightRoundDone by rememberSaveable { mutableStateOf(false) }
    val splash = splashMillis()
    LaunchedEffect(stage) {
        if (stage == FirstRunStage.Splash) {
            delay(splash)
            stage = FirstRunStage.Alarm
        }
    }
    when (stage) {
        FirstRunStage.Splash -> OnboardingSplash()
        FirstRunStage.Alarm -> FirstRunAlarmRoute(
            onFinished = { exit ->
                savedId = (exit as? FirstRunExit.Saved)?.alarmId
                stage = FirstRunStage.Permissions
            },
            needsNotificationCard = { firstRunNeedsNotificationCard(context) },
            notificationCard = { done -> FirstRunNotificationCard(onDone = done) },
            demo = demo,
            viewModel = alarmViewModel,
        )
        FirstRunStage.Permissions -> FirstRunPermissionRoute(
            onFinish = {
                if (onLightRoundFinished != null && !lightRoundDone) {
                    lightRoundDone = true
                    onLightRoundFinished()
                    alarmViewModel.restart()
                    stage = FirstRunStage.Alarm
                } else {
                    onFinished(savedId)
                }
            },
            showAll = demo,
        )
    }
}
