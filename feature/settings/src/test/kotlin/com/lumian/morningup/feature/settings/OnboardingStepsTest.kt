package com.lumian.morningup.feature.settings

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.model.AlarmPermission
import com.lumian.morningup.core.model.PermissionReport
import org.junit.Test

class OnboardingStepsTest {
    private fun report(vararg off: AlarmPermission) =
        PermissionReport(AlarmPermission.entries.associateWith { it !in off })

    private val allOff = report(
        AlarmPermission.Notifications,
        AlarmPermission.FullScreen,
        AlarmPermission.Battery,
        AlarmPermission.Overlay,
        AlarmPermission.Location,
    )

    /** [report]에서 '나중에 하기'만 눌러 끝까지 갈 때 보이는 장 순서. */
    private fun walk(report: PermissionReport): List<OnboardingStep> =
        generateSequence(OnboardingStep.first(report)) { OnboardingStep.after(it, report) }.toList()

    @Test
    fun order_isNotificationsFullScreenBatteryOverlayLocation() {
        assertThat(walk(allOff)).containsExactly(
            OnboardingStep.Notifications,
            OnboardingStep.FullScreen,
            OnboardingStep.Battery,
            OnboardingStep.Overlay,
            OnboardingStep.Location,
        ).inOrder()
    }

    @Test
    fun dots_areFive_andFollowOrder() {
        assertThat(OnboardingStep.DotCount).isEqualTo(5)
        assertThat(OnboardingStep.entries.map { it.dotIndex }).containsExactly(0, 1, 2, 3, 4).inOrder()
    }

    @Test
    fun grantedSteps_areSkipped_eachOnItsOwn() {
        // 안드로이드 13 이하처럼 전체 화면 알림이 이미 켜져 있으면 O2-2만 빠져요.
        val fullScreenOn = report(
            AlarmPermission.Notifications,
            AlarmPermission.Battery,
            AlarmPermission.Overlay,
            AlarmPermission.Location,
        )
        assertThat(walk(fullScreenOn)).containsExactly(
            OnboardingStep.Notifications,
            OnboardingStep.Battery,
            OnboardingStep.Overlay,
            OnboardingStep.Location,
        ).inOrder()
        // 다른 앱 위에 표시만 켜져 있으면 O2-4만 빠져요.
        val overlayOn = report(
            AlarmPermission.Notifications,
            AlarmPermission.FullScreen,
            AlarmPermission.Battery,
            AlarmPermission.Location,
        )
        assertThat(walk(overlayOn)).doesNotContain(OnboardingStep.Overlay)
        assertThat(walk(overlayOn)).contains(OnboardingStep.FullScreen)
    }

    @Test
    fun allGranted_noSteps_goStraightHome() {
        assertThat(OnboardingStep.first(report())).isNull()
    }

    @Test
    fun onlyLocationOff_showsOnlyWeatherStep() {
        assertThat(walk(report(AlarmPermission.Location))).containsExactly(OnboardingStep.Location)
    }

    @Test
    fun settle_staysWhenStillOff_movesWhenGranted() {
        assertThat(OnboardingStep.settle(OnboardingStep.Notifications, allOff))
            .isEqualTo(OnboardingStep.Notifications)
        // 알림을 허용하고 돌아오면 다음 꺼진 장(전체 화면 알림이 켜져 있으면 배터리)으로 가요.
        val afterAllow = report(AlarmPermission.Battery, AlarmPermission.Overlay, AlarmPermission.Location)
        assertThat(OnboardingStep.settle(OnboardingStep.Notifications, afterAllow))
            .isEqualTo(OnboardingStep.Battery)
    }

    @Test
    fun lastStep_grantedOrLater_finishes() {
        val locationOn = report()
        assertThat(OnboardingStep.settle(OnboardingStep.Location, locationOn)).isNull()
        assertThat(OnboardingStep.after(OnboardingStep.Location, allOff)).isNull()
    }

    @Test
    fun later_skipsToNextOffStep_evenIfCurrentStillOff() {
        val onlyNotificationsAndLocationOff = report(AlarmPermission.Notifications, AlarmPermission.Location)
        assertThat(OnboardingStep.after(OnboardingStep.Notifications, onlyNotificationsAndLocationOff))
            .isEqualTo(OnboardingStep.Location)
    }
}
