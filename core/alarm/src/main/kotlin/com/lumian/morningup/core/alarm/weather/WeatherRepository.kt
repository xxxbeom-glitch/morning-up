package com.lumian.morningup.core.alarm.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.lumian.morningup.core.alarm.AlarmScope
import com.lumian.morningup.core.model.WeatherKind
import com.lumian.morningup.core.model.WeatherSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.roundToLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import timber.log.Timber

/**
 * 날씨(F2, 9단계). 대략적인 위치를 약 5km 칸(0.05도)으로 반올림해 Open-Meteo에 보내고, 받은 날씨 장면만 저장해요.
 * 좌표는 저장하지 않아요. '배경 애니메이션'(T1)이 꺼졌거나 위치 권한이 없거나 못 불러왔으면 [weather]는 null이고,
 * 홈은 시간대만 따르는 맑은 하늘, W1은 짧은 인사만 보여요.
 * 잠금을 풀기 전 울림 화면(W1)에서도 읽히게 기기 보호 저장소에 둬요.
 */
@Singleton
class WeatherRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @AlarmScope private val scope: CoroutineScope,
) : WeatherSource {
    private val prefs = context.createDeviceProtectedStorageContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val enabledState = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, true))
    private val permissionState = MutableStateFlow(hasLocationPermission())
    private val cached = MutableStateFlow(readCached())
    private var fetching: Job? = null

    /** T1 '배경 애니메이션'. 처음 값은 켜짐이지만 위치 권한이 있어야 날씨가 보여요. */
    val enabled: StateFlow<Boolean> = enabledState.asStateFlow()

    /** T1 '위치 권한' 줄 값. */
    val locationGranted: StateFlow<Boolean> = permissionState.asStateFlow()

    /** 지금 보여 줄 날씨. null이면 날씨 없이(맑은 하늘, 인사만). */
    override val weather: StateFlow<WeatherKind?> = combine(enabledState, permissionState, cached) {
            on,
            granted,
            saved,
        ->
        saved?.takeIf { on && granted && System.currentTimeMillis() - it.second < SHOW_FOR_MILLIS }?.first
    }.stateIn(scope, SharingStarted.Eagerly, null)

    fun setEnabled(on: Boolean) {
        prefs.edit { putBoolean(KEY_ENABLED, on) }
        enabledState.value = on
        if (on) refresh(force = true)
    }

    /** 앱이 앞으로 나올 때, 권한 창이 닫혔을 때 불러요. 권한을 다시 읽고, 날씨가 오래됐으면 새로 받아요. */
    override fun refresh(force: Boolean) {
        permissionState.value = hasLocationPermission()
        if (!enabledState.value || !permissionState.value) return
        val saved = cached.value
        if (!force && saved != null && System.currentTimeMillis() - saved.second < REFRESH_AFTER_MILLIS) return
        if (fetching?.isActive == true) return
        fetching = scope.launch {
            val kind = runCatching { fetch() }.onFailure { Timber.w(it, "weather fetch failed") }.getOrNull()
            if (kind != null) save(kind)
        }
    }

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private suspend fun fetch(): WeatherKind? {
        val location = lastLocation() ?: return null
        val url = String.format(
            Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f&current=weather_code",
            location.latitude.toCell(),
            location.longitude.toCell(),
        )
        return withContext(Dispatchers.IO) {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = TIMEOUT_MILLIS
                connection.readTimeout = TIMEOUT_MILLIS
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                WeatherKind.fromWmo(JSONObject(body).getJSONObject("current").getInt("weather_code"))
            } finally {
                connection.disconnect()
            }
        }
    }

    /** 가장 최근에 알던 대략적인 위치. 없으면(안드로이드 11부터) 지금 위치를 한 번 물어요. */
    @SuppressLint("MissingPermission") // refresh에서 권한을 확인한 뒤에만 불려요.
    private suspend fun lastLocation(): Location? {
        val manager = context.getSystemService(LocationManager::class.java)
        if (manager == null || !hasLocationPermission()) return null
        val known = manager.getProviders(true)
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        val canAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return known ?: if (canAsk) currentLocation(manager) else null
    }

    @SuppressLint("MissingPermission", "NewApi") // lastLocation에서 권한과 안드로이드 11 이상을 확인해요.
    private suspend fun currentLocation(manager: LocationManager): Location? {
        return withTimeoutOrNull(TIMEOUT_MILLIS.toLong()) {
            suspendCancellableCoroutine { cont ->
                manager.getCurrentLocation(LocationManager.NETWORK_PROVIDER, null, context.mainExecutor) {
                    if (cont.isActive) cont.resume(it)
                }
            }
        }
    }

    private fun save(kind: WeatherKind) {
        val at = System.currentTimeMillis()
        prefs.edit {
            putString(KEY_KIND, kind.name)
            putLong(KEY_AT, at)
        }
        cached.value = kind to at
    }

    private fun readCached(): Pair<WeatherKind, Long>? {
        val name = prefs.getString(KEY_KIND, null) ?: return null
        val kind = WeatherKind.entries.firstOrNull { it.name == name } ?: return null
        return kind to prefs.getLong(KEY_AT, 0L)
    }

    /** 약 5km 칸(0.05도)으로 반올림해요. 정확한 좌표는 폰 밖으로 나가지 않아요. */
    private fun Double.toCell(): Double = (this * CELLS_PER_DEGREE).roundToLong() / CELLS_PER_DEGREE

    private companion object {
        const val PREFS = "weather"
        const val KEY_ENABLED = "enabled"
        const val KEY_KIND = "kind"
        const val KEY_AT = "at"
        const val CELLS_PER_DEGREE = 20.0
        const val TIMEOUT_MILLIS = 10_000
        const val REFRESH_AFTER_MILLIS = 30 * 60_000L
        const val SHOW_FOR_MILLIS = 6 * 60 * 60_000L
    }
}
