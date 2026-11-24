package com.lumian.morningup.core.alarm.ring

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

/**
 * 버튼 속이기 접근성 자동 꺼짐 까닭(결정 146 2-4, ISS-041). 울림 화면, 미리 해보기, 설정 T3이 같이 써요.
 * - `talkback`: 터치 탐색이 켜져 있어요(톡백, 삼성 보이스 어시스턴트).
 * - `a11y_service`: 화면 조작을 대신하는 서비스가 켜져 있어요(스위치 제어, 음성 제어, 삼성 유니버설 스위치, 보조 메뉴).
 *   알림 읽기, 앱 자동화, 브라우저 정리처럼 터치 입력을 바꾸지 않는 접근성 앱은 세지 않아요.
 * - `no_animation`: 폰 설정 '애니메이션 삭제'(애니메이터 길이 배율 0)예요.
 */
object TrickAccessibility {
    const val TALKBACK = "talkback"
    const val A11Y_SERVICE = "a11y_service"
    const val NO_ANIMATION = "no_animation"

    /**
     * 화면 조작을 대신하는 서비스의 이름 조각. 서비스 이름(패키지/클래스)에 들어 있으면 맞다고 봐요.
     * - `switchaccess`: 구글 스위치 제어(com.google.android.marvin.talkback/com.android.switchaccess.SwitchAccessService)
     * - `voiceaccess`: 구글 Voice Access(com.google.android.apps.accessibility.voiceaccess)
     * - `universalswitch`: 삼성 유니버설 스위치(com.samsung.accessibility/.universalswitch...)
     * - `assistantmenu`: 삼성 보조 메뉴(com.samsung.accessibility/.assistantmenu...)
     * 안드로이드에는 '터치를 대신한다'를 알려 주는 공통 표시가 없어서(제스처 보내기 권한은 자동화 앱도 가져요) 이름으로 골라요.
     */
    private val TOUCH_REPLACING = listOf("switchaccess", "voiceaccess", "universalswitch", "assistantmenu")

    fun offReason(context: Context): String? {
        val manager = context.getSystemService(AccessibilityManager::class.java)
        if (manager?.isTouchExplorationEnabled == true) return TALKBACK
        val replacing = manager
            ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .orEmpty()
            .any { info ->
                val service = info.resolveInfo?.serviceInfo
                service?.packageName != context.packageName &&
                    replacesTouch(info.id ?: service?.let { "${it.packageName}/${it.name}" }, info.flags)
            }
        if (replacing) return A11Y_SERVICE
        val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        return if (scale == 0f) NO_ANIMATION else null
    }

    /** 서비스 하나가 터치 입력을 바꾸는지. 터치 탐색을 요청하거나 이름이 [TOUCH_REPLACING]에 들면 맞아요. */
    fun replacesTouch(serviceId: String?, flags: Int): Boolean {
        if (flags and AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE != 0) return true
        val id = serviceId?.lowercase() ?: return false
        return TOUCH_REPLACING.any { it in id }
    }
}
