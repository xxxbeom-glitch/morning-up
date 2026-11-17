package com.lumian.morningup.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningShadows
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.designsystem.theme.morningShadow

/** 카드형 설정 묶음(알람 편집, 설정 메인). 흰 카드, 안 여백 24, 제목 `Title/Medium`, 줄 사이 16. */
@Composable
fun MorningSettingsCard(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(MorningDimens.radiusCard)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .morningShadow(MorningShadows.card, shape)
            .background(MorningTheme.colors.surfaceCard, shape)
            // 제목 없는 카드는 위아래 16, 양옆 24예요(피그마 T1 카드, 줄 높이 44가 위아래 여백을 채워요).
            .padding(
                horizontal = MorningDimens.layoutCardPadding,
                vertical = if (title == null) MorningDimens.space16 else MorningDimens.layoutCardPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(MorningDimens.space16),
    ) {
        // 제목이 없는 카드(설정 T1, 결정 101)는 내용만 둬요.
        if (title != null) {
            Text(
                text = title,
                style = MorningTheme.typography.titleMedium,
                color = MorningTheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
        }
        content()
    }
}
