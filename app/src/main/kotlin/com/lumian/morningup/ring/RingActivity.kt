package com.lumian.morningup.ring

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.lumian.morningup.MainActivity
import com.lumian.morningup.core.alarm.AlarmIntents
import com.lumian.morningup.core.alarm.ring.RingController
import com.lumian.morningup.core.alarm.ring.RingScreen
import com.lumian.morningup.core.data.session.RingSessionStore
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.feature.mission.RingMissionRoute
import com.lumian.morningup.feature.ring.RingRoute
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

    /** 저장된 울림을 다 읽기 전에는 닫지 않아요(앱이 새로 뜬 경우 처음엔 상태가 비어 있어요). */
    private val loaded = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        showOverLockScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        resumeIfNeeded()
        lifecycleScope.launch {
            // 세션이 보이면 읽기가 끝난 것이에요.
            controller.session.collect { if (it != null) loaded.value = true }
        }
        setContent {
            MorningTheme {
                val ready by loaded.collectAsStateWithLifecycle()
                RingRoute(
                    canClose = ready,
                    onClose = ::finish,
                    onWakeUpConfirmed = ::afterWakeUp,
                    missionContent = { _ -> RingMissionRoute() },
                )
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
