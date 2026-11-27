package com.lumian.morningup.core.alarm.ring

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import androidx.core.content.edit
import com.lumian.morningup.core.alarm.AlarmDiagnostics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 설정 > 소리 '이어폰, 블루투스로 울리기'(TSK-276 3절, TSK-287). 처음 꺼짐 = 지금 동작 그대로(길을 정하지 않아요).
 * 켜면 울리기 시작할 때마다 연결된 블루투스, 유선 이어폰을 찾아 그쪽으로 울리고, 없으면 폰 스피커예요.
 * 안전장치: 다시 울림 두 번째부터([SPEAKER_FROM_RESTS]번 쉬고 난 뒤)는 연결과 상관없이 폰 스피커로 울려요.
 */
@Singleton
class AlarmOutputRoute @Inject constructor(
    @ApplicationContext private val context: Context,
    private val diagnostics: AlarmDiagnostics,
) {
    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    private val enabledState by lazy { MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false)) }

    val enabled: StateFlow<Boolean> get() = enabledState.asStateFlow()

    fun setEnabled(on: Boolean) {
        prefs.edit { putBoolean(KEY_ENABLED, on) }
        enabledState.value = on
    }

    /** 이번 울림을 [player]로 낼 길을 정해요. [restsDone]은 이 알람에서 다시 울림 사이에 쉰 횟수예요. */
    fun apply(player: MediaPlayer, restsDone: Int) {
        if (!enabledState.value || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val manager = context.getSystemService(AudioManager::class.java) ?: return
        val outputs = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        val target = if (useSpeaker(restsDone)) {
            outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
        } else {
            outputs.firstOrNull { it.type in EXTERNAL_TYPES }
                ?: outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
        }
        val ok = target != null && runCatching { player.setPreferredDevice(target) }.getOrDefault(false)
        diagnostics.record("sound_route", detail = "type=${target?.type} rests=$restsDone ok=$ok")
    }

    companion object {
        private const val PREFS = "alarm_output"
        private const val KEY_ENABLED = "external_output"

        /** 다시 울림 두 번째부터 폰 스피커예요(처음 울림과 첫 다시 울림까지만 이어폰). */
        const val SPEAKER_FROM_RESTS = 2

        fun useSpeaker(restsDone: Int): Boolean = restsDone >= SPEAKER_FROM_RESTS

        private val EXTERNAL_TYPES = buildSet {
            add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
            add(AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
            add(AudioDeviceInfo.TYPE_WIRED_HEADSET)
            add(AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
            add(AudioDeviceInfo.TYPE_USB_HEADSET)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(AudioDeviceInfo.TYPE_BLE_HEADSET)
        }
    }
}
