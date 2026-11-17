package com.lumian.morningup.core.alarm.guard

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.lumian.morningup.core.alarm.AlarmDiagnostics
import com.lumian.morningup.core.data.session.RingSessionStore
import com.lumian.morningup.core.model.engine.RingPhase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * 전원 종료 방지(F1, P6, P7). 미션을 하는 동안 전원 메뉴 창이 뜨면 바로 닫아요.
 * 창이 바뀌었다는 신호와 그 창의 앱 이름, 창 종류만 보고 화면 글이나 내용은 읽지 않아요(P6 약속).
 * 설정 탭에서 전원 종료 방지를 켜 두었고(결정 101) 미션이 있는 알람의 미션 중일 때만 동작해요. 그 밖에는 아무것도 하지 않아요.
 */
@AndroidEntryPoint
class PowerGuardService : AccessibilityService() {
    @Inject lateinit var sessions: RingSessionStore

    @Inject lateinit var diagnostics: AlarmDiagnostics

    @Inject lateinit var setting: PowerGuardConsent

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile private var guarding = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        sessions.session
            .combine(setting.enabled) { session, on ->
                on && session?.phase == RingPhase.InMission && session.plan.missions.isNotEmpty()
            }
            .onEach { guarding = it }
            .launchIn(scope)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!guarding || event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!isPowerMenu(event.packageName?.toString(), event.className?.toString())) return
        performGlobalAction(GLOBAL_ACTION_BACK)
        diagnostics.record("power_menu_closed")
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        /**
         * 전원 메뉴 창인지. 기본 안드로이드는 시스템 UI의 GlobalActions 창이고, 삼성은 따로 된 globalactions 앱이에요.
         * 삼성 메뉴 이름은 실기기에서 다시 확인해요(design-system P7 메모).
         */
        fun isPowerMenu(packageName: String?, className: String?): Boolean {
            val pkg = packageName.orEmpty()
            val cls = className.orEmpty()
            return pkg.contains("globalactions", ignoreCase = true) ||
                (pkg == "com.android.systemui" && cls.contains("GlobalActions", ignoreCase = true))
        }
    }
}
