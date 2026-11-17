package com.lumian.morningup.core.designsystem.theme

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MorningFontScaleTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun bigSystemFont_isCappedAt130Percent_andFixedPartsStayAt100() {
        var themed = 0f
        var fixed = 0f
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f, 2f)) {
                MorningTheme(darkTheme = false, reduceMotion = true) {
                    themed = LocalDensity.current.fontScale
                    FixedFontScale { fixed = LocalDensity.current.fontScale }
                }
            }
        }
        compose.waitForIdle()
        assertThat(themed).isEqualTo(MAX_FONT_SCALE)
        assertThat(fixed).isEqualTo(1f)
    }

    @Test
    fun smallSystemFont_isKept() {
        var themed = 0f
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f, 0.85f)) {
                MorningTheme(darkTheme = false, reduceMotion = true) { themed = LocalDensity.current.fontScale }
            }
        }
        compose.waitForIdle()
        assertThat(themed).isEqualTo(0.85f)
    }
}
