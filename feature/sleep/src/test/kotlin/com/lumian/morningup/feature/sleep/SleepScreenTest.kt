package com.lumian.morningup.feature.sleep

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.SleepTracks
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class SleepScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(onOpen: (String) -> Unit = {}) {
        compose.setContent {
            MorningTheme(darkTheme = true, reduceMotion = true) { SleepScreen(onOpenTrack = onOpen) }
        }
    }

    @Test
    fun groups_useSituationNames_inTabsAndTitles() {
        show()
        // 결정 104: 상황별 이름. 위 탭과 묶음 제목이 같은 이름이에요.
        listOf("잠에 들기 어려울 때", "마음이 답답할 때", "생각이 복잡할 때", "아침 5분 명상").forEach { name ->
            assertThat(compose.onAllNodesWithText(name).fetchSemanticsNodes()).isNotEmpty()
        }
    }

    @Test
    fun tiles_areTwoColumnSquares_150_withGap12_at360() {
        show()
        val first = SleepTracks.groups[0].tracks
        val a = compose.onNodeWithTag("$SLEEP_TILE_TAG${first[0].id}").getUnclippedBoundsInRoot()
        val b = compose.onNodeWithTag("$SLEEP_TILE_TAG${first[1].id}").getUnclippedBoundsInRoot()
        val c = compose.onNodeWithTag("$SLEEP_TILE_TAG${first[2].id}").getUnclippedBoundsInRoot()
        assertThat(a.right - a.left).isEqualTo(150.dp)
        assertThat(a.bottom - a.top).isEqualTo(150.dp)
        assertThat(b.left - a.right).isEqualTo(12.dp)
        assertThat(b.top).isEqualTo(a.top)
        assertThat(c.top - a.bottom).isEqualTo(12.dp)
        assertThat(c.left).isEqualTo(a.left)
    }

    @Test
    fun tabTap_jumpsToGroup_andTileOpensTrack() {
        var opened: String? = null
        show { opened = it }
        val last = SleepTracks.groups.last()
        compose.onAllNodesWithText("아침 5분 명상")[0].performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("${SLEEP_GROUP_TAG}${SleepTracks.groups.lastIndex}").assertIsDisplayed()
        compose.onNodeWithTag("$SLEEP_TILE_TAG${last.tracks[0].id}").performClick()
        assertThat(opened).isEqualTo(last.tracks[0].id)
    }
}
