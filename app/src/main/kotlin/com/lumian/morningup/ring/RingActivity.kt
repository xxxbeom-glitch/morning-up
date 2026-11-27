package com.lumian.morningup.ring

import com.lumian.morningup.core.designsystem.theme.LocalThemeMode
import com.lumian.morningup.core.designsystem.theme.rememberThemeMode
import androidx.compose.runtime.CompositionLocalProvider
import com.lumian.morningup.BuildConfig
import android.app.KeyguardManager
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.lumian.morningup.MainActivity
import com.lumian.morningup.core.alarm.AlarmDiagnostics
import com.lumian.morningup.core.alarm.AlarmIntents
import com.lumian.morningup.core.alarm.ring.RingController
import com.lumian.morningup.core.alarm.ring.RingNotifications
import com.lumian.morningup.core.alarm.ring.RingScreen
import com.lumian.morningup.core.alarm.ring.RingService
import com.lumian.morningup.core.data.session.RingSessionStore
import com.lumian.morningup.core.designsystem.theme.FitDesignWidth
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.engine.RingEvent
import com.lumian.morningup.core.model.engine.RingPhase
import com.lumian.morningup.feature.mission.RingEscapeRoute
import com.lumian.morningup.feature.mission.RingMissionRoute
import com.lumian.morningup.feature.ring.RingRoute
import com.lumian.morningup.ui.SystemBarIcons
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 울림 화면을 잠금 화면 위에 띄우는 액티비티. 화면은 `:feature:ring`(D0, W1, W2)이 그리고,
 * 미션 자리에는 `:feature:mission`을 꽂아요(feature끼리 직접 부르지 않게 여기서 이어요).
 * 뒤로 가기는 화면 쪽 BackHandler가 막아요(W1만 '확인'과 같게 닫혀요).
 */
@AndroidEntryPoint
class RingActivity : ComponentActivity() {
    @Inject lateinit var controller: RingController

    @Inject lateinit var sessions: RingSessionStore

    @Inject lateinit var diagnostics: AlarmDiagnostics

    /** 저장된 울림을 다 읽기 전에는 닫지 않아요(앱이 새로 뜬 경우 처음엔 상태가 비어 있어요). */
    private val loaded = MutableStateFlow(false)

    /** 쉬는 중 알림을 눌러 열었어요(결정 70). 쉬는 동안에도 울림 화면을 보여 줘요. */
    private var openedFromRest = false

    /** W3 기다림 알림이나 홈 띠를 눌러 열었어요(결정 145). 정한 때보다 먼저 W3를 보여 줘요. */
    private var openedForRecheck = false

    /** 미션 비상구 알림을 눌러 열었어요(결정 171). 저장된 울림을 읽은 뒤 M5를 열어요. */
    private var openedForEscape = false

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(RingNotifications.EXTRA_FROM_REST, false)) controller.openDuringRest()
        if (intent.getBooleanExtra(RingNotifications.EXTRA_OPEN_RECHECK, false)) controller.openRecheckEarly()
        if (intent.getBooleanExtra(RingNotifications.EXTRA_OPEN_ESCAPE, false)) openEscape()
    }

    /** 미션 비상구 알림을 눌렀어요. M5를 열고 소리를 끄도록 울림 서비스에 알려요(결정 171). 열었으면 true예요. */
    private fun openEscape(): Boolean {
        if (!controller.openEscape()) return false
        RingService.send(this, RingEvent.EscapeOpened)
        return true
    }

    /** 울림 화면이 떠 있는 동안 옆 볼륨 버튼은 아무 일도 안 해요(11-17 사용자 지시: 소리를 줄여 끄지 못하게). */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode !in VOLUME_KEYS) return super.onKeyDown(keyCode, event)
        if (event?.repeatCount == 0) {
            diagnostics.record(
                "volume_key_blocked",
                controller.session.value?.plan?.alarmId,
                KeyEvent.keyCodeToString(keyCode),
            )
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean =
        keyCode in VOLUME_KEYS || super.onKeyUp(keyCode, event)

    override fun onCreate(savedInstanceState: Bundle?) {
        showOverLockScreen()
        // 홈(MainActivity)과 같이 내비게이션 바 뒤에 흰 막을 깔지 않고 하늘, 미션 바탕이 끝까지 이어지게 해요.
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        openedFromRest = intent?.getBooleanExtra(RingNotifications.EXTRA_FROM_REST, false) == true
        if (openedFromRest) controller.openDuringRest()
        openedForRecheck = intent?.getBooleanExtra(RingNotifications.EXTRA_OPEN_RECHECK, false) == true
        if (openedForRecheck && controller.openRecheckEarly()) openedForRecheck = false
        openedForEscape = intent?.getBooleanExtra(RingNotifications.EXTRA_OPEN_ESCAPE, false) == true
        if (openedForEscape && openEscape()) openedForEscape = false
        resumeIfNeeded()
        lifecycleScope.launch {
            // 세션이 보이면 읽기가 끝난 것이에요. 앱이 새로 떠서 늦게 읽혔으면 그때 쉬는 중 화면을 열어요.
            controller.session.collect {
                if (it != null) {
                    if (openedFromRest) {
                        controller.openDuringRest()
                        openedFromRest = false
                    }
                    if (openedForRecheck) {
                        controller.openRecheckEarly()
                        openedForRecheck = false
                    }
                    if (openedForEscape) {
                        openEscape()
                        openedForEscape = false
                    }
                    loaded.value = true
                }
                keepScreenOn(it?.phase)
            }
        }
        setContent {
            FitDesignWidth {
                // 디버그 빌드에서 고른 화면 모드(TSK-166)를 울림 화면에도 내려 줘요.
                val themeMode = rememberThemeMode(enabled = BuildConfig.DEBUG)
                MorningTheme {
                  CompositionLocalProvider(LocalThemeMode provides themeMode) {
                    val ready by loaded.collectAsStateWithLifecycle()
                    RingRoute(
                        canClose = ready,
                        onClose = ::finish,
                        onWakeUpConfirmed = ::afterWakeUp,
                        missionContent = { _ ->
                            LightSystemBarIcons()
                            RingMissionRoute()
                        },
                        escapeContent = {
                            LightSystemBarIcons()
                            RingEscapeRoute()
                        },
                        // 다크 울림 화면에서 상태 표시줄 아이콘이 검게 남아 안 보였어요(ISS-047).
                        systemBars = { dark -> SystemBarIcons(dark) },
                    )
                  }
                }
            }
        }
    }

    /** 미션 화면은 바탕이 어두워서, 떠 있는 동안만 상태 표시줄과 내비게이션 줄 아이콘을 흰색으로 바꿔요. */
    @Composable
    private fun LightSystemBarIcons() {
        DisposableEffect(Unit) {
            val bars = WindowCompat.getInsetsController(window, window.decorView)
            val status = bars.isAppearanceLightStatusBars
            val navigation = bars.isAppearanceLightNavigationBars
            bars.isAppearanceLightStatusBars = false
            bars.isAppearanceLightNavigationBars = false
            onDispose {
                bars.isAppearanceLightStatusBars = status
                bars.isAppearanceLightNavigationBars = navigation
            }
        }
    }

    /** W1 '확인'. 잠긴 채 껐으면 닫기만, 풀려 있었으면 앱 홈으로 가요. */
    private fun afterWakeUp() {
        val locked = getSystemService(KeyguardManager::class.java)?.isKeyguardLocked ?: true
        Timber.i("wake_up_closed locked=%s", locked)
        if (!locked) {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            )
        }
        finish()
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

    /**
     * 화면 켜 두기는 울리는 중, 미션 중, W2/W3를 묻는 중에만 걸어요. 쉬는 중, W2/W3 기다림, 끝난 뒤(W1) 화면이 남아 있으면
     * 풀어서 폰 설정 시간대로 꺼지게 해요(TSK-278, 배터리 점검 5번). 다시 울리면 서비스가 화면을 새로 켜요.
     */
    private fun keepScreenOn(phase: RingPhase?) {
        val keep = phase == RingPhase.Ringing || phase == RingPhase.InMission || phase?.isAsking == true
        if (keep) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
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

private val VOLUME_KEYS = setOf(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE)
