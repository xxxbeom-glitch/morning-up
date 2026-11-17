package com.lumian.morningup.core.model

import kotlinx.coroutines.flow.StateFlow

/** 홈 하늘(H0)과 해제 완료(W1) 한마디가 따르는 날씨. 비와 눈만 장면이 바뀌고 나머지는 맑음이에요. */
enum class WeatherKind {
    Clear,
    Rain,
    Snow,
    ;

    companion object {
        /**
         * Open-Meteo `weather_code`(WMO 기준)를 장면으로 바꿔요.
         * 이슬비, 비, 소나기, 뇌우는 비. 눈, 진눈깨비 눈, 눈 소나기는 눈. 흐림, 안개는 맑음 장면으로 둬요.
         */
        @Suppress("MagicNumber") // WMO 날씨 코드 표 그대로예요.
        fun fromWmo(code: Int): WeatherKind = when (code) {
            in 51..67, in 80..82, in 95..99 -> Rain
            in 71..77, 85, 86 -> Snow
            else -> Clear
        }
    }
}

/** 홈과 W1이 읽는 날씨. null이면 날씨 없이(맑은 하늘, 인사만) 보여요. */
interface WeatherSource {
    val weather: StateFlow<WeatherKind?>

    /** 앱이 앞으로 나올 때 불러요. 권한을 다시 읽고 날씨가 오래됐으면 새로 받아요. */
    fun refresh(force: Boolean = false)
}
