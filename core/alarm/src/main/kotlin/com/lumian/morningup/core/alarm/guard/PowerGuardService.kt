package com.lumian.morningup.core.alarm.guard

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.lumian.morningup.core.alarm.AlarmDiagnostics
import com.lumian.morningup.core.alarm.ring.RingScreenIntentFactory
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

    @Inject lateinit var ringScreen: RingScreenIntentFactory

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile private var guarding = false

    /** 울림 화면을 다시 띄울 때인지(울리는 중, 미션 중). 쉬는 중에는 메뉴만 닫아요. */
    @Volatile private var reopenScreen = false

    private val main = Handler(Looper.getMainLooper())
    private var lastCloseAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        sessions.session
            .combine(setting.enabled) { session, on ->
                on && session != null && session.phase in GUARDED_PHASES && session.plan.missions.isNotEmpty()
            }
            .onEach { guarding = it }
            .launchIn(scope)
        sessions.session
            .onEach { reopenScreen = it?.phase in REOPEN_PHASES }
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
        val now = SystemClock.elapsedRealtime()
        if (now - lastCloseAt < CLOSE_DEBOUNCE_MILLIS) return
        lastCloseAt = now
        closePowerMenu("pkg=$pkg cls=$cls")
    }

    /**
     * 삼성 전원 메뉴는 '뒤로'만으로는 안 닫혀요(ISS-060). 알라미처럼 여러 겹으로 닫아요.
     * 1) 뒤로와 시스템 대화 닫기 신호, 2) 홈(시스템이 홈으로 갈 때 전원 메뉴 같은 시스템 대화를 모두 닫아요),
     * 3) 울리는 중이나 미션 중이면 울림 화면을 다시 맨 앞으로. 순서와 시간은 [CLOSE_STEPS].
     */
    private fun closePowerMenu(detail: String) {
        CLOSE_STEPS.forEach { step ->
            main.postDelayed({ if (guarding) run(step, detail) }, step.delayMillis)
        }
    }

    private fun run(step: CloseStep, detail: String) {
        val ok = when (step.action) {
            CloseAction.Back -> closeSystemDialogs().let { performGlobalAction(GLOBAL_ACTION_BACK) }
            CloseAction.Home -> performGlobalAction(GLOBAL_ACTION_HOME)
            CloseAction.ReopenScreen -> !reopenScreen || reopenRingScreen()
        }
        diagnostics.record("power_menu_closed", detail = "$detail step=${step.action} ok=$ok")
    }

    /** 안드로이드 12부터 막힌 기종이 많아서 실패해도 넘어가요. */
    @Suppress("DEPRECATION")
    private fun closeSystemDialogs(): Boolean = runCatching {
        sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
    }.isSuccess

    private fun reopenRingScreen(): Boolean = runCatching {
        startActivity(ringScreen.create(this).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
    }.isSuccess

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        /** 막는 때: 울리는 중, 다시 울림 사이에 쉬는 중, 미션 중. */
        val GUARDED_PHASES = setOf(RingPhase.Ringing, RingPhase.Resting, RingPhase.InMission)

        /** 메뉴를 닫은 뒤 울림 화면을 다시 띄우는 때. */
        val REOPEN_PHASES = setOf(RingPhase.Ringing, RingPhase.InMission)

        /** 닫는 순서(ISS-060): 뒤로 → 0.2초 뒤 홈 → 0.5초 뒤 울림 화면 다시 맨 앞. */
        val CLOSE_STEPS = listOf(
            CloseStep(0L, CloseAction.Back),
            CloseStep(200L, CloseAction.Home),
            CloseStep(500L, CloseAction.ReopenScreen),
        )

        /** 한 번 닫는 동안 같은 메뉴 신호가 겹쳐 와도 다시 시작하지 않아요. 다시 열면 이 시간 뒤부터 또 닫아요. */
        const val CLOSE_DEBOUNCE_MILLIS = 700L

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

    enum class CloseAction { Back, Home, ReopenScreen }

    data class CloseStep(val delayMillis: Long, val action: CloseAction)
}
