package com.lumian.morningup.feature.settings

import com.lumian.morningup.core.model.AlarmPermission
import com.lumian.morningup.core.model.PermissionReport

/**
 * 온보딩 권한 장(결정 129, 131). 순서는 스플래시 다음 O2-1 알림 → O2-2 전체 화면 알림 → O2-3 배터리
 * → O2-4 다른 앱 위에 표시 → O3 날씨(위치)이고, 진행 점은 이 다섯 장에만 있어요(스플래시엔 없음).
 * 이미 켜진 권한 장은 건너뛰어요(결정 129 보충 3, 결정 130 원칙 2). 안드로이드 13 이하는 전체 화면 알림이
 * 자동 허용이라 O2-2가 늘 건너뛰어져요. 건너뛰어도 점 개수는 5개 그대로예요(결정 107).
 */
enum class OnboardingStep(val permission: AlarmPermission) {
    Notifications(AlarmPermission.Notifications),
    FullScreen(AlarmPermission.FullScreen),
    Battery(AlarmPermission.Battery),
    Overlay(AlarmPermission.Overlay),
    Location(AlarmPermission.Location),
    ;

    /** 진행 점에서 이 장의 자리(0부터). */
    val dotIndex: Int get() = ordinal

    companion object {
        /** 진행 점 개수. 건너뛰는 장이 있어도 줄이지 않아요. */
        val DotCount: Int = entries.size

        /** 스플래시 다음에 보일 첫 장. 모두 켜져 있으면 null(바로 홈). */
        fun first(report: PermissionReport): OnboardingStep? = after(null, report)

        /** [step] 다음으로 보일 장. 켜진 권한 장은 건너뛰어요. 남은 장이 없으면 null(온보딩 끝). */
        fun after(step: OnboardingStep?, report: PermissionReport): OnboardingStep? {
            val start = step?.let { it.ordinal + 1 } ?: 0
            return entries.drop(start).firstOrNull { !report.isGranted(it.permission) }
        }

        /**
         * 폰 설정이나 시스템 창에서 돌아온 뒤 보일 장. 지금 장의 권한이 켜졌으면 다음 꺼진 장으로 넘기고,
         * 아직 꺼져 있으면 그 장에 그대로 머물러요. null이면 온보딩 끝이에요.
         */
        fun settle(step: OnboardingStep, report: PermissionReport): OnboardingStep? =
            if (report.isGranted(step.permission)) after(step, report) else step

        /**
         * 온보딩 전용 확인 앱(11-19 사용자 지시)에서 [step] 다음 장. 켜진 권한도 건너뛰지 않고 순서대로 다 보여요.
         * 남은 장이 없으면 null(온보딩 끝).
         */
        fun next(step: OnboardingStep?): OnboardingStep? = entries.getOrNull(step?.let { it.ordinal + 1 } ?: 0)

        /** [showAll]이면 [next], 아니면 [after]. */
        fun after(step: OnboardingStep?, report: PermissionReport, showAll: Boolean): OnboardingStep? =
            if (showAll) next(step) else after(step, report)

        /**
         * [showAll]이면 이 장에 있는 동안 권한이 새로 켜졌을 때만([grantedOnEnter]가 false) 다음 장으로 넘겨요.
         * 아니면 [settle]과 같아요.
         */
        fun settle(
            step: OnboardingStep,
            report: PermissionReport,
            showAll: Boolean,
            grantedOnEnter: Boolean,
        ): OnboardingStep? = when {
            !showAll -> settle(step, report)
            report.isGranted(step.permission) && !grantedOnEnter -> next(step)
            else -> step
        }
    }
}
