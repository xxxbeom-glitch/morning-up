package com.lumian.morningup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.lumian.morningup.core.alarm.AlarmDiagnostics
import com.lumian.morningup.core.alarm.AlarmIntents
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.alarm.ring.RingNotifications
import com.lumian.morningup.core.data.session.RingSessionStore
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.PermissionChecker
import com.lumian.morningup.core.model.engine.AlarmRescheduler
import com.lumian.morningup.ring.RingActivity
import com.lumian.morningup.ui.DebugAlarmTools
import com.lumian.morningup.ui.MorningNavHost
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

    /** 열어 달라는 편집 화면의 알람 id. 화면 길이 받아 열면 비워요. */
    private val editRequests = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            // 울리는 중인 알람이 있으면 끌 수 있는 울림 화면으로 보내요(알림을 못 띄운 경우의 길).
            if (sessions.load() != null) startActivity(Intent(this@MainActivity, RingActivity::class.java))
            // 앱을 열 때마다 예약을 맞춰요. 시스템 신호를 놓쳤거나 예약이 지워진 경우를 메워요.
            rescheduler.rescheduleAll(now.now())
        }
        checkAlertPermissions()
        if (savedInstanceState == null) readEditRequest(intent)
        setContent {
            MorningTheme {
                MorningNavHost(
                    editRequests = editRequests,
                    onEditRequestHandled = { editRequests.value = null },
                    debugTools = { if (BuildConfig.DEBUG) DebugAlarmTools() },
                )
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

    /** 상태 표시줄 알람 아이콘으로 열렸으면 그 알람의 편집 화면을 열어 달라고 남겨요. */
    private fun readEditRequest(intent: Intent?) {
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
