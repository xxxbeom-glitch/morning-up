package com.lumian.morningup.core.alarm.ring

import android.content.Context
import android.media.AudioManager
import android.os.Build
import com.lumian.morningup.core.alarm.AlarmDiagnostics
import com.lumian.morningup.core.model.engine.VolumeFloor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 알람 볼륨 0 대책(결정 143, C-14). 소리를 내는 쪽이 [hold]로 잡고 [release]로 놓아요.
 * 처음 잡을 때 알람 볼륨이 가장 작으면 [VolumeFloor]대로 올리고, 모두 놓으면 원래대로 돌려요.
 * 울리는 동안 사용자가 볼륨을 직접 바꿨으면 그 값을 그대로 둬요.
 */
@Singleton
class AlarmVolumeGuard internal constructor(
    private val audio: AudioManager?,
    private val record: (event: String, alarmId: Long?, detail: String) -> Unit,
) {
    @Inject constructor(
        @ApplicationContext context: Context,
        diagnostics: AlarmDiagnostics,
    ) : this(context.getSystemService(AudioManager::class.java), diagnostics::record)

    private var holders = 0
    private var saved: Int? = null
    private var raised: Int? = null

    @Synchronized
    fun hold(alarmId: Long?) {
        holders++
        if (holders > 1) return
        val a = audio ?: return
        val current = a.getStreamVolume(STREAM)
        val min = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) a.getStreamMinVolume(STREAM) else 0
        val to = VolumeFloor.raisedTo(current, min, a.getStreamMaxVolume(STREAM)) ?: return
        runCatching { a.setStreamVolume(STREAM, to, 0) }
            .onSuccess {
                saved = current
                raised = to
                record(EVENT_RAISED, alarmId, "$current->$to")
            }
            .onFailure { record(EVENT_FAILED, alarmId, it.javaClass.simpleName) }
    }

    @Synchronized
    fun release() {
        if (holders == 0) return
        holders--
        if (holders > 0) return
        val to = raised ?: return
        val from = saved ?: return
        raised = null
        saved = null
        val a = audio ?: return
        val current = a.getStreamVolume(STREAM)
        if (VolumeFloor.shouldRestore(current, to)) {
            runCatching { a.setStreamVolume(STREAM, from, 0) }
            record(EVENT_RESTORED, null, "$to->$from")
        } else {
            record(EVENT_KEPT, null, "user=$current")
        }
    }

    companion object {
        private const val STREAM = AudioManager.STREAM_ALARM
        const val EVENT_RAISED = "volume_raised"
        const val EVENT_FAILED = "volume_raise_failed"
        const val EVENT_RESTORED = "volume_restored"
        const val EVENT_KEPT = "volume_kept_user"
    }
}
