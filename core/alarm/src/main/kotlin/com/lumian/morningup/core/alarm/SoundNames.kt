package com.lumian.morningup.core.alarm

import androidx.annotation.StringRes
import com.lumian.morningup.core.model.Alarm

/**
 * 알람음, 잠들기 콘텐츠의 화면 이름 리소스(TSK-273). 목록 정의(`Ringtones`, `SleepTracks`)는 id와 key만 갖고,
 * 글자는 `res/values/sound_names.xml`에 모아 언어별로 바꿀 수 있게 해요.
 * 모르는 id는 기본 알람음 이름(소리) 또는 0(잠들기)을 돌려줘요.
 */
object SoundNames {
    private val ringtoneGroups = mapOf(
        "basic" to R.string.ringtone_group_basic,
        "calm" to R.string.ringtone_group_calm,
        "upbeat" to R.string.ringtone_group_upbeat,
        "loud" to R.string.ringtone_group_loud,
        "grand" to R.string.ringtone_group_grand,
        "mystic" to R.string.ringtone_group_mystic,
        "christmas" to R.string.ringtone_group_christmas,
    )
    private val ringtones = mapOf(
        "random" to R.string.ringtone_random,
        "default" to R.string.ringtone_default,
        "aube_paisible" to R.string.ringtone_aube_paisible,
        "douceur" to R.string.ringtone_douceur,
        "primavera" to R.string.ringtone_primavera,
        "petit_bois" to R.string.ringtone_petit_bois,
        "gouttes_de_bois" to R.string.ringtone_gouttes_de_bois,
        "bonjour_soleil" to R.string.ringtone_bonjour_soleil,
        "buongiorno" to R.string.ringtone_buongiorno,
        "joie_de_vivre" to R.string.ringtone_joie_de_vivre,
        "allegro_vivace" to R.string.ringtone_allegro_vivace,
        "presto" to R.string.ringtone_presto,
        "petits_rebonds" to R.string.ringtone_petits_rebonds,
        "route_de_campagne" to R.string.ringtone_route_de_campagne,
        "soleil_mineur" to R.string.ringtone_soleil_mineur,
        "fortissimo" to R.string.ringtone_fortissimo,
        "nuit_blanche" to R.string.ringtone_nuit_blanche,
        "tambour_battant" to R.string.ringtone_tambour_battant,
        "neon_rouge" to R.string.ringtone_neon_rouge,
        "maestoso" to R.string.ringtone_maestoso,
        "marche_triomphale" to R.string.ringtone_marche_triomphale,
        "grandeur" to R.string.ringtone_grandeur,
        "reverie" to R.string.ringtone_reverie,
        "danse_des_ombres" to R.string.ringtone_danse_des_ombres,
        "joyeux_noel" to R.string.ringtone_joyeux_noel,
        "nuit_de_noel" to R.string.ringtone_nuit_de_noel,
    )
    private val sleepGroups = mapOf(
        "hard_to_sleep" to R.string.sleep_group_hard_to_sleep,
        "heavy_heart" to R.string.sleep_group_heavy_heart,
        "busy_mind" to R.string.sleep_group_busy_mind,
        "morning_meditation" to R.string.sleep_group_morning_meditation,
    )
    private val sleepTracks = mapOf(
        "night_rain" to R.string.sleep_track_night_rain,
        "waves" to R.string.sleep_track_waves,
        "forest_birds" to R.string.sleep_track_forest_birds,
        "campfire" to R.string.sleep_track_campfire,
        "deep_night" to R.string.sleep_track_deep_night,
        "cloud_walk" to R.string.sleep_track_cloud_walk,
        "slow_breath" to R.string.sleep_track_slow_breath,
        "starry_field" to R.string.sleep_track_starry_field,
        "quiet_room" to R.string.sleep_track_quiet_room,
        "moon_walk" to R.string.sleep_track_moon_walk,
        "dawn_waltz" to R.string.sleep_track_dawn_waltz,
        "rain_sonata" to R.string.sleep_track_rain_sonata,
        "two_am" to R.string.sleep_track_two_am,
        "alley_cafe" to R.string.sleep_track_alley_cafe,
        "night_bus" to R.string.sleep_track_night_bus,
        "slow_afternoon" to R.string.sleep_track_slow_afternoon,
    )

    @StringRes
    fun ringtoneGroup(key: String): Int = ringtoneGroups.getValue(key)

    @StringRes
    fun ringtone(id: String): Int = ringtones[id] ?: ringtones.getValue(Alarm.DEFAULT_SOUND_ID)

    @StringRes
    fun sleepGroup(key: String): Int = sleepGroups.getValue(key)

    @StringRes
    fun sleepTrack(id: String): Int = sleepTracks.getValue(id)

    /** 시험용: 이름이 있는 id와 key 전부. */
    internal val ringtoneIds: Set<String> get() = ringtones.keys
    internal val ringtoneGroupKeys: Set<String> get() = ringtoneGroups.keys
    internal val sleepTrackIds: Set<String> get() = sleepTracks.keys
    internal val sleepGroupKeys: Set<String> get() = sleepGroups.keys
}
