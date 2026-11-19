package com.lumian.morningup

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.lumian.morningup.core.alarm.AlarmDiagnostics
import com.lumian.morningup.core.alarm.AlarmIntents
import com.lumian.morningup.core.alarm.BatteryReminder
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.alarm.ring.RingNotifications
import com.lumian.morningup.core.data.session.RingSessionStore
import com.lumian.morningup.core.designsystem.component.MorningOverlayRoot
import com.lumian.morningup.core.designsystem.theme.FitDesignWidth
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.PermissionChecker
import com.lumian.morningup.core.model.PermissionStep
import com.lumian.morningup.core.model.engine.AlarmRescheduler
import com.lumian.morningup.feature.settings.OnboardingStore
import com.lumian.morningup.ring.RingActivity
import com.lumian.morningup.ui.DebugAlarmTools
import com.lumian.morningup.ui.MorningNavHost
import com.lumian.morningup.ui.SystemBarIcons
import com.lumian.morningup.ui.rememberNightByClock
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var rescheduler: AlarmRescheduler

    @Inject lateinit var now: NowProvider

    @Inject lateinit var sessions: RingSessionStore

    @Inject lateinit var notifications: RingNotifications

    @Inject lateinit var diagnostics: AlarmDiagnostics

    @Inject lateinit var permissions: PermissionChecker

    @Inject lateinit var onboarding: OnboardingStore

    @Inject lateinit var batteryReminder: BatteryReminder

    /** 열어 달라는 편집 화면의 알람 id. 화면 길이 받아 열면 비워요. */
    private val editRequests = MutableStateFlow<Long?>(null)

    /** 배터리 전날 밤 알림으로 열렸을 때 열어 달라는 권한 시트 단계. 화면 길이 받아 열면 비워요. */
    private val permissionRequests = MutableStateFlow<PermissionStep?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // 버튼식 내비게이션 바 뒤에도 흰 막을 깔지 않고 화면 바탕색이 이어지게 해요.
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        lifecycleScope.launch {
            // 울리는 중인 알람이 있으면 끌 수 있는 울림 화면으로 보내요(알림을 못 띄운 경우의 길).
            if (sessions.load() != null) {
                startActivity(Intent(this@MainActivity, RingActivity::class.java))
            } else {
                // 울리는 알람이 없는데 남은 울림, 기다림 알림이 있으면 지워요(앱을 새로 깐 뒤 등).
                notifications.cancelRinging()
            }
            // 앱을 열 때마다 예약을 맞춰요. 시스템 신호를 놓쳤거나 예약이 지워진 경우를 메워요.
            rescheduler.rescheduleAll(now.now())
        }
        // 배터리 전날 밤 확인을 예약해요(결정 113, 119). 밤마다 스스로 다음 날을 다시 잡아요.
        // 온보딩 전용 확인 앱은 배터리 알림을 보내지 않아요(원래 앱과 겹치지 않게).
        if (!BuildConfig.ONBOARDING_ONLY) batteryReminder.schedule()
        checkAlertPermissions()
        if (savedInstanceState == null) readEditRequest(intent)
        // 첫 실행이면 온보딩부터(결정 131). 한 번 정하면 화면을 다시 그려도 바꾸지 않아요.
        // 온보딩 전용 확인 앱(11-19 사용자 지시)은 켤 때마다 온보딩만 보여요.
        val showOnboarding = BuildConfig.ONBOARDING_ONLY || !onboarding.isDone
        setContent {
            FitDesignWidth {
                // 테마는 지금 시각을 따라요(아침 라이트, 밤 다크). 폰 다크 모드 설정은 보지 않아요.
                val night = rememberNightByClock(now::now)
                MorningTheme(darkTheme = night) {
                    SystemBarIcons(dark = night)
                    // 결정 시트(전원 종료 방지 등)를 하단 메뉴와 시스템 바 뒤까지 덮어 그리는 자리.
                    MorningOverlayRoot {
                        MorningNavHost(
                            editRequests = editRequests,
                            onEditRequestHandled = { editRequests.value = null },
                            permissionRequests = permissionRequests,
                            onPermissionRequestHandled = { permissionRequests.value = null },
                            showOnboarding = showOnboarding,
                            onOnboardingFinished = onboarding::markDone,
                            onboardingOnly = BuildConfig.ONBOARDING_ONLY,
                            debugTools = { if (BuildConfig.DEBUG) DebugAlarmTools() },
                        )
                    }
                }
            }
        }
    }

    /** 기기 설정에서 권한을 바꾸고 돌아오면 바로 다시 확인해요(3번 흐름 표 14). */
    override fun onResume() {
        super.onResume()
        permissions.refresh()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readEditRequest(intent)
    }

    /**
     * 상태 표시줄 알람 아이콘으로 열렸으면 그 알람의 편집 화면을, 배터리 전날 밤 알림으로 열렸으면
     * 나중용 권한 시트 2단계를 열어 달라고 남겨요.
     */
    private fun readEditRequest(intent: Intent?) {
        if (intent?.getBooleanExtra(AlarmIntents.EXTRA_OPEN_BATTERY_SHEET, false) == true) {
            permissionRequests.value = PermissionStep.Optional
            intent.removeExtra(AlarmIntents.EXTRA_OPEN_BATTERY_SHEET)
        }
        val id = intent?.getLongExtra(AlarmIntents.EXTRA_EDIT_ALARM_ID, NO_ALARM) ?: NO_ALARM
        if (id != NO_ALARM) {
            editRequests.value = id
            intent?.removeExtra(AlarmIntents.EXTRA_EDIT_ALARM_ID)
        }
    }

    /**
     * 앱을 열 때는 권한 창을 띄우지 않아요(F21). 첫 알람을 저장할 때 한 번 묻고, 나머지는 H2 배너와 설정으로 안내해요.
     * 여기서는 전체 화면 권한이 꺼진 것만 진단 기록에 남겨요.
     */
    private fun checkAlertPermissions() {
        if (!notifications.canShowFullScreen()) diagnostics.record("full_screen_intent_denied")
    }

    private companion object {
        const val NO_ALARM = -1L
    }
}
