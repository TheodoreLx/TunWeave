package io.github.theodorelx.tunweave

import android.net.VpnService
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.theodorelx.tunweave.data.VpnState
import io.github.theodorelx.tunweave.service.ProxyVpnService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VpnStartupSmokeTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @After
    fun tearDown() {
        if (ProxyVpnService.isRunning()) {
            context.startService(ProxyVpnService.buildStopIntent(context))
        }
    }

    @Test
    fun vpnEngineStartsStaysAliveAndStops() = runBlocking {
        assumeTrue(
            "Pass -e runVpnSmoke true to run this device-dependent test",
            InstrumentationRegistry.getArguments().getString("runVpnSmoke") == "true",
        )
        assertNull("The device must already grant VPN consent", VpnService.prepare(context))

        ContextCompat.startForegroundService(context, ProxyVpnService.buildStartIntent(context))
        val startedState = withTimeout(15_000) {
            ProxyVpnService.state.first { it == VpnState.CONNECTED || it == VpnState.ERROR }
        }
        assertEquals(VpnState.CONNECTED, startedState)

        delay(5_000)
        assertTrue("Native VPN engine stopped unexpectedly", ProxyVpnService.isRunning())

        context.startService(ProxyVpnService.buildStopIntent(context))
        withTimeout(10_000) {
            ProxyVpnService.state.first { it == VpnState.DISCONNECTED }
        }
    }
}
