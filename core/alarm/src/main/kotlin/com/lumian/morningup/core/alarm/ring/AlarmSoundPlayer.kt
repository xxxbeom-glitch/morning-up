package com.lumian.morningup.core.alarm.ring

import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.net.toUri
import com.lumian.morningup.core.alarm.AlarmDiagnostics
import com.lumian.morningup.core.alarm.R
import com.lumian.morningup.core.model.engine.CallPolicy
import com.lumian.morningup.core.model.engine.SoundOutput
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 알람 소리와 진동. 알람에 고른 알람음([RingtoneFiles])으로 울리고, 모르는 소리면 앱 기본음(res/raw/alarm_default)이에요.
 * 고른 소리를 못 열면 앱 기본음, 그것도 못 열면 폰 기본 알람음으로 대신 울리고 기록을 남겨요.
 * 소리는 알람 소리 통로(USAGE_ALARM)로 내서 무음 모드와 미디어 볼륨에 상관없이 들려요.
 * 통화 중이면 [CallPolicy]대로 작은 소리와 진동으로 울리고, 통화가 끝나면 원래 크기로 돌아가요.
 */
@Singleton
class AlarmSoundPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val diagnostics: AlarmDiagnostics,
    private val calls: CallMonitor,
) {
    private var requested: SoundOutput? = null

    private var player: MediaPlayer? = null
    private var vibrating = false
    private var tone: ToneGenerator? = null

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    val isPlaying: Boolean get() = player != null || tone != null

    private var soundId: String? = null

    /** [volume]은 0~1. 이미 울리고 있으면 볼륨만 바꿔요. [soundId]는 알람에 고른 알람음이에요. */
    @Synchronized
    fun play(volume: Float, vibrate: Boolean, soundId: String? = null) {
        if (player == null) this.soundId = soundId
        if (requested == null) {
            calls.start { inCall ->
                diagnostics.record(if (inCall) "call_started" else "call_ended")
                reapply()
            }
        }
        requested = SoundOutput(volume.coerceIn(0f, 1f), vibrate)
        reapply()
    }

    @Synchronized
    private fun reapply() {
        val wanted = requested ?: return
        val out = CallPolicy.output(wanted, calls.inCall())
        val current = player ?: start()
        current?.setVolume(out.volume, out.volume)
        if (out.vibrate) startVibration() else stopVibration()
    }

    @Synchronized
    fun stop() {
        requested = null
        calls.stop()
        player?.run {
            runCatching { stop() }
            release()
        }
        player = null
        tone?.run {
            stopTone()
            release()
        }
        tone = null
        stopVibration()
    }

    /** 마지막 수단: 어떤 소리 파일도 못 열면 알람 소리 통로로 신호음을 내요. */
    private fun startTone() {
        if (tone != null) return
        tone = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME) }.getOrNull()
        tone?.startTone(ToneGenerator.TONE_SUP_RINGTONE)
    }

    private fun start(): MediaPlayer? {
        val defaultUri =
            "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.alarm_default}".toUri()
        val chosenUri = RingtoneFiles.uriFor(context, soundId)
        val chosen = if (chosenUri != defaultUri) {
            open(chosenUri) ?: run {
                diagnostics.record("sound_fallback", detail = "chosen sound failed")
                null
            }
        } else {
            null
        }
        val created = chosen ?: open(defaultUri) ?: run {
            diagnostics.record("sound_fallback", detail = "default tone failed")
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.let(::open)
        }
        if (created == null) {
            diagnostics.record("sound_failed")
            startTone()
        }
        player = created
        return created
    }

    private fun open(uri: Uri): MediaPlayer? {
        val mp = MediaPlayer()
        return try {
            mp.setAudioAttributes(attributes)
            mp.setDataSource(context, uri)
            mp.isLooping = true
            mp.prepare()
            mp.start()
            mp
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // IOException, IllegalState, IllegalArgument, Security(잠금 풀기 전 폰 기본음) 모두 여기로 와요.
            diagnostics.record("sound_open_failed", detail = e.javaClass.simpleName)
            mp.release()
            null
        }
    }

    private fun startVibration() {
        if (vibrating) return
        vibrating = true
        vibrator().vibrate(VibrationEffect.createWaveform(PATTERN, 0), attributes)
    }

    private fun stopVibration() {
        if (!vibrating) return
        vibrating = false
        vibrator().cancel()
    }

    @Suppress("DEPRECATION")
    private fun vibrator(): Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        context.getSystemService(Vibrator::class.java)
    }

    private companion object {
        /** 0.8초 떨고 0.6초 쉬기를 되풀이해요. */
        val PATTERN = longArrayOf(0, 800, 600)
    }
}
