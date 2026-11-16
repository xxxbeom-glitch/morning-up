package com.lumian.morningup.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MorningColorsTest {
    @Test
    fun light_usesFigmaLightValues() {
        val colors = lightMorningColors()
        assertThat(colors.backgroundBase).isEqualTo(Color(0xFFF2F4F9))
        assertThat(colors.primaryDefault).isEqualTo(Color(0xFF0B6CF0))
        assertThat(colors.isDark).isFalse()
    }

    @Test
    fun dark_usesFigmaDarkValues() {
        val colors = darkMorningColors()
        assertThat(colors.backgroundBase).isEqualTo(Color(0xFF0B0F16))
        assertThat(colors.primaryDefault).isEqualTo(Color(0xFF8AB8FB))
        assertThat(colors.isDark).isTrue()
    }

    @Test
    fun dimens_followFigma() {
        assertThat(MorningDimens.sizeSwitchW.value).isEqualTo(48f)
        assertThat(MorningDimens.radiusFull.value).isEqualTo(999f)
    }
}
