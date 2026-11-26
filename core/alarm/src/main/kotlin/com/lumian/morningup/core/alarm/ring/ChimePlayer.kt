package com.lumian.morningup.core.alarm.ring

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.lumian.morningup.core.alarm.AlarmDiagnostics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * W2, W3가 뜰 때 짧은 알림음과 진동 2번(결정 145 3번). 알람 소리 통로(USAGE_ALARM)로 알람 볼륨의 30% 크기로
 * 한 번만 울려요. 무음 모드에서도 들려요. 폰 알람 볼륨이 0이면 울리는 동안 [AlarmVolumeGuard]가
 * 최소 크기로 올리고 끝나면 돌려요(결정 143 대책을 W3에도).
 */
@Singleton
class ChimePlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val diagnostics: AlarmDiagnostics,
    private val volumeGuard: AlarmVolumeGuard,
) {
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private var player: MediaPlayer? = null
    private var volumeHeld = false

    @Synchronized
    fun play(alarmId: Long?) {
        val audio = context.getSystemService(AudioManager::class.java)
        if (audio?.getStreamVolume(AudioManager.STREAM_ALARM) == 0) diagnostics.record("chime_volume_zero", alarmId)
        vibrateTwice()
        release()
        volumeHeld = true
        volumeGuard.hold(alarmId)
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) ?: return release()
        val mp = MediaPlayer()
        player = try {
            mp.setAudioAttributes(attributes)
            mp.setDataSource(context, uri)
            mp.prepare()
            val volume = RingMute.volume(VOLUME_FRACTION)
            mp.setVolume(volume, volume)
            mp.setOnCompletionListener { done -> synchronized(this) { if (player === done) release() } }
            mp.start()
            diagnostics.record("chime", alarmId)
            mp
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            diagnostics.record("chime_failed", alarmId, e.javaClass.simpleName)
            mp.release()
            release()
            null
        }
    }

    @Synchronized
    fun release() {
        player?.release()
        player = null
        if (volumeHeld) {
            volumeHeld = false
            volumeGuard.release()
        }
    }

    private fun vibrateTwice() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(TWICE, NO_REPEAT), attributes)
    }

    private companion object {
        /** 알람 볼륨의 30%. */
        const val VOLUME_FRACTION = 0.3f
        const val NO_REPEAT = -1
        val TWICE = longArrayOf(0, 200, 150, 200)
    }
}
