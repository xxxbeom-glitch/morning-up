package com.lumian.morningup.feature.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lumian.morningup.core.alarm.ring.RingtonePreviewer
import com.lumian.morningup.core.designsystem.component.MorningRadioRow
import com.lumian.morningup.core.designsystem.component.MorningTopBar
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.RingtoneGroup
import com.lumian.morningup.core.model.Ringtones
import kotlinx.coroutines.launch

/**
 * S5 알람음(일반형, 피그마 `S5-2` `162:1494`). 위 언더라인 탭은 그 묶음 위치로 가는 바로가기이고,
 * 아래는 모든 소리를 묶음별로 이어 보여 줘요. 줄을 누르면 바로 고르고 3초 미리 들려줘요.
 * 완료 버튼 없이 뒤로 나가면 고른 값이 그대로 남아요.
 */
@Composable
fun SoundPage(
    selectedId: String,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    groups: List<RingtoneGroup> = Ringtones.groups,
) {
    val colors = MorningTheme.colors
    val context = LocalContext.current
    val previewer = remember(context) { RingtonePreviewer(context.applicationContext) }
    DisposableEffect(previewer) { onDispose { previewer.stop() } }
    val selected = Ringtones.find(selectedId).id
    val listState = rememberLazyListState()
    // 묶음 제목 줄의 목록 위치. 묶음마다 제목 1줄 + 소리 줄 수.
    val headerIndexes = remember(groups) {
        groups.runningFold(0) { index, group -> index + 1 + group.ringtones.size }.dropLast(1)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundBase)
            // 아래에 깔린 S0이 눌리지 않게 막아요.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .statusBarsPadding(),
    ) {
        MorningTopBar(
            title = stringResource(R.string.edit_sound),
            onBack = onBack,
            backContentDescription = stringResource(R.string.edit_back),
        )
        GroupTabs(groups = groups, headerIndexes = headerIndexes, listState = listState)
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = MorningDimens.space24,
                end = MorningDimens.space24,
                top = MorningDimens.space8,
                bottom = MorningDimens.space32,
            ),
        ) {
            groups.forEachIndexed { groupIndex, group ->
                item(key = "group-${group.name}") {
                    Text(
                        text = group.name,
                        style = MorningTheme.typography.bodySmallStrong,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(
                            top = if (groupIndex == 0) 0.dp else MorningDimens.space40,
                            bottom = MorningDimens.space4,
                        ),
                    )
                }
                group.ringtones.forEach { tone ->
                    item(key = tone.id) {
                        MorningRadioRow(
                            label = tone.name,
                            selected = tone.id == selected,
                            onClick = {
                                onSelect(tone.id)
                                previewer.preview(tone.id)
                            },
                            modifier = Modifier.height(MorningDimens.sizeRowPlain),
                        )
                    }
                }
            }
        }
    }
}

/** 언더라인 탭 줄. 누르면 그 묶음으로 스크롤하고, 지금 보이는 묶음의 탭이 켜져요. */
@Composable
private fun GroupTabs(groups: List<RingtoneGroup>, headerIndexes: List<Int>, listState: LazyListState) {
    val colors = MorningTheme.colors
    val scope = rememberCoroutineScope()
    val current by remember(headerIndexes) {
        derivedStateOf { headerIndexes.indexOfLast { it <= listState.firstVisibleItemIndex }.coerceAtLeast(0) }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = MorningDimens.space24),
        horizontalArrangement = Arrangement.spacedBy(MorningDimens.space24),
    ) {
        groups.forEachIndexed { index, group ->
            val on = index == current
            Column(
                modifier = Modifier
                    .semantics { selected = on }
                    .clickable(role = Role.Tab) { scope.launch { listState.animateScrollToItem(headerIndexes[index]) } }
                    .padding(top = MorningDimens.space8),
            ) {
                Text(
                    text = group.name,
                    style = if (on) MorningTheme.typography.bodyMediumStrong else MorningTheme.typography.bodyMedium,
                    color = if (on) colors.textPrimary else colors.textTertiary,
                )
                Spacer(Modifier.height(MorningDimens.space8))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(if (on) colors.textPrimary else colors.backgroundBase),
                )
            }
        }
    }
}
