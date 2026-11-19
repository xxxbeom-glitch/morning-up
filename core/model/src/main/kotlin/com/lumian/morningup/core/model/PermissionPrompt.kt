package com.lumian.morningup.core.model

/**
 * 홈 H2 권한 경고 배너(결정 113, 119, 120). 꺼진 권한이 여러 개여도 급한 것 하나만 보여요.
 * 순서: [NotificationsAndOverlay](H2-1) → [Notifications](H2-2) → [Overlay](H2-3) → [FullScreen] → [Battery](H2-5).
 * [Battery]는 필수 셋이 모두 켜졌을 때만 나와요. 정확한 알람은 배너에 넣지 않아요(결정 133).
 */
enum class PermissionBanner(val startStep: PermissionStep) {
    /** H2-1 알림과 '다른 앱 위에 표시'가 둘 다 꺼짐. */
    NotificationsAndOverlay(PermissionStep.Required),

    /** H2-2 알림 꺼짐. */
    Notifications(PermissionStep.Required),

    /** H2-3 '다른 앱 위에 표시' 꺼짐. */
    Overlay(PermissionStep.Required),

    /**
     * 전체 화면 알림만 꺼짐(안드로이드 14 이상). 피그마 H2에는 이 경우의 배너가 없어서 추천 문구로 채워요(정할 것).
     * 꺼진 필수 권한이 있는데 배너가 없으면 사용자가 알 길이 없어서요.
     */
    FullScreen(PermissionStep.Required),

    /** H2-5 배터리 사용 제한이 켜져 있음(절전 예외가 꺼짐). */
    Battery(PermissionStep.Optional),
}

/** 지금 띄울 배너. 없으면 null. */
val PermissionReport.banner: PermissionBanner?
    get() {
        val notifications = isGranted(AlarmPermission.Notifications)
        val overlay = isGranted(AlarmPermission.Overlay)
        return when {
            !notifications && !overlay -> PermissionBanner.NotificationsAndOverlay
            !notifications -> PermissionBanner.Notifications
            !overlay -> PermissionBanner.Overlay
            !isGranted(AlarmPermission.FullScreen) -> PermissionBanner.FullScreen
            !isGranted(AlarmPermission.Battery) -> PermissionBanner.Battery
            else -> null
        }
    }

/** 나중용 권한 시트 PL2의 두 단계(결정 129 보충 5, 132). */
enum class PermissionStep(val permissions: List<AlarmPermission>) {
    /** 1단계: 알림, 전체 화면 알림, 다른 앱 위에 표시. '다음'은 셋이 다 켜져야 눌려요. */
    Required(listOf(AlarmPermission.Notifications, AlarmPermission.FullScreen, AlarmPermission.Overlay)),

    /** 2단계: 위치, 배터리 사용 제한 해제(피그마 PL2-3 줄 순서). '완료'는 언제나 눌려요. */
    Optional(listOf(AlarmPermission.Location, AlarmPermission.Battery)),
}

/** 이 단계의 권한이 모두 켜졌는지. */
fun PermissionReport.allGranted(step: PermissionStep): Boolean = step.permissions.all(::isGranted)

/**
 * 시트를 [requested] 단계로 열 때 실제로 보일 단계. null이면 보일 것이 없어 열지 않아요.
 * 1단계로 열었는데 필수가 다 켜져 있으면 2단계부터, 2단계로 열었는데 필수가 꺼져 있으면 1단계부터예요
 * (배터리 푸시를 눌렀을 때, 필수가 더 급해서요).
 */
fun PermissionReport.openingStep(requested: PermissionStep): PermissionStep? = when {
    !allGranted(PermissionStep.Required) -> PermissionStep.Required
    requested == PermissionStep.Required && allGranted(PermissionStep.Optional) -> null
    else -> PermissionStep.Optional
}

/** 1단계 '다음'을 누른 뒤. 선택 둘이 이미 다 켜져 있으면 2단계를 보여 주지 않고 닫아요(null). */
fun PermissionReport.stepAfterRequired(): PermissionStep? =
    if (allGranted(PermissionStep.Optional)) null else PermissionStep.Optional
