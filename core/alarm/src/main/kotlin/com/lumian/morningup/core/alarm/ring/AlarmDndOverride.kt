package com.lumian.morningup.core.alarm.ring

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.lumian.morningup.core.alarm.AlarmDiagnostics
import com.lumian.morningup.core.alarm.AlarmSilenceReader
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 방해 금지를 읽고 바꾸는 창구. 시험에서는 가짜로 바꿔 끼워요. */
internal interface DndControl {
    /** 방해 금지 접근 권한이 있는지. 없으면 앱이 방해 금지를 바꿀 수 없어요. */
    fun accessGranted(): Boolean

    /** 지금 방해 금지가 알람 소리를 막는지([AlarmSilenceReader]). */
    fun silenced(): Boolean

    fun filter(): Int

    fun setFilter(filter: Int)
}

/**
 * 울리는 동안 방해 금지를 '알람만 허용'으로 잠깐 바꾸기(TSK-177 답 3, permission-matrix R3).
 * 사용자가 설정 탭에서 켰을 때만([enabled], 처음 꺼짐) 하고, 방해 금지 접근 권한이 있어야 해요.
 * 소리를 내는 쪽이 [hold]로 잡고 [release]로 놓아요. 모두 놓으면 원래 방해 금지 상태로 돌려요.
 * 울리는 동안 사용자나 루틴이 방해 금지를 다시 바꿨으면 그 값을 그대로 둬요.
 */
@Singleton
class AlarmDndOverride internal constructor(
    private val prefs: SharedPreferences,
    private val control: DndControl,
    private val record: (event: String, detail: String) -> Unit,
) {
    @Inject constructor(
        @ApplicationContext context: Context,
        silence: AlarmSilenceReader,
        diagnostics: AlarmDiagnostics,
    ) : this(
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE),
        AndroidDndControl(context, silence),
        { event, detail -> diagnostics.record(event, null, detail) },
    )

    private val enabledState = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))

    /** 설정 탭 '울릴 때 방해 금지 잠깐 끄기' 토글. */
    val enabled: StateFlow<Boolean> = enabledState.asStateFlow()

    fun setEnabled(on: Boolean) {
        prefs.edit { putBoolean(KEY_ENABLED, on) }
        enabledState.value = on
    }

    /** 방해 금지 접근 권한이 있는지. 설정 탭 토글이 켜진 모양인지 정할 때 써요. */
    fun accessGranted(): Boolean = control.accessGranted()

    private var holders = 0
    private var saved: Int? = null

    @Synchronized
    fun hold() {
        holders++
        if (holders > 1 || !enabledState.value || !control.silenced()) return
        if (!control.accessGranted()) {
            record(EVENT_NO_ACCESS, "")
            return
        }
        val current = control.filter()
        runCatching { control.setFilter(ALARMS_ONLY) }
            .onSuccess {
                saved = current
                record(EVENT_CHANGED, "$current->$ALARMS_ONLY")
            }
            .onFailure { record(EVENT_FAILED, it.javaClass.simpleName) }
    }

    @Synchronized
    fun release() {
        holders = (holders - 1).coerceAtLeast(0)
        if (holders > 0) return
        val from = saved ?: return
        saved = null
        val current = control.filter()
        if (current == ALARMS_ONLY && control.accessGranted()) {
            runCatching { control.setFilter(from) }
            record(EVENT_RESTORED, "$ALARMS_ONLY->$from")
        } else {
            record(EVENT_KEPT, "now=$current")
        }
    }

    companion object {
        private const val PREFS = "dnd_override"
        private const val KEY_ENABLED = "enabled"
        internal const val ALARMS_ONLY = NotificationManager.INTERRUPTION_FILTER_ALARMS
        const val EVENT_CHANGED = "dnd_alarms_only"
        const val EVENT_RESTORED = "dnd_restored"
        const val EVENT_KEPT = "dnd_kept_user"
        const val EVENT_FAILED = "dnd_change_failed"
        const val EVENT_NO_ACCESS = "dnd_no_access"
    }
}

private class AndroidDndControl(context: Context, private val silence: AlarmSilenceReader) : DndControl {
    private val manager = context.getSystemService(NotificationManager::class.java)

    override fun accessGranted(): Boolean = manager?.isNotificationPolicyAccessGranted ?: false

    override fun silenced(): Boolean = silence.silenced()

    override fun filter(): Int = manager?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_UNKNOWN

    override fun setFilter(filter: Int) {
        manager?.setInterruptionFilter(filter)
    }
}
