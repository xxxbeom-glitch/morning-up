package com.lumian.morningup.core.model

import kotlinx.coroutines.flow.StateFlow

/**
 * 알람이 제대로 울리고 보이는 데 쓰는 권한과 기기 상태(결정 130 분류표, 133).
 * [group]은 설정 '권한' 화면의 어느 카드에 줄이 있는지, [required]는 꺼졌을 때 H2 배너, T1 '권한' 줄 상태 글자,
 * 하단 메뉴 '확인 필요' 말풍선을 켜는지예요. 필수는 알림, 전체 화면 알림, 다른 앱 위에 표시 셋뿐이에요.
 */
enum class AlarmPermission(val group: PermissionGroup, val required: Boolean) {
    /** 알림. 꺼지면 잠금 화면 위로 울림 화면을 못 띄워요. 울림 알림 채널이 꺼진 것도 포함해요. */
    Notifications(PermissionGroup.Required, required = true),

    /** 전체 화면 알림(안드로이드 14 이상에서 끌 수 있어요). */
    FullScreen(PermissionGroup.Required, required = true),

    /** 다른 앱 위에 표시(결정 105로 필수). */
    Overlay(PermissionGroup.Required, required = true),

    /** 배터리 사용 제한 없음(절전 최적화 예외). 선택이지만 꺼지면 H2-5 배너와 전날 밤 푸시가 있어요(결정 112). */
    Battery(PermissionGroup.Optional, required = false),

    /** 대략적인 위치(날씨용). 선택이고 배너, 푸시는 없어요(결정 129 보충 4). */
    Location(PermissionGroup.Optional, required = false),

    /** 방해 금지 모드 예외. 알람은 알람 소리 통로로 울려서 꺼져도 경고하지 않아요(11-17 결정 1). */
    DoNotDisturb(PermissionGroup.Other, required = false),

    /** 알람 음량이 0이 아닌지. 권한이 아니라 기기 볼륨이라 경고 없이 '기타' 카드에 화살표 줄로 둬요. */
    AlarmVolume(PermissionGroup.Other, required = false),

    /** 정확한 알람. `USE_EXACT_ALARM`으로 설치 때 받아서 화면 어디에도 줄을 두지 않아요(결정 133). 진단 기록용으로만 읽어요. */
    ExactAlarm(PermissionGroup.Hidden, required = false),

    /** 전원 종료 방지(접근성 서비스). 설정 첫 화면 토글로만 다뤄요(결정 101, 134). */
    PowerGuard(PermissionGroup.Hidden, required = false),
}

/** 설정 '권한' 화면의 카드(결정 130 보충). [Hidden]은 '권한' 화면에 줄이 없어요. */
enum class PermissionGroup { Required, Optional, Other, Hidden }

/** 한 번 확인한 권한 상태 묶음. */
data class PermissionReport(val granted: Map<AlarmPermission, Boolean>) {
    fun isGranted(permission: AlarmPermission): Boolean = granted[permission] ?: false

    /** 꺼진 필수 권한. 비어 있지 않으면 배너, 말풍선을 켜요. */
    val missingRequired: List<AlarmPermission>
        get() = AlarmPermission.entries.filter { it.required && !isGranted(it) }

    val needsAttention: Boolean get() = missingRequired.isNotEmpty()

    /** 켜진 필수 권한 수. T1 '권한' 줄 상태 글자(허용됨, 일부 허용됨, 허용 안 됨)에 써요. */
    val grantedRequiredCount: Int
        get() = AlarmPermission.entries.count { it.required && isGranted(it) }

    companion object {
        val RequiredCount: Int = AlarmPermission.entries.count { it.required }

        /** 아직 확인 전. 확인하기 전에 경고를 잘못 띄우지 않게 모두 켜진 것으로 봐요. */
        val Unknown = PermissionReport(AlarmPermission.entries.associateWith { true })
    }
}

/** 기기에서 권한 상태를 읽어요. 기기 설정에 갔다가 앱으로 돌아올 때마다 [refresh]를 불러요. */
interface PermissionChecker {
    val report: StateFlow<PermissionReport>

    fun refresh()
}
