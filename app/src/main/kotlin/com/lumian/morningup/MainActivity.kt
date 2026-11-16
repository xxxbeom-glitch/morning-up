package com.lumian.morningup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.engine.AlarmRescheduler
import com.lumian.morningup.ui.DebugAlarmTools
import com.lumian.morningup.ui.MorningUpApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var rescheduler: AlarmRescheduler

    @Inject lateinit var now: NowProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // 앱을 열 때마다 예약을 맞춰요. 시스템 신호를 놓쳤거나 예약이 지워진 경우를 메워요.
        lifecycleScope.launch { rescheduler.rescheduleAll(now.now()) }
        setContent {
            MorningTheme {
                MorningUpApp(debugTools = { if (BuildConfig.DEBUG) DebugAlarmTools() })
            }
        }
    }
}
