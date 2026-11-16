package com.lumian.morningup.core.alarm.ring

import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.net.toUri
import com.lumian.morningup.core.alarm.AlarmDiagnostics
import com.lumian.morningup.core.alarm.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 알람 소리와 진동. 알람음 목록은 아직 목업이라 어떤 항목을 골라도 앱 기본음(res/raw/alarm_default)으로 울려요.
 * 기본음을 못 열면 폰 기본 알람음으로 대신 울리고 기록을 남겨요.
 * 소리는 알람 소리 통로(USAGE_ALARM)로 내서 무음 모드와 미디어 볼륨에 상관없이 들려요.
 */
@Singleton
class AlarmSoundPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val diagnostics: AlarmDiagnostics,
) {
    private var player: MediaPlayer? = null
    private var vibrating = false

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    val isPlaying: Boolean get() = player != null

    /** [volume]은 0~1. 이미 울리고 있으면 볼륨만 바꿔요. */
    fun play(volume: Float, vibrate: Boolean) {
        val v = volume.coerceIn(0f, 1f)
        val current = player ?: start()
        current?.setVolume(v, v)
        if (vibrate) startVibration() else stopVibration()
    }

    fun stop() {
        player?.run {
            runCatching { stop() }
            release()
        }
        player = null
        stopVibration()
    }

    private fun start(): MediaPlayer? {
        val defaultUri =
            "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.alarm_default}".toUri()
        val created = open(defaultUri) ?: run {
            diagnostics.record("sound_fallback", detail = "default tone failed")
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.let(::open)
        }
        if (created == null) diagnostics.record("sound_failed")
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
        } catch (e: IOException) {
            diagnostics.record("sound_open_failed", detail = e.message.orEmpty())
            mp.release()
            null
        } catch (e: IllegalStateException) {
            diagnostics.record("sound_open_failed", detail = e.message.orEmpty())
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
