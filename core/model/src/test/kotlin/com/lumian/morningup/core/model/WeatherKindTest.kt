package com.lumian.morningup.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherKindTest {
    @Test
    fun fromWmo_mapsRainSnowAndClear() {
        assertEquals(WeatherKind.Clear, WeatherKind.fromWmo(0))
        assertEquals(WeatherKind.Clear, WeatherKind.fromWmo(3))
        assertEquals(WeatherKind.Clear, WeatherKind.fromWmo(45))
        assertEquals(WeatherKind.Rain, WeatherKind.fromWmo(51))
        assertEquals(WeatherKind.Rain, WeatherKind.fromWmo(63))
        assertEquals(WeatherKind.Rain, WeatherKind.fromWmo(81))
        assertEquals(WeatherKind.Rain, WeatherKind.fromWmo(95))
        assertEquals(WeatherKind.Snow, WeatherKind.fromWmo(71))
        assertEquals(WeatherKind.Snow, WeatherKind.fromWmo(77))
        assertEquals(WeatherKind.Snow, WeatherKind.fromWmo(86))
    }
}
