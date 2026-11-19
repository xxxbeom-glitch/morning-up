package com.lumian.morningup.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PermissionPromptTest {
    private fun report(vararg off: AlarmPermission) =
        PermissionReport(AlarmPermission.entries.associateWith { it !in off })

    @Test
    fun allOn_noBanner() {
        assertThat(report().banner).isNull()
    }

    @Test
    fun notificationsAndOverlayOff_isH2_1() {
        assertThat(report(AlarmPermission.Notifications, AlarmPermission.Overlay, AlarmPermission.Battery).banner)
            .isEqualTo(PermissionBanner.NotificationsAndOverlay)
    }

    @Test
    fun order_notificationsThenOverlayThenFullScreenThenBattery() {
        assertThat(report(AlarmPermission.Notifications, AlarmPermission.FullScreen).banner)
            .isEqualTo(PermissionBanner.Notifications)
        assertThat(report(AlarmPermission.Overlay, AlarmPermission.FullScreen, AlarmPermission.Battery).banner)
            .isEqualTo(PermissionBanner.Overlay)
        assertThat(report(AlarmPermission.FullScreen, AlarmPermission.Battery).banner)
            .isEqualTo(PermissionBanner.FullScreen)
        assertThat(report(AlarmPermission.Battery).banner).isEqualTo(PermissionBanner.Battery)
    }

    @Test
    fun locationExactDndVolumeOff_noBanner() {
        val r = report(
            AlarmPermission.Location,
            AlarmPermission.ExactAlarm,
            AlarmPermission.DoNotDisturb,
            AlarmPermission.AlarmVolume,
        )
        assertThat(r.banner).isNull()
    }

    @Test
    fun batteryBanner_opensOptionalStep_otherBannersRequired() {
        assertThat(PermissionBanner.Battery.startStep).isEqualTo(PermissionStep.Optional)
        PermissionBanner.entries.filter { it != PermissionBanner.Battery }.forEach {
            assertThat(it.startStep).isEqualTo(PermissionStep.Required)
        }
    }

    @Test
    fun openingStep_skipsRequiredWhenAllRequiredOn() {
        assertThat(report(AlarmPermission.Battery).openingStep(PermissionStep.Required))
            .isEqualTo(PermissionStep.Optional)
    }

    @Test
    fun openingStep_optionalRequestStartsAtRequiredWhenRequiredOff() {
        assertThat(report(AlarmPermission.Overlay, AlarmPermission.Battery).openingStep(PermissionStep.Optional))
            .isEqualTo(PermissionStep.Required)
    }

    @Test
    fun openingStep_nothingToShow() {
        assertThat(report().openingStep(PermissionStep.Required)).isNull()
        assertThat(report().openingStep(PermissionStep.Optional)).isEqualTo(PermissionStep.Optional)
    }

    @Test
    fun stepAfterRequired_closesWhenOptionalAlreadyOn() {
        assertThat(report().stepAfterRequired()).isNull()
        assertThat(report(AlarmPermission.Location).stepAfterRequired()).isEqualTo(PermissionStep.Optional)
    }
}
