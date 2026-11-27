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
 * 전원 종료 방지(F1, P6, P7). 알람이 끝나기 전에 전원 메뉴 창이 뜨면 바로 닫아요.
 * 창이 바뀌었다는 신호와 그 창의 앱 이름, 창 종류만 보고 화면 글이나 내용은 읽지 않아요(P6 약속).
 * 설정 탭에서 전원 종료 방지를 켜 두었고(결정 101) 미션이 있는 알람일 때 동작해요. 처음엔 미션 중에만 막았는데, 울림 화면에서
 * '미션 시작'을 누르기 전이나 다시 울림 사이에 꺼 버리면 미션을 건너뛰게 돼서 울림, 쉬는 중, 미션 중 모두 막아요(TSK-254).
 * W1 뒤 확인(W2, W3)과 그 밖에는 아무것도 하지 않아요.
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
                on && session != null && session.phase in GUARDED_PHASES && session.plan.missions.isNotEmpty()
            }
            .onEach { guarding = it }
            .launchIn(scope)
        diagnostics.record("power_guard_connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!guarding || event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString()
        val cls = event.className?.toString()
        if (!isPowerMenu(pkg, cls)) {
            // 기종마다 전원 메뉴 창 이름이 달라요. 시스템 창이 바뀌면 앱 이름과 창 종류만 남겨 실기기에서 맞춰요(글은 안 읽어요).
            if (isSystemWindow(pkg)) diagnostics.record("power_guard_window", detail = "pkg=$pkg cls=$cls")
            return
        }
        val closed = performGlobalAction(GLOBAL_ACTION_BACK)
        diagnostics.record("power_menu_closed", detail = "pkg=$pkg cls=$cls ok=$closed")
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        /** 막는 때: 울리는 중, 다시 울림 사이에 쉬는 중, 미션 중. */
        val GUARDED_PHASES = setOf(RingPhase.Ringing, RingPhase.Resting, RingPhase.InMission)

        /** 전원 메뉴 창 종류에 흔히 들어가는 이름(AOSP GlobalActionsDialog(Lite), 삼성 SecGlobalActions 등). */
        private val POWER_MENU_CLASS_HINTS = listOf("GlobalActions", "PowerMenu", "PowerOptions", "Shutdown")

        private fun isSystemWindow(packageName: String?): Boolean {
            val pkg = packageName.orEmpty()
            return pkg == "com.android.systemui" || pkg.startsWith("com.samsung.android") || pkg == "android"
        }

        /**
         * 전원 메뉴 창인지. 기본 안드로이드는 시스템 UI의 GlobalActions 창이고, 삼성은 따로 된 globalactions 앱이에요.
         * 삼성 메뉴 이름은 실기기에서 다시 확인해요(design-system P7 메모).
         */
        fun isPowerMenu(packageName: String?, className: String?): Boolean {
            val pkg = packageName.orEmpty()
            val cls = className.orEmpty()
            return pkg.contains("globalactions", ignoreCase = true) ||
                (isSystemWindow(pkg) && POWER_MENU_CLASS_HINTS.any { cls.contains(it, ignoreCase = true) })
        }
    }
}
