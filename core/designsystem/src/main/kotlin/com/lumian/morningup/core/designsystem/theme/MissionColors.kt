package com.lumian.morningup.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * 미션 플레이 화면(M) 색 묶음 `mission/…`. 미션 화면만 컬러가이드(p8) 밖의 색을 써요(design-system '미션 플레이 화면 (M)').
 * 바탕은 Z1 잠들기와 같은 무드(어두운 위아래 그라데이션 + 빛 덩어리 3개)이고 미션마다 색만 달라요.
 * 요소는 테두리, 그림자 없이 흰색 칠의 진하기(5%, 6%, 10%, 100%)로만 나눠요. 다크 모드와 상관없이 같아요.
 */
object MissionColors {
    /** 미션 하나의 바탕. [top] → [bottom] 그라데이션 위에 빛 덩어리 [glows]. */
    data class Backdrop(val top: Color, val bottom: Color, val glows: List<Color>)

    /** M1 Drop Words 청록. */
    val DropWords =
        Backdrop(Color(0xFF071619), Color(0xFF090E10), listOf(Color(0xFF0F4F4C), Color(0xFF173F5C), Color(0xFF1C3A2A)))

    /** M2 Pop Quiz 와인. */
    val PopQuiz =
        Backdrop(Color(0xFF180A12), Color(0xFF100A0D), listOf(Color(0xFF5A1C38), Color(0xFF6A2A48), Color(0xFF3A1C2C)))

    /** M3 Quick Math 코발트. */
    val QuickMath =
        Backdrop(Color(0xFF081022), Color(0xFF0A0D15), listOf(Color(0xFF173A80), Color(0xFF1A4C6E), Color(0xFF202C5C)))

    /** M4 Shake Shake 호박. */
    val ShakeShake =
        Backdrop(Color(0xFF170F07), Color(0xFF0F0C0A), listOf(Color(0xFF5C3414), Color(0xFF6A3420), Color(0xFF3A2A14)))

    /** 빛 덩어리 투명도(Z1과 같음: 0.55, 0.38, 0.35). */
    val GlowAlphas = listOf(0.55f, 0.38f, 0.35f)

    val Text = Color.White

    /** 위 줄 상태 글자 흰색 70%. */
    val TextStatus = Color.White.copy(alpha = 0.7f)

    /** 분야, 동작 안내 흰색 60%. */
    val TextGuide = Color.White.copy(alpha = 0.6f)

    /** 보조 안내('틀리면 새 문제로 처음부터 해요') 흰색 50%. */
    val TextHint = Color.White.copy(alpha = 0.5f)

    /** 타이머 바탕 고리 흰색 18%. */
    val TimerTrack = Color.White.copy(alpha = 0.18f)

    val TimerFill = Color.White

    /** M4 큰 시간 고리 바탕 흰색 15%. */
    val RingTrack = Color.White.copy(alpha = 0.15f)

    /** 키, 답 줄, 입력칸 칠 흰색 10%. */
    val Fill = Color.White.copy(alpha = 0.10f)

    /** 지우기 키 흰색 6%. */
    val FillWeak = Color.White.copy(alpha = 0.06f)

    /** 단어가 떨어지는 칸 흰색 5%. */
    val FillFaint = Color.White.copy(alpha = 0.05f)

    /** 누른 키(10%보다 한 단계 진하게). */
    val FillPressed = Color.White.copy(alpha = 0.18f)

    /** 확인 키 흰 칠 위의 어두운 아이콘. */
    val OnConfirm = Color(0xFF0A0D15)

    /** 정답 표시(`status/success` #00C281 10% 칠, dev-plan 정답 움직임). 미션 화면은 모드와 상관없어 값으로 둬요. */
    val Success = Color(0xFF00C281).copy(alpha = 0.10f)
}
