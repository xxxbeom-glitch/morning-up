package com.lumian.morningup.core.alarm.ring

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
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

    /**
     * 앱이 사용자 방해 금지를 풀 수 있는 판인지. 안드로이드 15부터는 앱의 setInterruptionFilter가 앱 규칙(AutomaticZenRule)
     * 하나만 켜고 끄고, 사용자가 켠 방해 금지(더 엄격한 쪽)가 이겨서 알람 소리를 풀 수 없어요(ISS-057).
     */
    fun canOverride(): Boolean
}

/**
 * 울리는 동안 방해 금지를 '알람만 허용'으로 잠깐 바꾸기(TSK-177 답 3, permission-matrix R3).
 * 사용자가 설정 탭에서 켰을 때만([enabled], 처음 꺼짐) 하고, 방해 금지 접근 권한이 있어야 해요.
 * 소리를 내는 쪽이 [hold]로 잡고 [release]로 놓아요. 모두 놓으면 원래 방해 금지 상태로 돌려요.
 * 울리는 동안 사용자나 루틴이 방해 금지를 다시 바꿨으면 그 값을 그대로 둬요.
 * 안드로이드 15부터는 풀 수 없어서 바꾸지 않고 기록만 해요. 설정 탭은 '알람 허용' 안내(T2-1)로 보내요(ISS-057).
 * 바꾼 상태는 저장해 두고, 앱이 강제 종료돼 되돌리지 못했으면 다음 시작 때 [recoverLeftover]가 돌려요.
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

    /** 이 폰에서 잠깐 풀기가 되는지(안드로이드 14까지). 안 되면 설정 탭이 T2-1 안내로 보내요. */
    val supported: Boolean get() = control.canOverride()

    /**
     * 앱 시작 때 한 번. 지난번에 바꾸고 되돌리지 못한 방해 금지를 돌려요. 안드로이드 15부터는 예전 판이 남긴 앱 규칙을
     * 꺼요(앱 규칙만 꺼지고 사용자가 켠 방해 금지는 그대로예요).
     */
    @Synchronized
    fun recoverLeftover() {
        if (holders > 0 || !control.accessGranted()) return
        val active = prefs.getBoolean(KEY_ACTIVE, false)
        if (!control.canOverride()) {
            if (active || !prefs.getBoolean(KEY_APP_RULE_CLEARED, false)) {
                runCatching { control.setFilter(ALL) }
                record(EVENT_LEFTOVER, "app_rule_off")
            }
            prefs.edit {
                putBoolean(KEY_APP_RULE_CLEARED, true)
                remove(KEY_ACTIVE)
                remove(KEY_SAVED)
            }
            return
        }
        if (!active) return
        val from = prefs.getInt(KEY_SAVED, ALL)
        if (control.filter() == ALARMS_ONLY) {
            runCatching { control.setFilter(from) }
            record(EVENT_LEFTOVER, "$ALARMS_ONLY->$from")
        }
        clearActive()
    }

    private var holders = 0
    private var saved: Int? = null

    @Synchronized
    fun hold() {
        holders++
        if (holders > 1 || !enabledState.value || !control.silenced()) return
        if (!control.canOverride()) {
            record(EVENT_UNSUPPORTED, "")
            return
        }
        if (!control.accessGranted()) {
            record(EVENT_NO_ACCESS, "")
            return
        }
        val current = control.filter()
        runCatching { control.setFilter(ALARMS_ONLY) }
            .onSuccess {
                saved = current
                prefs.edit {
                    putBoolean(KEY_ACTIVE, true)
                    putInt(KEY_SAVED, current)
                }
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
        clearActive()
        val current = control.filter()
        if (current == ALARMS_ONLY && control.accessGranted()) {
            runCatching { control.setFilter(from) }
            record(EVENT_RESTORED, "$ALARMS_ONLY->$from")
        } else {
            record(EVENT_KEPT, "now=$current")
        }
    }

    private fun clearActive() = prefs.edit {
        remove(KEY_ACTIVE)
        remove(KEY_SAVED)
    }

    companion object {
        private const val PREFS = "dnd_override"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_ACTIVE = "active"
        private const val KEY_SAVED = "saved_filter"
        private const val KEY_APP_RULE_CLEARED = "app_rule_cleared"
        internal const val ALL = NotificationManager.INTERRUPTION_FILTER_ALL
        internal const val ALARMS_ONLY = NotificationManager.INTERRUPTION_FILTER_ALARMS
        const val EVENT_CHANGED = "dnd_alarms_only"
        const val EVENT_RESTORED = "dnd_restored"
        const val EVENT_KEPT = "dnd_kept_user"
        const val EVENT_FAILED = "dnd_change_failed"
        const val EVENT_NO_ACCESS = "dnd_no_access"
        const val EVENT_UNSUPPORTED = "dnd_cannot_override"
        const val EVENT_LEFTOVER = "dnd_leftover_restored"
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

    override fun canOverride(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM
}
