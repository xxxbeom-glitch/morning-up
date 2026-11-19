package com.lumian.morningup.feature.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.RingtoneGroup
import com.lumian.morningup.core.model.Ringtones
import kotlinx.coroutines.launch

/**
 * S5 알람음(일반형, 피그마 `S5-2` `162:1494`). 위 언더라인 탭은 그 묶음 위치로 가는 바로가기이고,
 * 아래는 모든 소리를 묶음별로 이어 보여 줘요. 줄을 누르면 바로 고르고 6초 미리 들려줘요.
 * 완료 버튼 없이 뒤로 나가면 고른 값이 그대로 남아요.
 */
@Composable
fun SoundPage(
    selectedId: String,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onOff: () -> Unit,
    modifier: Modifier = Modifier,
    groups: List<RingtoneGroup> = Ringtones.groups,
) {
    val colors = MorningTheme.colors
    val context = LocalContext.current
    val previewer = remember(context) { RingtonePreviewer(context.applicationContext) }
    DisposableEffect(previewer) { onDispose { previewer.stop() } }
    // 알람음을 끈 알람(진동만)은 맨 위 '사용 안 함'만 고른 모양이에요.
    val off = selectedId == Alarm.SILENT_SOUND_ID
    val selected = if (off) null else Ringtones.find(selectedId).id
    val listState = rememberLazyListState()
    val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // 묶음 제목 줄의 목록 위치. 맨 위 '사용 안 함' 1줄 뒤로 묶음마다 제목 1줄 + 소리 줄 수.
    val headerIndexes = remember(groups) {
        groups.runningFold(1) { index, group -> index + 1 + group.ringtones.size }.dropLast(1)
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
                // 피그마: 탭 줄 아래 8 + 목록 위 24.
                top = MorningDimens.space32,
                // 앱이 화면 끝까지 그려서, 아래 내비게이션 줄 높이만큼 더 띄워야 마지막 줄이 가려지지 않아요.
                bottom = MorningDimens.space32 + navBar,
            ),
        ) {
            item(key = "off") {
                MorningRadioRow(
                    label = stringResource(R.string.edit_off_value),
                    selected = off,
                    onClick = {
                        previewer.stop()
                        onOff()
                    },
                    modifier = Modifier.padding(bottom = MorningDimens.space24),
                )
            }
            groups.forEachIndexed { groupIndex, group ->
                item(key = "group-${group.name}") {
                    Text(
                        text = group.name,
                        style = MorningTheme.typography.bodySmallStrong,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(
                            top = if (groupIndex == 0) 0.dp else MorningDimens.space40,
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
    // 탭을 누른 묶음. 끝 쪽 묶음은 맨 위까지 못 올라가서, 손으로 넘기기 전까지는 누른 탭을 켜 둬요.
    var tapped by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { if (it is DragInteraction.Start) tapped = null }
    }
    val current by remember(headerIndexes) {
        derivedStateOf {
            val atBottom = !listState.canScrollForward && listState.canScrollBackward
            tapped ?: if (atBottom) {
                // 맨 아래에 닿으면 마지막 묶음 탭을 켜요.
                headerIndexes.lastIndex
            } else {
                headerIndexes.indexOfLast { it <= listState.firstVisibleItemIndex }.coerceAtLeast(0)
            }
        }
    }
    // 피그마 S5-2 탭 줄: 칸 높이 48(위 14, 글자 20, 사이 12, 밑줄 2), 칸 안 좌우 4, 칸 사이 24, 아래 1 구분선.
    Column(Modifier.fillMaxWidth()) {
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
                        .width(IntrinsicSize.Max)
                        .semantics { selected = on }
                        .clickable(role = Role.Tab) {
                            tapped = index
                            scope.launch { listState.animateScrollToItem(headerIndexes[index]) }
                        }
                        .padding(start = TabSidePadding, end = TabSidePadding, top = TabTopPadding),
                ) {
                    Text(
                        text = group.name,
                        style = with(MorningTheme.typography) { if (on) bodyMediumStrong else bodyMedium },
                        color = if (on) colors.textPrimary else colors.textSecondary,
                    )
                    Spacer(Modifier.height(MorningDimens.space12))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(TabUnderline)
                            .background(
                                color = if (on) colors.primaryDefault else Color.Transparent,
                                shape = RoundedCornerShape(topStart = 1.dp, topEnd = 1.dp),
                            ),
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.borderDefault),
        )
    }
}

private val TabSidePadding = 4.dp
private val TabTopPadding = 14.dp
private val TabUnderline = 2.dp
