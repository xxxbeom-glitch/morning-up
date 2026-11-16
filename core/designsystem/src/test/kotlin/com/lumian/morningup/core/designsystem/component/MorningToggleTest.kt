package com.lumian.morningup.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MorningToggleTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun click_turnsOn_thenOff() {
        val changes = mutableListOf<Boolean>()
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                var checked by remember { mutableStateOf(false) }
                MorningToggle(
                    checked = checked,
                    onCheckedChange = {
                        changes += it
                        checked = it
                    },
                    modifier = Modifier.testTag("toggle"),
                )
            }
        }
        compose.onNodeWithTag("toggle").assertIsOff().performClick().assertIsOn().performClick().assertIsOff()
        assertThat(changes).containsExactly(true, false).inOrder()
    }

    @Test
    fun disabled_ignoresClick() {
        var called = false
        compose.setContent {
            MorningTheme(darkTheme = true, reduceMotion = false) {
                MorningToggle(
                    checked = false,
                    onCheckedChange = { called = true },
                    enabled = false,
                    modifier = Modifier.testTag("toggle"),
                )
            }
        }
        compose.onNodeWithTag("toggle").performClick()
        assertThat(called).isFalse()
    }
}
