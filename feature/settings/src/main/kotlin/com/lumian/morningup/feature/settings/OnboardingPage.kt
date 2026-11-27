package com.lumian.morningup.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lumian.morningup.core.designsystem.theme.MorningTheme

/** 움직임 줄이기를 켠 폰의 스플래시 시간(다 쓴 글자만 보여 줘요). */
private const val SPLASH_REDUCED_MILLIS = 1_200L

/** 스플래시를 보여 주는 시간. 손글씨를 다 쓰고 0.5초 멈춘 뒤 넘어가요(결정 170). 움직임 줄이기면 짧게요. */
@Composable
fun splashMillis(): Long = if (MorningTheme.reduceMotion) {
    SPLASH_REDUCED_MILLIS
} else {
    SplashHandwriting.TOTAL_MILLIS.toLong()
}

/**
 * O1 스플래시(결정 131, 170). 아이콘과 같은 어두운 바탕에 아이콘 손글씨 '일어나'를 한 획씩 졸린 속도로 써요.
 * 라이트, 다크 모두 아이콘 색 그대로예요. 끝나면 첫 알람 만들기(O4-1)로 가요(결정 191).
 */
@Composable
fun OnboardingSplash(modifier: Modifier = Modifier) {
    val name = stringResource(R.string.onboarding_splash_desc)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashBackground),
        contentAlignment = Alignment.Center,
    ) {
        SplashHandwritingLogo(
            Modifier
                .size(SplashLogoSize)
                .semantics { contentDescription = name },
        )
    }
}

/** 손글씨 칸. 아이콘 viewport 540을 이 크기에 맞춰서 글자 폭은 약 170dp예요. */
private val SplashLogoSize = 320.dp
