package com.lumian.morningup.feature.edit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ImagePlus
import com.composables.icons.lucide.Lucide
import com.lumian.morningup.core.designsystem.component.MorningIconButton
import com.lumian.morningup.core.designsystem.component.MorningSelectMark
import com.lumian.morningup.core.designsystem.component.MorningTopBar
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.ui.AlarmBackground
import com.lumian.morningup.core.ui.AlarmBackgrounds
import com.lumian.morningup.core.ui.BackgroundGroup
import com.lumian.morningup.core.ui.R as UiR
import com.lumian.morningup.core.ui.rememberBackgroundPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val COLUMNS = 3

/** 피그마 S8 타일 96 x 168. */
private const val TILE_RATIO = 96f / 168f
private const val PHOTO_SCRIM = 0.25f
private val TileShape = RoundedCornerShape(16.dp)
private val MarkInset = 8.dp
private val UnderlineHeight = 2.dp

/**
 * S8 알람 화면 배경(피그마 `170:1853`). 위 언더라인 탭(하늘, 자연, 단색, 내 사진)은 그 묶음 위치로 가는 바로가기,
 * 아래는 3열 타일이에요. 타일 가운데 '오전 6:30' 미리보기, 고른 타일 오른쪽 위에 Select Mark.
 * 헤더 오른쪽 사진 버튼으로 갤러리 사진을 골라 '내 사진'에 더해요(앱 안으로 복사).
 */
@Composable
fun BackgroundPage(
    selectedId: String?,
    onBack: () -> Unit,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MorningTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    val photos by produceState(emptyList<String>(), refresh) {
        value = withContext(Dispatchers.IO) { AlarmBackgrounds.savedPhotos(context) }
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val id = withContext(Dispatchers.IO) { AlarmBackgrounds.importPhoto(context.applicationContext, uri) }
                if (id != null) {
                    refresh++
                    onSelect(id)
                }
            }
        }
    }
    val groups = BackgroundGroup.entries
    val named = remember { AlarmBackgrounds.all.groupBy { it.group } }
    // 묶음 제목 칸의 위치. 묶음마다 제목 1칸 + 타일 수(내 사진은 사진 수, 없으면 안내 1칸).
    val headerIndexes = groups.runningFold(0) { index, group ->
        val count = if (group == BackgroundGroup.Photo) photos.size.coerceAtLeast(1) else named[group].orEmpty().size
        index + 1 + count
    }.dropLast(1)
    val gridState = rememberLazyGridState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundBase)
            // 아래에 깔린 S0이 눌리지 않게 막아요.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .statusBarsPadding(),
    ) {
        Box(Modifier.fillMaxWidth()) {
            MorningTopBar(
                title = stringResource(R.string.edit_background),
                onBack = onBack,
                backContentDescription = stringResource(R.string.edit_back),
            )
            MorningIconButton(
                icon = Lucide.ImagePlus,
                contentDescription = stringResource(R.string.edit_background_add_photo),
                onClick = {
                    pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = MorningDimens.space4),
            )
        }
        BackgroundTabs(groups, headerIndexes, gridState)
        LazyVerticalGrid(
            columns = GridCells.Fixed(COLUMNS),
            state = gridState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = MorningDimens.layoutScreenMargin,
                end = MorningDimens.layoutScreenMargin,
                top = MorningDimens.space8,
                bottom = MorningDimens.space32,
            ),
            horizontalArrangement = Arrangement.spacedBy(MorningDimens.space12),
            verticalArrangement = Arrangement.spacedBy(MorningDimens.space12),
        ) {
            groups.forEachIndexed { groupIndex, group ->
                item(key = "group-$group", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(group.title),
                        style = MorningTheme.typography.bodySmallStrong,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = if (groupIndex == 0) 0.dp else MorningDimens.space24),
                    )
                }
                if (group == BackgroundGroup.Photo) {
                    if (photos.isEmpty()) {
                        item(key = "photo-empty", span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = stringResource(R.string.edit_background_photo_empty),
                                style = MorningTheme.typography.bodySmall,
                                color = colors.textTertiary,
                            )
                        }
                    }
                    photos.forEach { id ->
                        item(key = id) { PhotoTile(id, selected = selectedId == id, onClick = { onSelect(id) }) }
                    }
                } else {
                    named[group].orEmpty().forEach { bg ->
                        item(key = bg.id ?: "time-sky") {
                            NamedTile(bg, selected = selectedId == bg.id, onClick = { onSelect(bg.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NamedTile(bg: AlarmBackground, selected: Boolean, onClick: () -> Unit) {
    TileFrame(name = stringResource(bg.name), selected = selected, onClick = onClick) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(bg.top, bg.bottom))),
            contentAlignment = Alignment.Center,
        ) {
            PreviewClock(dark = bg.darkText)
        }
    }
}

@Composable
private fun PhotoTile(id: String, selected: Boolean, onClick: () -> Unit) {
    val photo = rememberBackgroundPhoto(id)
    TileFrame(name = stringResource(UiR.string.ui_bg_my_photo), selected = selected, onClick = onClick) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MorningTheme.colors.controlTrackOff),
            contentAlignment = Alignment.Center,
        ) {
            if (photo != null) {
                Image(photo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = PHOTO_SCRIM)),
                )
            }
            PreviewClock(dark = false)
        }
    }
}

/** 타일 틀. 이름은 화면 읽기에만 쓰고, 고르면 오른쪽 위에 Select Mark를 칠로 보여요(테두리 없음). */
@Composable
private fun TileFrame(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(TILE_RATIO)
            .clip(TileShape)
            .semantics { this.selected = selected }
            .clickable(role = Role.RadioButton, onClickLabel = name, onClick = onClick),
    ) {
        content()
        if (selected) {
            MorningSelectMark(
                selected = true,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(MarkInset),
            )
        }
    }
}

/** 타일 가운데 '오전 6:30'. 밝은 배경에는 진한 글자, 어두운 배경에는 흰 글자. */
@Composable
private fun PreviewClock(dark: Boolean) {
    Text(
        text = stringResource(R.string.edit_background_preview_time),
        style = MorningTheme.typography.bodyMediumStrong,
        color = if (dark) MorningTheme.colors.textPrimary else MorningTheme.colors.textInverse,
    )
}

/** 언더라인 탭 줄(S5와 같은 모양). 누르면 그 묶음으로 스크롤하고, 지금 보이는 묶음의 탭이 켜져요. */
@Composable
private fun BackgroundTabs(groups: List<BackgroundGroup>, headerIndexes: List<Int>, gridState: LazyGridState) {
    val colors = MorningTheme.colors
    val scope = rememberCoroutineScope()
    val current by remember(headerIndexes) {
        derivedStateOf { headerIndexes.indexOfLast { it <= gridState.firstVisibleItemIndex }.coerceAtLeast(0) }
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
                    .clickable(role = Role.Tab) { scope.launch { gridState.animateScrollToItem(headerIndexes[index]) } }
                    .padding(top = MorningDimens.space8),
            ) {
                Text(
                    text = stringResource(group.title),
                    style = if (on) MorningTheme.typography.bodyMediumStrong else MorningTheme.typography.bodyMedium,
                    color = if (on) colors.textPrimary else colors.textTertiary,
                )
                Spacer(Modifier.height(MorningDimens.space8))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UnderlineHeight)
                        .background(if (on) colors.textPrimary else colors.backgroundBase),
                )
            }
        }
    }
}
