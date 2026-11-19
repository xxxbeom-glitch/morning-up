package com.lumian.morningup.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PermissionReportTest {
    private fun report(vararg off: AlarmPermission) =
        PermissionReport(AlarmPermission.entries.associateWith { it !in off })

    @Test
    fun allOn_needsNothing() {
        val all = report()
        assertThat(all.needsAttention).isFalse()
        assertThat(all.missingRequired).isEmpty()
        assertThat(all.grantedRequiredCount).isEqualTo(PermissionReport.RequiredCount)
    }

    @Test
    fun unknown_doesNotWarn() {
        assertThat(PermissionReport.Unknown.needsAttention).isFalse()
    }

    @Test
    fun requiredAreOnlyNotificationsFullScreenOverlay() {
        assertThat(AlarmPermission.entries.filter { it.required }).containsExactly(
            AlarmPermission.Notifications,
            AlarmPermission.FullScreen,
            AlarmPermission.Overlay,
        )
    }

    @Test
    fun optionalAndOtherOff_doNotWarn() {
        val r = report(
            AlarmPermission.Battery,
            AlarmPermission.Location,
            AlarmPermission.DoNotDisturb,
            AlarmPermission.AlarmVolume,
            AlarmPermission.ExactAlarm,
        )
        assertThat(r.needsAttention).isFalse()
        assertThat(r.isGranted(AlarmPermission.Battery)).isFalse()
    }

    @Test
    fun requiredOff_warns() {
        val r = report(AlarmPermission.Overlay)
        assertThat(r.needsAttention).isTrue()
        assertThat(r.missingRequired).containsExactly(AlarmPermission.Overlay)
        assertThat(r.grantedRequiredCount).isEqualTo(2)
    }

    @Test
    fun missingEntry_countsAsOff() {
        val r = PermissionReport(emptyMap())
        assertThat(r.missingRequired).containsExactlyElementsIn(AlarmPermission.entries.filter { it.required })
    }
}
