package com.lumian.morningup.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/** 모드와 상관없는 고정 색(design-system '예외'). 화면 코드에 원색을 직접 쓰지 않고 여기 것을 써요. */
object MorningFixedColors {
    /** 시트, 다이얼로그 뒤 막: #14202E 40%. */
    val Scrim = Color(0x6614202E)

    /** W1 해제 완료 뒤 딤: 아침 검정 45%. */
    val WakeDimMorning = Color(0x73000000)

    /** W1 해제 완료 뒤 딤: 밤 검정 60%. */
    val WakeDimNight = Color(0x99000000)

    /**
     * 미션 칸 삭제 배지(`remove-badge/fill`): #0B0F16 85%. 라이트, 다크 모두 같은 진한 원에 흰 X예요.
     * 전에는 `text/body`라 다크에서 연한 회색 원에 흰 X가 되어 안 보였어요(11-19 사용자 지적 6).
     */
    val RemoveBadgeFill = Color(0xD90B0F16)

    /** 미션 칸 삭제 배지 둘레(`remove-badge/ring`): 흰색 90%, 1.5. 진한 그림 위에서도 원 모양이 보이게 해요. */
    val RemoveBadgeRing = Color(0xE6FFFFFF)

    /**
     * 빨간 버튼(Danger) 글자(`danger/on`): 라이트, 다크 모두 흰색. 전에는 `text/inverse`라 다크에서 진한 #0B0F16 글자였어요
     * (11-19 사용자 지적 4). 라이트는 원래 흰색이라 그대로예요.
     */
    val DangerOn = Color(0xFFFFFFFF)
}
