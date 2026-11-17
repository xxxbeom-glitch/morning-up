package com.lumian.morningup.core.model

import kotlinx.coroutines.flow.StateFlow

/**
 * 알람이 제대로 울리고 보이는 데 쓰는 권한과 기기 상태(design-system.md 권한 줄, F20).
 * [screen]은 어느 상세 화면에 줄이 있는지, [required]는 꺼졌을 때 H2 배너, 설정 경고 아이콘,
 * 하단 메뉴 '확인 필요' 말풍선을 켜는지예요.
 */
enum class AlarmPermission(val screen: PermissionScreen, val required: Boolean) {
    /** 정확한 알람. 꺼졌을 때만 T2-A 맨 위에 보여요. */
    ExactAlarm(PermissionScreen.Ring, required = true),

    /** 배터리 사용 제한 없음(절전 최적화 예외). */
    Battery(PermissionScreen.Ring, required = true),

    /**
     * 방해 금지 모드 예외. 알람은 알람 소리 통로로 울려서 방해 금지 중에도 보통 울리므로 경고는 켜지 않아요.
     * T2-A 줄로 둘지는 사용자 확인이 필요해요(dev-plan 6단계).
     */
    DoNotDisturb(PermissionScreen.Ring, required = false),

    /** 알람 음량이 0이 아닌지. 권한은 아니지만 0이면 소리가 안 나서 같은 줄로 보여 줘요. */
    AlarmVolume(PermissionScreen.Ring, required = true),

    /** 알림. 꺼지면 잠금 화면 위로 울림 화면을 못 띄워요. 울림 알림 채널이 꺼진 것도 포함해요. */
    Notifications(PermissionScreen.Screen, required = true),

    /** 전체 화면 알림(안드로이드 14 이상에서 끌 수 있어요). */
    FullScreen(PermissionScreen.Screen, required = true),

    /** 다른 앱 위에 표시. 선택 권한이에요. */
    Overlay(PermissionScreen.Screen, required = false),
}

/** 권한 줄이 있는 상세 화면. [Ring]은 T2-A 알람 울림, [Screen]은 T2-B 알람 화면 표시. */
enum class PermissionScreen { Ring, Screen }

/** 한 번 확인한 권한 상태 묶음. */
data class PermissionReport(val granted: Map<AlarmPermission, Boolean>) {
    fun isGranted(permission: AlarmPermission): Boolean = granted[permission] ?: false

    /** 꺼진 필수 권한. 비어 있지 않으면 배너, 경고 아이콘, 말풍선을 켜요. */
    val missingRequired: List<AlarmPermission>
        get() = AlarmPermission.entries.filter { it.required && !isGranted(it) }

    val needsAttention: Boolean get() = missingRequired.isNotEmpty()

    /** 상세 화면 하나의 필수 권한 중 꺼진 것이 있는지. T1 '알람 울림', '알람 화면 표시' 줄 경고 아이콘에 써요. */
    fun needsAttention(screen: PermissionScreen): Boolean = missingRequired.any { it.screen == screen }

    companion object {
        /** 아직 확인 전. 확인하기 전에 경고를 잘못 띄우지 않게 모두 켜진 것으로 봐요. */
        val Unknown = PermissionReport(AlarmPermission.entries.associateWith { true })
    }
}

/** 기기에서 권한 상태를 읽어요. 기기 설정에 갔다가 앱으로 돌아올 때마다 [refresh]를 불러요. */
interface PermissionChecker {
    val report: StateFlow<PermissionReport>

    fun refresh()
}
