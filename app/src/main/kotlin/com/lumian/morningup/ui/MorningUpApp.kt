package com.lumian.morningup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.composables.icons.lucide.AlarmClock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Moon
import com.composables.icons.lucide.Settings
import com.lumian.morningup.R
import com.lumian.morningup.core.designsystem.component.MorningBottomNav
import com.lumian.morningup.core.designsystem.component.MorningNavItem
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme

/** 앱의 세 탭. 순서가 하단 메뉴 순서예요. */
enum class TopTab { Alarm, Sleep, Settings }

/**
 * 앱 껍데기: 탭 내용 + 아래 가운데 떠 있는 하단 메뉴. [alarmTab]은 알람 탭(홈), [settingsTab]은 설정 탭(T1)이에요.
 */
@Composable
fun MorningUpApp(
    modifier: Modifier = Modifier,
    alarmTab: @Composable (Modifier) -> Unit = { Placeholder(stringResource(R.string.tab_alarm)) },
    settingsTab: @Composable (Modifier) -> Unit = { Placeholder(stringResource(R.string.tab_settings)) },
) {
    var tab by rememberSaveable { mutableStateOf(TopTab.Alarm) }
    val labels = listOf(
        stringResource(R.string.tab_alarm),
        stringResource(R.string.tab_sleep),
        stringResource(R.string.tab_settings),
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MorningTheme.colors.backgroundBase),
    ) {
        when {
            tab == TopTab.Alarm -> alarmTab(Modifier.fillMaxSize())
            tab == TopTab.Settings -> settingsTab(Modifier.fillMaxSize())
            else -> Placeholder(labels[tab.ordinal])
        }
        MorningBottomNav(
            items = listOf(
                MorningNavItem(Lucide.AlarmClock, labels[0]),
                MorningNavItem(Lucide.Moon, labels[1]),
                MorningNavItem(Lucide.Settings, labels[2]),
            ),
            selectedIndex = tab.ordinal,
            onSelect = { tab = TopTab.entries[it] },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = MorningDimens.space16),
        )
    }
}

@Composable
private fun Placeholder(label: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.tab_placeholder, label),
            style = MorningTheme.typography.bodyMedium,
            color = MorningTheme.colors.textTertiary,
        )
    }
}
