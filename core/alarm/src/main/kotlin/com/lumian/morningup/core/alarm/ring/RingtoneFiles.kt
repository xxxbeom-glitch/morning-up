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
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.Ringtones
import kotlin.random.Random

/**
 * 알람음 id와 `res/raw` 소리 파일을 잇는 곳. 목록([Ringtones])에 소리를 더하면 여기에도 한 줄 더해요.
 * 모르는 id(지운 소리, '진동만' 등)는 앱 기본음으로 울려요.
 * '랜덤'([Alarm.RANDOM_SOUND_ID])은 부를 때마다 벨소리 하나를 새로 골라요. 알람은 울리기 시작할 때 한 번 불러요.
 */
object RingtoneFiles {
    private val files: Map<String, Int> = mapOf(
        "aube_paisible" to R.raw.ringtone_aube_paisible,
        "douceur" to R.raw.ringtone_douceur,
        "primavera" to R.raw.ringtone_primavera,
        "petit_bois" to R.raw.ringtone_petit_bois,
        "gouttes_de_bois" to R.raw.ringtone_gouttes_de_bois,
        "bonjour_soleil" to R.raw.ringtone_bonjour_soleil,
        "buongiorno" to R.raw.ringtone_buongiorno,
        "joie_de_vivre" to R.raw.ringtone_joie_de_vivre,
        "allegro_vivace" to R.raw.ringtone_allegro_vivace,
        "presto" to R.raw.ringtone_presto,
        "petits_rebonds" to R.raw.ringtone_petits_rebonds,
        "route_de_campagne" to R.raw.ringtone_route_de_campagne,
        "soleil_mineur" to R.raw.ringtone_soleil_mineur,
        "fortissimo" to R.raw.ringtone_fortissimo,
        "nuit_blanche" to R.raw.ringtone_nuit_blanche,
        "tambour_battant" to R.raw.ringtone_tambour_battant,
        "neon_rouge" to R.raw.ringtone_neon_rouge,
        "maestoso" to R.raw.ringtone_maestoso,
        "marche_triomphale" to R.raw.ringtone_marche_triomphale,
        "grandeur" to R.raw.ringtone_grandeur,
        "reverie" to R.raw.ringtone_reverie,
        "danse_des_ombres" to R.raw.ringtone_danse_des_ombres,
        "joyeux_noel" to R.raw.ringtone_joyeux_noel,
        "nuit_de_noel" to R.raw.ringtone_nuit_de_noel,
    )

    @RawRes
    fun rawFor(id: String?, random: Random = Random.Default): Int = when (id) {
        Alarm.RANDOM_SOUND_ID -> files.getValue(Ringtones.randomPool.random(random))
        else -> id?.let { files[Ringtones.currentId(it)] } ?: R.raw.alarm_default
    }

    fun uriFor(context: Context, id: String?): Uri =
        "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${rawFor(id)}".toUri()

    /** 목록의 모든 id에 소리 파일이 있는지(기본음 제외). 테스트에서 써요. */
    internal val ids: Set<String> get() = files.keys
}

/**
 * S5에서 줄을 누르면 그 소리를 [PREVIEW_MILLIS](10초) 동안 알람 소리 통로로 들려줘요.
 * 크기는 그 알람의 볼륨(0~100)이에요(11-19 사용자 결정). '랜덤'은 누를 때마다 한 곡을 골라 들려줘요.
 */
class RingtonePreviewer(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private val stopper = Runnable { stop() }

    fun preview(id: String, volumePercent: Int = FULL_PERCENT) {
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
            val fraction = volumePercent.coerceIn(0, FULL_PERCENT) / FULL_PERCENT.toFloat()
            RingMute.volume(fraction).let { mp.setVolume(it, it) }
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
        const val PREVIEW_MILLIS = 10_000L
        const val FULL_PERCENT = 100
    }
}
