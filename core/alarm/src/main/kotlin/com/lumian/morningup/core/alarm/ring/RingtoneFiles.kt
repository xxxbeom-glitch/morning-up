package com.lumian.morningup.core.alarm.ring

import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.RawRes
import androidx.core.net.toUri
import com.lumian.morningup.core.alarm.R
import com.lumian.morningup.core.model.Ringtones

/**
 * 알람음 id와 `res/raw` 소리 파일을 잇는 곳. 목록([Ringtones])에 소리를 더하면 여기에도 한 줄 더해요.
 * 모르는 id(지운 소리, '진동만' 등)는 앱 기본음으로 울려요.
 */
object RingtoneFiles {
    private val files: Map<String, Int> = mapOf(
        "morning" to R.raw.ringtone_morning,
        "morning_loop" to R.raw.ringtone_morning_loop,
        "love_serenade" to R.raw.ringtone_love_serenade,
        "spring_season" to R.raw.ringtone_spring_season,
        "good_morning" to R.raw.ringtone_good_morning,
    )

    @RawRes
    fun rawFor(id: String?): Int = id?.let(files::get) ?: R.raw.alarm_default

    fun uriFor(context: Context, id: String?): Uri =
        "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${rawFor(id)}".toUri()

    /** 목록의 모든 id에 소리 파일이 있는지(기본음 제외). 테스트에서 써요. */
    internal val ids: Set<String> get() = files.keys
}

/** S5에서 줄을 누르면 그 소리를 [PREVIEW_MILLIS] 동안 알람 소리 통로로 들려줘요. */
class RingtonePreviewer(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private val stopper = Runnable { stop() }

    fun preview(id: String) {
        stop()
        val mp = MediaPlayer()
        val started = runCatching {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            mp.setDataSource(context, RingtoneFiles.uriFor(context, id))
            mp.prepare()
            mp.start()
        }.isSuccess
        if (started) {
            player = mp
            handler.postDelayed(stopper, PREVIEW_MILLIS)
        } else {
            mp.release()
        }
    }

    fun stop() {
        handler.removeCallbacks(stopper)
        player?.run {
            runCatching { stop() }
            release()
        }
        player = null
    }

    private companion object {
        const val PREVIEW_MILLIS = 3_000L
    }
}
