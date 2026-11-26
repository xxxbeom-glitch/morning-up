package com.lumian.morningup.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lumian.morningup.core.designsystem.component.MorningButton
import com.lumian.morningup.core.designsystem.component.MorningSheetHeader
import com.lumian.morningup.core.designsystem.theme.MorningDimens

/** 시트 판(좌우 24) 밖으로 8씩 넓혀 피그마 footer(좌우 16)와 맞춰요(편집 시트와 같음). */
internal val FooterBleed = 8.dp

/** 설정 시트 공통 틀: 머리, 내용(머리 아래 16), 아래 '완료' 버튼(위 24). 편집 시트와 같아요. */
@Composable
internal fun SheetFrame(
    title: String,
    description: String,
    onDone: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    MorningSheetHeader(
        title = title,
        description = description,
        modifier = Modifier.padding(top = MorningDimens.space8),
    )
    Spacer(Modifier.height(MorningDimens.space16))
    Column(verticalArrangement = Arrangement.spacedBy(MorningDimens.space4), content = content)
    Spacer(Modifier.height(MorningDimens.space24))
    MorningButton(
        text = stringResource(R.string.pl_done),
        onClick = onDone,
        modifier = Modifier.fillMaxWidth().bleed(FooterBleed).testTag(SHEET_DONE_TAG),
    )
}

const val SHEET_DONE_TAG = "settings_sheet_done"

internal fun Modifier.bleed(horizontal: Dp): Modifier = layout { measurable, constraints ->
    val extra = horizontal.roundToPx() * 2
    val placeable = measurable.measure(
        constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra),
    )
    layout(placeable.width - extra, placeable.height) { placeable.place(-extra / 2, 0) }
}
