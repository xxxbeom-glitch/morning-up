package com.lumian.morningup.core.alarm.ring

import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.net.toUri
import com.lumian.morningup.core.alarm.AlarmDiagnostics
import com.lumian.morningup.core.alarm.R
import com.lumian.morningup.core.model.engine.AlarmStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * W2, W3가 뜰 때 짧은 알림음과 진동 2번(결정 145 3번). 알람 소리 통로(USAGE_ALARM)로 울려서 무음 모드에서도 들려요.
 * 폰 알람 볼륨이 0이면 울리는 동안 [AlarmVolumeGuard]가 최소 크기로 올리고 끝나면 돌려요(결정 143 대책).
 * W2 '깨어 있나요?'와 W3 '정말 깼어요?' 모두 앱에 넣은 증기 사이렌 소리(`res/raw/w3_recheck`)를 그 알람의 벨소리 볼륨
 * (S6 볼륨 시트) 그대로 울려요(사용자 지시 11-26, 결정 185). 언제 울릴지는 [ChimeNudge]예요. 소리 고르기는 아직 없어요.
 * 채널 소리가 아니라 직접 울려서 알림 채널을 바꿀 필요는 없어요.
 */
@Singleton
class ChimePlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val diagnostics: AlarmDiagnostics,
    private val volumeGuard: AlarmVolumeGuard,
    private val alarms: AlarmStore,
) {
    /**
     * W2, W3 알림음. 알람의 벨소리 볼륨으로 울려요. 알람을 못 읽으면 100%예요. [label]은 진단 기록 이름(`w2`, `w3`)이에요.
     */
    suspend fun playSiren(alarmId: Long?, label: String) {
        val percent = alarmId?.let { alarms.get(it)?.volumePercent } ?: FULL_PERCENT
        play(alarmId, label, volume = percent / FULL_PERCENT.toFloat())
    }

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private var player: MediaPlayer? = null
    private var volumeHeld = false

    @Synchronized
    private fun play(alarmId: Long?, label: String, volume: Float) {
        val audio = context.getSystemService(AudioManager::class.java)
        if (audio?.getStreamVolume(AudioManager.STREAM_ALARM) == 0) diagnostics.record("chime_volume_zero", alarmId)
        vibrateTwice()
        release()
        volumeHeld = true
        volumeGuard.hold(alarmId)
        val uri = "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.w3_recheck}".toUri()
        val mp = MediaPlayer()
        player = try {
            mp.setAudioAttributes(attributes)
            mp.setDataSource(context, uri)
            mp.prepare()
            val played = RingMute.volume(volume)
            mp.setVolume(played, played)
            mp.setOnCompletionListener { done -> synchronized(this) { if (player === done) release() } }
            mp.start()
            diagnostics.record("chime", alarmId, label)
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
        const val FULL_PERCENT = 100
        const val NO_REPEAT = -1
        val TWICE = longArrayOf(0, 200, 150, 200)
    }
}
