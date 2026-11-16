package com.lumian.morningup.ring

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.lumian.morningup.R
import com.lumian.morningup.core.alarm.AlarmIntents
import com.lumian.morningup.core.alarm.ring.RingController
import com.lumian.morningup.core.alarm.ring.RingScreen
import com.lumian.morningup.core.alarm.ring.RingService
import com.lumian.morningup.core.data.session.RingSessionStore
import com.lumian.morningup.core.designsystem.component.MorningButton
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.engine.RingEvent
import com.lumian.morningup.core.model.engine.RingPhase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * 임시 울림 화면(2단계). 엔진 흐름을 폰에서 확인할 수 있게 상태만 그려요.
 * 하늘 배경, 날짜, 미션 화면은 3~5단계에서 피그마 D0, W1, W2, M1~M4대로 바꿔요.
 */
@AndroidEntryPoint
class RingActivity : ComponentActivity() {
    @Inject lateinit var controller: RingController

    @Inject lateinit var sessions: RingSessionStore

    /** 저장된 울림을 다 읽기 전에는 닫지 않아요(앱이 새로 뜬 경우 처음엔 상태가 비어 있어요). */
    private val loaded = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        showOverLockScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // 울리는 동안 뒤로 가기로 빠져나가지 못해요. W1만 닫을 수 있어요.
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (controller.screen.value == RingScreen.WakeUp) closeWakeUp()
                }
            },
        )
        resumeIfNeeded()
        setContent {
            MorningTheme {
                val screen by controller.screen.collectAsStateWithLifecycle()
                val session by controller.session.collectAsStateWithLifecycle()
                val ready by loaded.collectAsStateWithLifecycle()
                LaunchedEffect(screen, session, ready) {
                    if (session != null) loaded.value = true
                    if (ready && screen == RingScreen.None && session == null) finish()
                }
                // 이어가기로 살아난 쉬는 중, W2 기다림에는 Show 효과가 없어서 단계에 맞는 화면을 골라요.
                val shown = when {
                    screen != RingScreen.None -> screen
                    session?.phase == RingPhase.AwaitingWakeCheck -> RingScreen.WakeUp
                    session != null -> RingScreen.Ringing
                    else -> RingScreen.None
                }
                RingContent(
                    screen = shown,
                    hasMissions = session?.plan?.missions?.isNotEmpty() == true,
                    send = { RingService.send(this, it) },
                    closeWakeUp = ::closeWakeUp,
                )
            }
        }
    }

    /** 앱이 죽었다 살아나 화면부터 열렸으면, 저장된 울림을 서비스가 이어가게 해요. */
    private fun resumeIfNeeded() {
        if (controller.session.value != null || controller.screen.value != RingScreen.None) {
            loaded.value = true
            return
        }
        lifecycleScope.launch {
            if (sessions.load() != null) {
                startForegroundService(AlarmIntents.serviceIntent(this@RingActivity, AlarmIntents.ACTION_RESUME))
                // 오래된 울림이라 이어가지 않고 끝난 경우를 위해 잠시 뒤에는 닫을 수 있게 해요.
                delay(RESUME_WAIT_MILLIS)
            }
            loaded.value = true
        }
    }

    private fun closeWakeUp() {
        controller.clearScreen()
        finish()
    }

    @Suppress("DEPRECATION")
    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

private const val RESUME_WAIT_MILLIS = 5_000L

@Composable
private fun RingContent(
    screen: RingScreen,
    hasMissions: Boolean,
    send: (RingEvent) -> Unit,
    closeWakeUp: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(MorningTheme.colors.backgroundBase).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (screen) {
            RingScreen.Ringing -> {
                Title(stringResource(R.string.ring_temp_ringing))
                if (hasMissions) {
                    Button(R.string.ring_button_start_mission) { send(RingEvent.MissionStarted) }
                } else {
                    Button(R.string.ring_button_dismiss) { send(RingEvent.DismissWithoutMission) }
                }
            }
            is RingScreen.Mission -> {
                Title(stringResource(R.string.ring_temp_mission, screen.index + 1))
                Button(R.string.ring_button_touch) { send(RingEvent.MissionInteraction) }
                Button(R.string.ring_button_mission_done) { send(RingEvent.MissionCompleted) }
            }
            RingScreen.WakeUp -> {
                Title(stringResource(R.string.ring_temp_wake_up))
                Button(R.string.ring_button_ok, closeWakeUp)
            }
            RingScreen.WakeCheck -> {
                Title(stringResource(R.string.ring_temp_wake_check))
                Button(R.string.ring_button_awake) { send(RingEvent.WakeCheckConfirmed) }
            }
            RingScreen.None -> Unit
        }
    }
}

@Composable
private fun Title(text: String) =
    Text(text, style = MorningTheme.typography.headlineLarge, color = MorningTheme.colors.textPrimary)

@Composable
private fun Button(label: Int, onClick: () -> Unit) = MorningButton(stringResource(label), onClick = onClick)
