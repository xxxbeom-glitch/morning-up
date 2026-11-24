package com.lumian.morningup.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme

/**
 * 피그마 Preview Button(`297:7400`). 폭 꽉 참, 높이 36, 모서리 12, `primary/subtle` 칠,
 * `Body/Small Strong` `primary/text`. 편집 화면 '미리 해보기', 홈 카드 '이번만 쉬기'에 써요.
 */
@Composable
fun MorningPreviewButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MorningDimens.sizeChip)
            .morningClickable(onClick = onClick)
            .background(MorningTheme.colors.primarySubtle, RoundedCornerShape(MorningDimens.radiusChip)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MorningTheme.typography.bodySmallStrong, color = MorningTheme.colors.primaryText)
    }
}
