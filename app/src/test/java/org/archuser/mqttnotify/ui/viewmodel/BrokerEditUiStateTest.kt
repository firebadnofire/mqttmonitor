package org.archuser.mqttnotify.ui.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrokerEditUiStateTest {

    @Test
    fun `verified config requires matching tested fingerprint`() {
        val state = BrokerEditUiState(
            label = "Home",
            host = "mqtt.example.com",
            testedAt = 1_000L
        )
        val fingerprint = state.currentFingerprint()

        assertTrue(state.copy(testedFingerprint = fingerprint).hasVerifiedCurrentConfig())
        assertFalse(state.copy(testedFingerprint = fingerprint).copy(host = "other.example.com").hasVerifiedCurrentConfig())
    }
}
