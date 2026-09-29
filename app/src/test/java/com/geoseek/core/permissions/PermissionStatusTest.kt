package com.geoseek.core.permissions

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PermissionStatusTest {
    @Test
    fun `granted wins over everything`() {
        assertThat(resolvePermissionStatus(granted = true, shouldShowRationale = true, requestedBefore = true))
            .isEqualTo(PermissionStatus.GRANTED)
    }

    @Test
    fun `never asked`() {
        assertThat(resolvePermissionStatus(granted = false, shouldShowRationale = false, requestedBefore = false))
            .isEqualTo(PermissionStatus.NOT_REQUESTED)
    }

    @Test
    fun `denied once shows rationale`() {
        assertThat(resolvePermissionStatus(granted = false, shouldShowRationale = true, requestedBefore = true))
            .isEqualTo(PermissionStatus.SHOW_RATIONALE)
    }

    @Test
    fun `asked before and no rationale means permanently denied`() {
        assertThat(resolvePermissionStatus(granted = false, shouldShowRationale = false, requestedBefore = true))
            .isEqualTo(PermissionStatus.PERMANENTLY_DENIED)
    }
}
