package com.lumian.morningup.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningShadows
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.designsystem.theme.morningShadow

private val HandleWidth = 36.dp
private val HandleHeight = 4.dp

/**
 * 바텀시트 몸체. 위 모서리만 둥근(`radius/card`) `background/elevated` 판, 맨 위 손잡이,
 * 안은 일반형 규칙(좌우 24). 띄우고 끌어 내리는 동작은 쓰는 화면이 정해요(뒤 막은 `MorningFixedColors.Scrim`).
 */
@Composable
fun MorningSheetSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(topStart = MorningDimens.radiusCard, topEnd = MorningDimens.radiusCard)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .morningShadow(MorningShadows.float, shape)
            .background(MorningTheme.colors.backgroundElevated, shape)
            .navigationBarsPadding()
            .padding(horizontal = MorningDimens.space24),
    ) {
        Box(
            Modifier
                .padding(vertical = MorningDimens.space12)
                .size(HandleWidth, HandleHeight)
                .background(MorningTheme.colors.controlTrackOff, RoundedCornerShape(MorningDimens.radiusFull))
                .align(Alignment.CenterHorizontally),
        )
        content()
        Spacer(Modifier.height(MorningDimens.space32))
    }
}
