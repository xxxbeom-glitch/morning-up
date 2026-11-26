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
    fun order_overlayThenNotificationsThenFullScreenThenBattery() {
        assertThat(report(AlarmPermission.Notifications, AlarmPermission.FullScreen).banner)
            .isEqualTo(PermissionBanner.Notifications)
        assertThat(report(AlarmPermission.Overlay, AlarmPermission.FullScreen, AlarmPermission.Battery).banner)
            .isEqualTo(PermissionBanner.Overlay)
        assertThat(report(AlarmPermission.FullScreen, AlarmPermission.Battery).banner)
            .isEqualTo(PermissionBanner.FullScreen)
        assertThat(report(AlarmPermission.Battery).banner).isEqualTo(PermissionBanner.Battery)
    }

    @Test
    fun locationDndAccessVolumeOff_noBanner() {
        val r = report(AlarmPermission.Location, AlarmPermission.DoNotDisturb, AlarmPermission.AlarmVolume)
        assertThat(r.banner).isNull()
        assertThat(r.bannerMore).isEqualTo(0)
    }

    @Test
    fun exactAlarmOff_bannerBeforeBattery() {
        // 정확한 알람은 안드로이드 12에서만 꺼질 수 있어요(TSK-177 답 8).
        assertThat(report(AlarmPermission.ExactAlarm, AlarmPermission.Battery).banner)
            .isEqualTo(PermissionBanner.ExactAlarm)
        assertThat(report(AlarmPermission.FullScreen, AlarmPermission.ExactAlarm).banner)
            .isEqualTo(PermissionBanner.FullScreen)
    }

    @Test
    fun restrictedThenSilenced_comeFirst_andCountOthers() {
        val all = report(AlarmPermission.Notifications, AlarmPermission.Overlay, AlarmPermission.Battery)
            .copy(backgroundRestricted = true, alarmsSilenced = true)
        assertThat(all.banners).containsExactly(
            PermissionBanner.BatteryRestricted,
            PermissionBanner.AlarmsSilenced,
            PermissionBanner.NotificationsAndOverlay,
            PermissionBanner.Battery,
        ).inOrder()
        assertThat(all.bannerMore).isEqualTo(3)
        val silenced = report(AlarmPermission.Battery).copy(alarmsSilenced = true)
        assertThat(silenced.banner).isEqualTo(PermissionBanner.AlarmsSilenced)
        assertThat(silenced.bannerMore).isEqualTo(1)
    }

    @Test
    fun startSteps_batteryOptional_deviceStatesNone_othersRequired() {
        assertThat(PermissionBanner.Battery.startStep).isEqualTo(PermissionStep.Optional)
        val noSheet =
            setOf(PermissionBanner.BatteryRestricted, PermissionBanner.AlarmsSilenced, PermissionBanner.ExactAlarm)
        noSheet.forEach { assertThat(it.startStep).isNull() }
        PermissionBanner.entries.filter { it != PermissionBanner.Battery && it !in noSheet }.forEach {
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
