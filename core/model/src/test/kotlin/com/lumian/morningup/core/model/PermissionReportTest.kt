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
    }

    @Test
    fun unknown_doesNotWarn() {
        assertThat(PermissionReport.Unknown.needsAttention).isFalse()
    }

    @Test
    fun optionalOff_doesNotWarn() {
        val r = report(AlarmPermission.Overlay, AlarmPermission.DoNotDisturb)
        assertThat(r.needsAttention).isFalse()
        assertThat(r.isGranted(AlarmPermission.Overlay)).isFalse()
    }

    @Test
    fun requiredOff_warnsOnItsScreenOnly() {
        val r = report(AlarmPermission.FullScreen)
        assertThat(r.needsAttention).isTrue()
        assertThat(r.missingRequired).containsExactly(AlarmPermission.FullScreen)
        assertThat(r.needsAttention(PermissionScreen.Screen)).isTrue()
        assertThat(r.needsAttention(PermissionScreen.Ring)).isFalse()
    }

    @Test
    fun missingEntry_countsAsOff() {
        val r = PermissionReport(emptyMap())
        assertThat(r.missingRequired).containsExactlyElementsIn(AlarmPermission.entries.filter { it.required })
    }
}
