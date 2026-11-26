package com.lumian.morningup.core.model

/**
 * 홈 H2 권한 경고 배너(결정 113, 119, 120, TSK-177 표 5절 순서). 꺼진 것이 여러 개여도 급한 것 하나만 보이고,
 * 나머지 수는 설명 끝 '그 밖에 n개 더'로 알려요([PermissionReport.bannerMore]).
 * 순서: [BatteryRestricted](H2-7) → [AlarmsSilenced](H2-6) → [NotificationsAndOverlay](H2-1) → [Overlay](H2-3)
 * → [Notifications](H2-2) → [FullScreen] → [ExactAlarm](H2-8) → [Battery](H2-5).
 * [startStep]이 null이면 권한 시트가 아니라 기기 설정이나 안내 화면을 열어요.
 */
enum class PermissionBanner(val startStep: PermissionStep?) {
    /** H2-7 배터리 사용이 '제한'(서비스가 못 떠서 소리도 화면도 막혀요). */
    BatteryRestricted(null),

    /** H2-6 방해 금지가 알람 소리를 막고 있어요. 앱이 못 켜서 T2-1 안내로 가요. */
    AlarmsSilenced(null),

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

    /**
     * H2-8 정확한 알람 꺼짐. 안드로이드 13 이상은 설치 때 받아 늘 켜져 있어서 12에서만 나와요(TSK-177 답 8).
     */
    ExactAlarm(null),

    /** H2-5 배터리 사용 제한이 켜져 있음(절전 예외가 꺼짐). */
    Battery(PermissionStep.Optional),
}

/** 지금 걸리는 배너 모두, 급한 순서. 알림과 다른 앱 위에 표시가 둘 다 꺼지면 한 칸([NotificationsAndOverlay])이에요. */
val PermissionReport.banners: List<PermissionBanner>
    get() {
        val notifications = isGranted(AlarmPermission.Notifications)
        val overlay = isGranted(AlarmPermission.Overlay)
        return buildList {
            if (backgroundRestricted) add(PermissionBanner.BatteryRestricted)
            if (alarmsSilenced) add(PermissionBanner.AlarmsSilenced)
            when {
                !notifications && !overlay -> add(PermissionBanner.NotificationsAndOverlay)
                !overlay -> add(PermissionBanner.Overlay)
                !notifications -> add(PermissionBanner.Notifications)
            }
            if (!isGranted(AlarmPermission.FullScreen)) add(PermissionBanner.FullScreen)
            if (!isGranted(AlarmPermission.ExactAlarm)) add(PermissionBanner.ExactAlarm)
            if (!isGranted(AlarmPermission.Battery)) add(PermissionBanner.Battery)
        }
    }

/** 지금 띄울 배너. 없으면 null. */
val PermissionReport.banner: PermissionBanner? get() = banners.firstOrNull()

/** 띄운 배너 말고 더 걸린 수. '그 밖에 n개 더'에 써요. */
val PermissionReport.bannerMore: Int get() = (banners.size - 1).coerceAtLeast(0)

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

/**
 * 알람을 저장할 때 띄우는 경고 창(TSK-177 답 1, 2, 시안 S0-2, S0-3). 저장은 막지 않고, 창의 두 버튼 모두 저장해요.
 * 둘 다 걸리면 배너 순서(TSK-177 답 7)대로 [AlarmsSilenced]를 먼저 보여요. 나머지는 홈 배너가 이어서 알려요.
 */
enum class SaveWarning {
    /** S0-3 방해 금지가 알람 소리를 막는 중. '괜찮아요', '설정 열기'(T2-1 안내). */
    AlarmsSilenced,

    /** S0-2 알림과 '다른 앱 위에 표시'가 둘 다 꺼져 울려도 끌 화면이 안 떠요. '그래도 저장', '권한 켜기'(PL2 1단계). */
    CannotDismiss,
}

/** 지금 저장하면 띄울 경고. 없으면 null. */
val PermissionReport.saveWarning: SaveWarning?
    get() = when {
        alarmsSilenced -> SaveWarning.AlarmsSilenced
        !isGranted(AlarmPermission.Notifications) && !isGranted(AlarmPermission.Overlay) -> SaveWarning.CannotDismiss
        else -> null
    }
