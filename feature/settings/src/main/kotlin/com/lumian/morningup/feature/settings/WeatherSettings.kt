package com.lumian.morningup.feature.settings

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumian.morningup.core.alarm.PermissionSettings
import com.lumian.morningup.core.alarm.weather.WeatherRepository
import com.lumian.morningup.core.designsystem.component.MorningDialog
import com.lumian.morningup.core.designsystem.component.MorningSettingRow
import com.lumian.morningup.core.designsystem.component.MorningSettingsCard
import com.lumian.morningup.core.designsystem.component.SettingRowState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** T1 '날씨 애니메이션' 카드. 위치 권한과 '배경 애니메이션' 켜고 끄기를 날씨 저장소에 이어요. */
@HiltViewModel
class WeatherSettingsViewModel @Inject constructor(private val weather: WeatherRepository) : ViewModel() {
    val enabled: StateFlow<Boolean> = weather.enabled
    val locationGranted: StateFlow<Boolean> = weather.locationGranted

    fun setEnabled(on: Boolean) = weather.setEnabled(on)

    fun refresh() = weather.refresh(force = false)
}

/**
 * T1 날씨 애니메이션 카드(design-system T1 ②). '위치 권한' 줄(값 '허용됨', '꺼짐')과 '배경 애니메이션' 토글.
 * 권한 없이 토글을 켜거나 '위치 권한' 줄을 누르면 P5 창을 먼저 띄우고, '허용하기'에서 시스템 요청 창으로 물어요.
 * 거절되면(두 번 거절해 시스템이 더 묻지 않는 경우 포함) 앱 정보 화면으로 보내요. 허용돼 있으면 줄은 앱 정보 화면을 열어요.
 */
@Composable
internal fun WeatherCard(viewModel: WeatherSettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val enabled by viewModel.enabled.collectAsStateWithLifecycle()
    val granted by viewModel.locationGranted.collectAsStateWithLifecycle()
    var asking by remember { mutableStateOf(false) }
    var turnOnAfterGrant by remember { mutableStateOf(false) }
    val openAppDetails = {
        PermissionSettings.open(
            context,
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()),
        )
    }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        viewModel.refresh()
        if (ok && turnOnAfterGrant) viewModel.setEnabled(true)
        if (!ok) openAppDetails()
        turnOnAfterGrant = false
    }
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose {}
    }
    MorningSettingsCard(title = stringResource(R.string.settings_weather_card)) {
        NavRow(
            label = stringResource(R.string.settings_weather_location),
            value = stringResource(
                if (granted) R.string.permission_granted else R.string.settings_weather_location_off,
            ),
            onClick = { if (granted) openAppDetails() else asking = true },
        )
        MorningSettingRow(
            label = stringResource(R.string.settings_weather_motion),
            value = stringResource(R.string.settings_weather_motion_value),
            state = if (enabled && granted) SettingRowState.On else SettingRowState.Off,
            onCheckedChange = { on ->
                when {
                    !on -> viewModel.setEnabled(false)
                    granted -> viewModel.setEnabled(true)
                    else -> {
                        turnOnAfterGrant = true
                        asking = true
                    }
                }
            },
        )
    }
    if (asking) {
        MorningDialog(
            title = stringResource(R.string.ask_location),
            message = stringResource(R.string.ask_location_msg),
            dismissLabel = stringResource(R.string.ask_later),
            confirmLabel = stringResource(R.string.ask_allow),
            onDismiss = {
                asking = false
                turnOnAfterGrant = false
            },
            onConfirm = {
                asking = false
                request.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            },
        )
    }
}
