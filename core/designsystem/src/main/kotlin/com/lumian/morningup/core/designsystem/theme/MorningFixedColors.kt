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
}
