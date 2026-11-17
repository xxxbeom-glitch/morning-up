package com.lumian.morningup.core.alarm.ring

import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.lumian.morningup.core.alarm.AlarmDiagnostics
import com.lumian.morningup.core.alarm.R
import com.lumian.morningup.core.model.VibrationPattern
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
    private var pattern: VibrationPattern = VibrationPattern.Basic

    /** S7 부드럽게 깨우기: 소리를 처음 켠 때와 끝까지 올리는 데 걸리는 시간. 0이면 바로 제 볼륨이에요. */
    private var rampStartedAt = 0L
    private var rampMillis = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val rampStep = object : Runnable {
        override fun run() {
            reapply()
            if (rampFactor() < 1f) handler.postDelayed(this, RAMP_STEP_MILLIS)
        }
    }

    /**
     * [volume]은 0~1. 이미 울리고 있으면 볼륨만 바꿔요. [soundId]는 알람에 고른 알람음, [pattern]은 진동 모양이에요.
     * [gentleSeconds]가 0보다 크면 처음 켤 때 그 시간 동안 작은 소리에서 [volume]까지 올려요(S7).
     */
    @Synchronized
    fun play(
        volume: Float,
        vibrate: Boolean,
        soundId: String? = null,
        pattern: VibrationPattern = VibrationPattern.Basic,
        gentleSeconds: Int = 0,
    ) {
        if (player == null && tone == null) {
            this.soundId = soundId
            this.pattern = pattern
            rampStartedAt = SystemClock.elapsedRealtime()
            rampMillis = gentleSeconds * MILLIS_PER_SECOND
            if (rampMillis > 0) handler.postDelayed(rampStep, RAMP_STEP_MILLIS)
        }
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
        val volume = RingMute.volume(out.volume * rampFactor())
        current?.setVolume(volume, volume)
        if (out.vibrate) startVibration() else stopVibration()
    }

    /** 부드럽게 깨우기에서 지금 올라간 정도(처음 5%에서 1까지). */
    private fun rampFactor(): Float {
        if (rampMillis <= 0) return 1f
        val done = (SystemClock.elapsedRealtime() - rampStartedAt).toFloat() / rampMillis
        return (RAMP_FLOOR + (1f - RAMP_FLOOR) * done).coerceIn(RAMP_FLOOR, 1f)
    }

    @Synchronized
    fun stop() {
        handler.removeCallbacks(rampStep)
        rampMillis = 0
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
        tone = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, toneVolume()) }.getOrNull()
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
        diagnostics.record("vibrate_on", detail = pattern.name)
        vibrateNow()
        if (!screenWatching) {
            screenWatching = true
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            ContextCompat.registerReceiver(context, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        }
    }

    private fun vibrateNow() {
        vibrator().vibrate(VibrationEffect.createWaveform(pattern.waveform, 0), attributes)
    }

    private fun stopVibration() {
        handler.removeCallbacks(revibrate)
        if (screenWatching) {
            screenWatching = false
            runCatching { context.unregisterReceiver(screenReceiver) }
        }
        if (!vibrating) return
        vibrating = false
        diagnostics.record("vibrate_off")
        vibrator().cancel()
    }

    /**
     * 안드로이드 14부터 화면이 꺼지면 그전에 시작한 진동을 알람 진동까지 멈춰요.
     * 화면이 꺼지거나 켜지면 잠시 뒤 진동을 다시 걸어서, 울리는 동안 진동이 끊기지 않게 해요.
     */
    private var screenWatching = false
    private val revibrate = Runnable {
        synchronized(this) {
            if (vibrating) {
                diagnostics.record("vibrate_again")
                vibrateNow()
            }
        }
    }
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val screen = if (intent.action == Intent.ACTION_SCREEN_OFF) "screen_off" else "screen_on"
            diagnostics.record(screen, detail = "while vibrating")
            handler.removeCallbacks(revibrate)
            handler.postDelayed(revibrate, REVIBRATE_DELAY_MILLIS)
        }
    }

    @Suppress("DEPRECATION")
    private fun toneVolume(): Int = (RingMute.volume(1f) * ToneGenerator.MAX_VOLUME).toInt()

    private fun vibrator(): Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        context.getSystemService(Vibrator::class.java)
    }

    private companion object {
        const val RAMP_STEP_MILLIS = 500L
        const val MILLIS_PER_SECOND = 1_000L
        const val RAMP_FLOOR = 0.05f
        const val REVIBRATE_DELAY_MILLIS = 300L
    }
}
