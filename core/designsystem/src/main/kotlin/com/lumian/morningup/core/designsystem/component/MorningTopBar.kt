package com.lumian.morningup.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme

private val TopBarHeight = 56.dp

/** 상태 표시줄과 상단바 사이 여백(11-18 사용자 요청: 홈 말고 모든 화면 위 여백을 늘림). */
private val TopBarTopGap = 8.dp

/** 아이콘 버튼(48) 안에서 아이콘(24)이 화면 왼쪽 16에 오도록 버튼을 4 들여요. */
private val BackButtonInset = 4.dp

/** 피그마 Top Bar(Type: Back, Title Only). 높이 56, 제목은 가운데 `Title/Medium`. */
@Composable
fun MorningTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backContentDescription: String = "",
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = TopBarTopGap)
            .height(TopBarHeight),
    ) {
        if (onBack != null) {
            MorningIconButton(
                icon = Lucide.ChevronLeft,
                contentDescription = backContentDescription,
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = BackButtonInset),
            )
        }
        Text(
            text = title,
            style = MorningTheme.typography.titleMedium,
            color = MorningTheme.colors.textPrimary,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = MorningDimens.space48)
                .semantics { heading() },
        )
    }
}
